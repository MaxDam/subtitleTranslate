#!/usr/bin/env python3
"""
video_to_mp3.py
 
Description:
Simple script to extract MP3 audio from videos using Python.
 
Requirements:
 - "lame"
 - "ffmpeg"
 
If the libraries are not installed just run the following command in your terminal:
- On Mac(OS X): brew install lame ffmpeg
- On Linux(Ubuntu): sudo apt-get install lame ffmpeg
 
How to use the script:
Just run the following command within your terminal replacing "NAME_OF_THE_VIDEO.mp4" by the name of your video file:
$ python video_to_mp3.py NAME_OF_THE_VIDEO.mp4
 
Note.- The video must be within the same directory of the python script, otherwise provide the full path.
"""
import sys
import os
import time
 
 
def video_to_mp3(file_name):
    """ Transforms video file into a MP3 file """
    try:
        file, extension = os.path.splitext(file_name)
        # Convert video into .wav file
        os.system('ffmpeg -i {file}{ext} {file}.wav'.format(file=file, ext=extension))
        # Convert .wav into final .mp3 file
        os.system('lame {file}.wav {file}.mp3'.format(file=file))
        os.remove('{}.wav'.format(file))  # Deletes the .wav file
        print('"{}" successfully converted into MP3!'.format(file_name))
    except OSError as err:
        print(err.reason)
        exit(1)
 
 
def main():
    # Confirm the script is called with the required params
    if len(sys.argv) != 2:
        print('Usage: python video_to_mp3.py FILE_NAME')
        exit(1)
 
    file_path = sys.argv[1]
    try:
        if not os.path.exists(file_path):
            print('file "{}" not found!'.format(file_path))
            exit(1)
 
    except OSError as err:
        print(err.reason)
        exit(1)
 
    video_to_mp3(file_path)
    time.sleep(1)
 
 
if __name__ == '__main__':
    main()