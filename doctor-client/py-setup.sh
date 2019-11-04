#!/usr/bin/env bash

# Run this script during setup of the doctor's client to install the python packages
# Assumes Python 3.7 is already installed, pip3 is installed
pip3 install numpy

# install OpenCV (cv2) directly with pip
pip3 install opencv-python
# face recognition libraries for python
pip3 install dlib
pip3 install face_recognition