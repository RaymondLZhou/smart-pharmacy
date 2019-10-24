#!/usr/bin/env python3

# TODO
# use asyncio library and async functionality which exists since Python 3.5
# if python version is too old and can't be updated on the Raspberry Pi, find a workaround

# these libraries come with python
import logging
import datetime
import struct

# please run setup.sh first to install these libraries
import numpy as np
import cv2
import face_recognition

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

def main_step(capture):
    """
    Contains the code for the main loop.
    A return here will act as a continue in the loop.
    """
    # TODO for async rewrite
    # wait for either user to press the button or a certain number of seconds to pass

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
    face_boxes = face_recognition.face_locations(pixels, model='hog')
    num_faces = len(face_boxes)

    logging.log(logging.DEBUG, 'Found ' + str(num_faces) + 'faces in the image')

    # no faces means nothing to do
    if num_faces == 0:return

    # TODO filter faces so only 1 is left, or else give up

    # generate the 128-vector as face fingerprint
    fingerprints = face_recognition.face_encodings(pixels, face_boxes)
    fingerprint = fingerprints[0]

    logging.log(logging.DEBUG, 'Face fingerprint was generated')

    # pack the fingerprint as bytes
    packed_fingerprint = pack_fingerprint(fingerprint)

    logging.log(logging.INFO, 'Packed face fingerprint as ' + packed_fingerprint.hex())

    # TODO communicate with the server and proceed to possibly dispense stuff

def main():
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
            main_step(capture)
        except KeyboardInterrupt:
            # the user intends to stop the program, so we respect this
            logging.log(logging.INFO, 'Exiting main loop because a keyboard interrupt (SIGINT) was received')
            raise KeyboardInterrupt
        except Exception as exc:
            # any other error must not break the program
            logging.log(logging.ERROR, exc)

    # get rid of the video capture object
    capture.release()

    # say bye bye
    logging.log(logging.WARNING, 'Exiting main function, program is ending | Current UTC time is ' + str(datetime.datetime.utcnow())

def main_test():
    """
    Previous main function left over from testing. Will be removed when it is no longer useful.
    """

    print('start of program')

    cap = cv2.VideoCapture(0)

    print('camera initialized')

    for _ in range(1):
        print('start of main loop')
        
        # try to capture an image
        # image is a 3d array: (Y, X, bgr)
        ret, frame = cap.read()
        
        print('image captured')
        
        # reorder to RGB
        # not necessary to do it this way but it works
        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        print('image converted to rgb')
        
        # we must first detect and locate faces within the image
        # this is separate from the face fingerprinting
        face_boxes = face_recognition.face_locations(frame,
            model='hog')
        
        print('faces detected in image')
        
        # face_recognition library includes a premade AI
        # this will spit out a 1d array with 128 floating point entries
        # they seem to be within [-1, 1] and average at 0
        # this fingerprint is a summary of the features of the faces
        # we will later transform this vector and then send that to the server for processing
        fingerprints = face_recognition.face_encodings(frame, face_boxes)

        print('face fingerprints generated')

        print(f'created {len(fingerprints)} fingerprints')
        
        for index, fingerprint in enumerate(fingerprints):
            print('-'*40)
            print(f'data of fingerprint #{index}')
            print(f'is a vector with shape {fingerprint.shape} and type {fingerprint.dtype}')
            print(f'min is {np.min(fingerprint)}')
            print(f'max is {np.max(fingerprint)}')
            print(f'mean is {np.mean(fingerprint)}')
            print('raw data')
            print(fingerprint)

    print('main loop exited')

    print('cleaning up')

    cap.release()
    cv2.destroyAllWindows()

    print('bye bye!')

# standard way to invoke main but only if this script is run as the program and not a library
if __name__ == '__main__':
    main_test()
