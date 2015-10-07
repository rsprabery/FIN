#!/usr/bin/env python

import subprocess
import argparse

from fin.handlers import InputHandler, ServerHandler
from fin.mitm import MitmServer

class SubProcHandler(InputHandler):

    def __init__(self, command):
        self.command = command

    def handle_input(self, from_function, input_args):
        print "in function: " + from_function
        full_messages = [from_function] + input_args
        encoded_data_for_child = MitmServer.encode_message(full_messages)
        child = subprocess.Popen(self.command, shell=False, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                 stderr=subprocess.STDOUT)
        output = child.communicate(encoded_data_for_child)[0]
        new_args = MitmServer.decode_message('nullFunc,' + output)[1]
        return new_args

try:

    parser = argparse.ArgumentParser(description="Allow your mobile device to connect to your computer.")
    parser.add_argument("script", help="The script used to process input from your mobile device.")
    parser.add_argument("--port", help="Port to listen on. Defaults to 8080", default=8080, type=int)
    args = parser.parse_args()

    ServerHandler(MitmServer(args.port), SubProcHandler(args.script)).run()
except KeyboardInterrupt as e:
    print "exiting"
    exit(0)

