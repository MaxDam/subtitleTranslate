#!/usr/bin/env python
# -*- coding: utf-8 -*-

#link:
#https://gist.github.com/jney/891218

#pysrt library is required:
#sudo pip install pysrt
#sudo pip install webvtt-py

import os, sys, json, urllib, re
import urllib.request
import html.parser
from pysrt import SubRipFile, SubRipItem
from webvtt import WebVTT

agent = {'User-Agent':
	"Mozilla/4.0 (\
	compatible;\
	MSIE 6.0;\
	Windows NT 5.1;\
	SV1;\
	.NET CLR 1.1.4322;\
	.NET CLR 2.0.50727;\
	.NET CLR 3.0.04506.30\
	)"}

base_link = "http://translate.google.com/m?hl=%s&sl=%s&q=%s"

both_language = False

#effettua l'unescape dei caratteri
def unescape(text):
    if (sys.version_info[0] < 3):
        parser = HTMLParser.HTMLParser()
    else:
        parser = html.parser.HTMLParser()
    return (parser.unescape(text))
    
#chiama google translate per tradurre la frase
def translate(text, input_language, output_language):
	link = base_link % (output_language, input_language, urllib.parse.quote(text))
	#print(link)
	request = urllib.request.Request(link, headers=agent)
	raw_data = urllib.request.urlopen(request).read()
	data = raw_data.decode("utf-8")
	expr = r'class="t0">(.*?)<'
	re_result = re.findall(expr, data)
	if (len(re_result) == 0):
		result = "None"
	else:
		result = unescape(re_result[0])
	#print("result", result)
	return (result)

#prova a leggere in formato vvt
def readVtt(input_file, output_file, input_language, output_language):
	webvtt = WebVTT().read(input_file)
	for sentence in webvtt:
		print(sentence.text)
		translateSentence = translate(sentence.text, input_language, output_language)
		if both_language:
			sentence.text = sentence.text + " (" + translateSentence + ")"
		else:
			sentence.text = translateSentence
		print(sentence.text)
	webvtt.save()
	os.rename(input_file, input_file+".old")
	os.rename(input_file.replace(".srt", ".vtt"), input_file)
	print(">", input_file, "saved!")

#prova a leggere in formato srt
def readSrt(input_file, output_file, input_language, output_language):
	print('processing file', input_file)
	subs = SubRipFile.open(input_file)
	print(">", "read file", input_file)
	for sentence in subs:
		print(sentence.text)
		translateSentence = translate(sentence.text, input_language, output_language)
		if both_language:
			sentence.text = sentence.text + " (" + translateSentence + ")"
		else:
			sentence.text = translateSentence
		print(sentence.text)
	subs.save(output_file, 'utf-8')
	webvtt = WebVTT().from_srt(output_file)
	webvtt.save()
	os.rename(input_file, input_file+".old")
	os.remove(output_file)
	os.rename(output_file.replace(".srt", ".vtt"), input_file)
	print(">", output_file, "saved!")

def translate_subtitle_file(input_file, output_file, input_language, output_language):
    """translate a srt file from a language to another"""
    
    print(">", "read file", input_file)
    try:
    	readVtt(input_file, output_file, input_language, output_language)
    except Exception as e:
    	readSrt(input_file, output_file, input_language, output_language)

def main():
	if len(sys.argv) == 1:
		subdirs = [x[0] for x in os.walk(".")]
		for subdir in subdirs:  
			input_file = subdir+"/en_US.srt"
			output_file = subdir+"/it_IT.srt"
			input_language = "en"
			output_language = "it"
			#print(input_file, output_file, input_language, output_language)
			try:
				if os.path.isfile(input_file + ".old"):
					print("saltata la traduzione di:", input_file);
				else:
					translate_subtitle_file(input_file=input_file, output_file=output_file, input_language=input_language, output_language=output_language)
			except Exception as e:
				print("error translate file:", input_file, output_file, input_language, output_language)
				print(str(e))
			
	elif len(sys.argv) == 4:
		translate_subtitle_file(*sys.argv[-4:])
	else:
		info = '''
        	translate a subtitle file from a language to another.
            Most of the code to use google translate API was taken there :
              https://github.com/zhendi/google-translate-cli
            usage:
              translate-srt.py input_file output_file input_language output_language
            example:
              translate-srt.py ./titanic-en.srt ./titanic-fr.srt en fr
        	'''
		print(info)

if __name__ == '__main__':
    main()