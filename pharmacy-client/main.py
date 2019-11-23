#!/usr/bin/env python3

"""
The main program that will be run on the Raspberry Pi,
which is the controller for the pharmacy client.
DINs of drugs on this pharmacy should be specified in din.cfg
"""

# these libraries come with python
import logging
import datetime
import struct
import asyncio
import json
import base64
import itertools

# pre-installed for the rapsberry pi
import RPi.GPIO as gpio

# please run setup.sh first to install these libraries
import numpy as np
import cv2
import face_recognition
import aiohttp

# constant: endpoint for the web API
api_endpoint = 'https://us-central1-smart-pharmacy-f818a.cloudfunctions.net'
# constant: current api version
api_version = '0'
# constant: safety factor for the multiple face selector algorithm
#   1 makes the check redundant
#   higher makes it less likely to detect the wrong face, but we also shouldn't be too cautious
area_safety_factor = 4
# constant: motor driving pattern
#   this pattern is designed with the requirement that
#   it must make 1 full turn (internally) when all pins are used
#   and make 0 full turns if the last pin (index 3) is disabled or not used
#   besides that, it tries to be short and apply more torque
motor_pin_pattern = np.array(
    [[1, 0, 0, 0],
     [1, 1, 0, 0],
     [0, 1, 1, 0],
     [0, 0, 1, 1],
     [0, 0, 0, 1],
     [0, 1, 0, 1],
     [1, 0, 0, 1]],
    dtype=bool
     )
# constant: number of repetitions of the motor pattern
#   to achieve a full turn of the output
motor_turn_repetitions = 500
# constant: delay in seconds between setps of the motor pattern
motor_step_delay = 0.002

# local drug database
din_to_motor = {}

def to_bytes_directly(s):
    return bytes(map(ord,s))

def from_bytes_directly(s):
    return ''.join(map(chr,s))

def as_din(s):
    """
    Coerce a DIN to the 8 digit string format.
    """
    return format(s, '>08')

def setup_pins():
    """
    Gets GPIO and the pins ready for use
    """
    # magic initialization of gpio
    gpio.cleanup()
    gpio.setmode(gpio.BCM)
    # get specific pins ready
    for turns, *motor in din_to_motor.values():
        for pin in motor:
            gpio.setup(pin, gpio.OUT)
            gpio.output(pin, False)

    logging.log(logging.INFO, 'All GPIO pins have been initialized')

async def dispense(din):
    """
    Try to dispense a drug.
    Returns true if it succeeded.
    """
    # we don't have that drug!
    if din not in din_to_motor:
        logging.log(logging.INFO, 'Cannot dispense drug with DIN ' + din + ' because this pharmacy does not have it')
        return False

    # get the driving information
    turns, *pins = din_to_motor[din]

    logging.log(logging.INFO, 'Attempting to dispense drug with DIN ' + din + ' by driving pins ' + str(pins) + ' for ' + str(turns) + ' turns')

    exc = None
    try:
        # run the motor pattern
        for step in itertools.islice(
            itertools.cycle(motor_pin_pattern),
            int(turns * motor_turn_repetitions * len(motor_pin_pattern))
            ):
            for on_off, pin in zip(step, pins):
                on_off = bool(on_off) # coerce to a regular bool
                gpio.output(pin, on_off)
            await asyncio.sleep(motor_step_delay)
    except Exception as exc2:
        exc = exc2
        logging.log(logging.ERROR, exc2)

    logging.log(logging.INFO, 'Dispensing complete, now resetting pins ' + str(pins))

    # reset all the pins for next time
    for pin in pins:
        gpio.output(pin, False)

    # errors are bad! don't let them pass
    if exc is not None:
        raise exc

    return True

async def report_dispensed(auth_token, drugs_dispensed):
    """
    Reports back to the server that drugs were dispensed... later
    """
    # get the timestamp NOW
    ts = int(datetime.datetime.utcnow().timestamp())

    # wait until dispensing should be done
    await asyncio.sleep(30)

    # if nothing was dispensed, easy
    if not drugs_dispensed:
        logging.log(logging.INFO, 'No drug dispensing, will report anyway')

    logging.log(logging.DEBUG, 'Now trying to report drug dispensed')

    # start a HTTP session
    async with aiohttp.ClientSession() as session:
        logging.log(logging.DEBUG, 'HTTP session started from report_dispensed')

        # build the json object to send
        data_send = {
            'version': api_version,
            'id': auth_token,
            'din': drugs_dispensed,
            'timestamp': ts
            }
        # response is assumed none until we get something
        data_response = None

        # it's not done until we've confirmed it's done
        while data_response is None:

            # connect to the api!
            async with session.post(
                api_endpoint + '/pharmacy_done',
                json = data_send
                ) as response:

                # get data as json
                data_response = await response.json()

                if data_response['version'] != api_version:
                    raise AssertionError('Incorrect API version encountered in report_dispensed')
                elif not data_response['success']:
                    logging.log(logging.INFO, 'API endpoint said drug dispense report failed for whatever reason')
                    data_response = None

            await asyncio.sleep(30)

        logging.log(logging.INFO, 'Drug delivery report completed and confirmed')

def pack_fingerprint(fingerprint):
    """
    Takes the vector which is a face fingerprint and
    creates a bytes object to represent it.
    Some information will be lost.
    """

    # test our assumptions

    if np.any(fingerprint >  1):raise ValueError('Fingerprint contains value greater than 1')
    if np.any(fingerprint < -1):raise ValueError('Fingerprint contains value less than -1')

    # convert from 64-bit float in range [-1, 1] to 16-bit int in full range

    # 1 - 2^-53 is the largest double value below 1
    # by scaling by this much, we prevent the edge case of boundary number 1 which can overflow to -2^15 after scaling
    scale = 1 - 2 ** -53

    if scale >= 1:raise AssertionError('Fingerprint packing uses incorrect scaling factor')

    # scale to get the 16-bit int range
    scale *= 2 ** 15

    # convert to the 16-bit int vector
    values = np.array(np.floor(fingerprint * scale), dtype=np.int16)

    # pack in bytes
    # 128 values, 16-bit integer, little endian -> 256 bytes
    result = struct.pack('<128h', *values)

    return result

async def main_step(capture):
    """
    Contains the code for the main loop.
    A return here will act as a continue in the loop.
    """
    # wait for either user to press the button or a certain number of seconds to pass

    await asyncio.sleep(1)

    logging.log(logging.DEBUG, 'Now trying to capture an image')

    # capture an image
    succeeded, pixels = capture.read()

    logging.log(logging.DEBUG, 'Image capture completed, and it ' + ('succeeded' if succeeded else 'failed'))

    # this line explains itself well
    if not succeeded:return

    # OpenCV uses BGR as its output format but we want RGB
    pixels = cv2.cvtColor(pixels, cv2.COLOR_BGR2RGB)

    logging.log(logging.DEBUG, 'Image colour channels changed to RGB')

    # find face locations in the image
    # these are represented as int tuples: (top, right, bottom, left)
    # the actual face pixels can be accessed as the rectangle pixels[top:bottom,left:right]
    face_boxes = face_recognition.face_locations(pixels, model='hog')
    num_faces = len(face_boxes)

    logging.log(logging.DEBUG, 'Found ' + str(num_faces) + ' faces in the image')

    # filter faces so only 1 is left
    if num_faces > 1:
        # the algorithm:
        # calculate the area of each bounding box
        # heuristic: an object closer to the camera takes up more space in the image
        # to be safe, we will demand a certain safety margin, so if the maximums are close, we act like there is nothing
        face_packs = [((xmax - xmin) * (ymax - ymin), (ymin, xmax, ymax, xmin)) for (ymin, xmax, ymax, xmin) in face_boxes]
        face_packs.sort(reverse=True)
        logging.log(logging.DEBUG, 'Comparing faces with area ' + str(face_packs[0][0]) + ' and area ' + str(face_packs[1][0]) + ' using safety factor ' + str(area_safety_factor))
        if face_packs[0][0] > face_packs[1][0] * area_safety_factor:
            logging.log(logging.DEBUG, 'Determined the first face to be significant enough, so using it')
            face_boxes = [face_packs[0][1]]
        else:
            logging.log(logging.DEBUG, 'Determined that no face is significant enough to use')
            face_boxes = []

    # no faces means nothing to do
    if num_faces == 0:return

    # generate the 128-vector as face fingerprint
    fingerprints = face_recognition.face_encodings(pixels, face_boxes)
    fingerprint = fingerprints[0]

    logging.log(logging.DEBUG, 'Face fingerprint was generated')

    # pack the fingerprint as bytes
    packed_fingerprint = pack_fingerprint(fingerprint)

    logging.log(logging.INFO, 'Packed face fingerprint as ' + packed_fingerprint.hex())

    # start a HTTP session
    async with aiohttp.ClientSession() as session:
        logging.log(logging.DEBUG, 'HTTP session started from main_step')

        # build the json object to send
        data_send = {
            'version': api_version,
            'fingerprint': from_bytes_directly(base64.b64encode(packed_fingerprint))
            }
        # response is assumed none until we get something
        data_response = None
        # people are not that patient
        timeout = aiohttp.ClientTimeout(total=10)

        # connect to the api!
        async with session.post(
            api_endpoint + '/pharmacy_get',
            json = data_send,
            timeout = timeout
            ) as response:

            logging.log(logging.DEBUG, 'Sent face fingerprint to authenticate')

            # get the response as json
            data_response = await response.json()

            logging.log(logging.DEBUG, 'Decoded response data as JSON')
        # continue if it succeeded
        if data_response is not None:
            if data_response['version'] != api_version:
                raise AssertionError('API response returned version ' + data_response['version'] + ' but this program uses version ' + api_version)
            if not data_response['success']:
                logging.log(logging.INFO, 'API authentication attempt reported failure')
                return
            logging.log(logging.INFO, 'Authenticated and prescription data acquired')

            # the authentication token for this session
            auth_token = data_response['id']
            # make a list of drugs that were dispensed
            drugs_dispensed = []

            await asyncio.create_task(report_dispensed(auth_token, drugs_dispensed))

            # loop over all valid prescriptions
            for pres in data_response['prescriptions']:
                # get the DIN of the drug
                din = pres['din']
                din = as_din(din)

                # is this drug in this pharmacy?
                if din in din_to_motor:
                    logging.log(logging.INFO, 'Attempting to dispense drug with DIN ' + din)

                    # try to dispense it
                    drug_was_dispensed = await dispense(din)

                    if drug_was_dispensed:
                        logging.log(logging.INFO, 'Drug dispense reported success')

                        drugs_dispensed.append(din)
                    else:
                        logging.log(logging.INFO, 'Drug dispense reported failure')

async def main_async():
    """
    Actual main function to be used in production.
    """
    # log timing information
    logging.log(logging.INFO, 'Starting main function | Current UTC time is ' + str(datetime.datetime.utcnow()))

    # set up the video capture object
    capture = cv2.VideoCapture(0)

    # the main loop
    while True:
        # log some timing information
        logging.log(logging.DEBUG, 'Starting the main loop | Current UTC time is ' + str(datetime.datetime.utcnow()))
        # try block to prevent errors from breaking the program
        try:
            # special function represents the code of the main loop
            await main_step(capture)
        except KeyboardInterrupt:
            # the user intends to stop the program, so we respect this
            logging.log(logging.INFO, 'Exiting main loop because a keyboard interrupt (SIGINT) was received')
            break
        except Exception as exc:
            # any other error must not break the program
            logging.log(logging.ERROR, exc)

    # get rid of the video capture object
    capture.release()

    # say bye bye
    logging.log(logging.WARNING, 'Exiting main function, program is ending | Current UTC time is ' + str(datetime.datetime.utcnow()))

def load_din_config():
    """
    Dedicated helper function to load in DIN/motor data from file din.cfg
    """
    global din_to_motor
    
    logging.log(logging.INFO, 'Loading configuration')

    with open('din.cfg','r') as file:
        for line in file:
                din, turns, *motor_pins = line.strip().split()
                din = as_din(din)
                turns = float(turns)
                motor_pins = tuple(map(int, motor_pins))
                din_to_motor[din] = turns, *motor_pins

    logging.log(logging.INFO, 'Read {DIN:pins} mapping as ' + str(din_to_motor))

def main():
    """
    Entry point to the program.
    Will first read in the local database from the config file.
    Redirects to main_async.
    """
    
    # we are seriously writing the logs somewhere
    log_ts = int(datetime.datetime.utcnow().timestamp())
    log_fn = f'log_{log_ts}.txt'
    logging.basicConfig(filename=log_fn, level=logging.DEBUG)

    load_din_config()

    asyncio.run(main_async())

# standard way to invoke main but only if this script is run as the program and not a library
if __name__ == '__main__':
    main()
