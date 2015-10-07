#!/usr/bin/env python

class InputHandler():
   def handle_input(self, from_function, args):
       raise NotImplementedError

class ServerHandler():
    def __init__(self, mitm_server, message_handler):
        self.mitm_server = mitm_server
        self.handler = message_handler

    def run(self):
        while(True):
            for function_name, args in self.mitm_server.get_function_parameters():
                new_args = self.handler.handle_input(function_name, args)
                self.mitm_server.send_params(new_args)
