#!/usr/bin/env python

import argparse
import time
import signal
import select
import os

from fin.handlers import InputHandler, ServerHandler
from fin.mitm import MitmServer


class CommandLineHandler(InputHandler):
    def __init__(self):
        self.block = True
        print "To start modifying arguments once the program beings, press CTRL-C."
        print "Pressing CTRL-C twice quickly will exit.\n"
        print "The function name will be printed, followed by any arguments to it."
        print "For each argument you will be prompted to enter modified input."
        print "Press ENTER without typing any data to simply re-send the original argument.\n"
        raw_input("Press ENTER to start.")

    def handle_input(self, from_function, input_args):
        print "In function: " + from_function
        new_args = []
        if self.block:
            for arg in input_args:
                new_arg = ""
                print "Input arg: " + str(arg)
                if self.block:
                    os.write(0, "Enter your new arg: ")
                    while (self.block):
                        try:
                            select_return = select.select([0], [], [], .3)
                            read_ready = select_return[0]
                            if len(read_ready) == 0:
                                pass
                            else:
                                new_arg = raw_input()
                                break
                        except select.error as e:
                            break

                if self.block:
                    new_arg = arg

                if len(new_arg) == 0:
                    new_arg = arg

                new_args.append(new_arg)
            input_args = new_args
        else:
            for arg in input_args:
                print arg

        return input_args


parser = argparse.ArgumentParser(description="This is the standard text interface for your function interception.")
parser.add_argument("--port", help="Port to listen on. Defaults to 8080", default=8080, type=int)
args = parser.parse_args()

command_line_handler = CommandLineHandler()


def handler(signnum, frame):
    signal.signal(signal.SIGINT, close_handler)
    print "\nChanging input mode."
    command_line_handler.block = not command_line_handler.block
    time.sleep(.75)
    signal.signal(signal.SIGINT, handler)


def close_handler(signnum, frame):
    print "Exiting..."
    exit(0)


signal.signal(signal.SIGINT, handler)

ServerHandler(MitmServer(args.port), command_line_handler).run()
