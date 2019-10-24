import numpy as np
import cv2
import face_recognition

def main_test():

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

if __name__ == '__main__':
    main_test()
