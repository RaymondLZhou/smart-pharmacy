#!/usr/bin/env python

import sys
import logging
import numpy as np
import struct
import cv2
import face_recognition

# constant: safety factor for the multiple face selector algorithm
#   1 makes the check redundant
#   higher makes it less likely to detect the wrong face, but we also shouldn't be too cautious
area_safety_factor = 4

def generate_fingerprint(pixels):
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
        face_packs = [((xmax - xmin) * (ymax - ymin), (ymin, xmax, ymax, xmin))
                      for (ymin, xmax, ymax, xmin) in face_boxes]
        face_packs.sort(reverse=True)
        logging.log(logging.DEBUG, 'Comparing faces with area ' + str(face_packs[0][0]) + ' and area '
                    + str(face_packs[1][0]) + ' using safety factor ' + str(area_safety_factor))
        if face_packs[0][0] > face_packs[1][0] * area_safety_factor:
            logging.log(logging.DEBUG, 'Determined the first face to be significant enough, so using it')
            face_boxes = [face_packs[0][1]]
        else:
            logging.log(logging.DEBUG, 'Determined that no face is significant enough to use')
            face_boxes = []

    # no faces means nothing to do
    if num_faces == 0:
        return

    # generate the 128-vector as face fingerprint
    fingerprints = face_recognition.face_encodings(pixels, face_boxes)
    fingerprint = fingerprints[0]

    logging.log(logging.DEBUG, 'Face fingerprint was generated')

    return fingerprint


if __name__ == '__main__':
    # we output to stderr so the Java app can mirror it to its stdout
    logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)

    if len(sys.argv) != 2:
        raise ValueError('Incorrect number of command-line arguments: ' + str(len(sys.argv)))

    filename = sys.argv[1]
    logging.log(logging.DEBUG, 'Reading image from {}'.format(filename))

    img = cv2.imread(filename)
    for v in generate_fingerprint(img):
        print(v)
