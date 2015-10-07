#!/usr/bin/env python

import socket
import atexit
import traceback
import struct
import base64

class RestartException(Exception):
    pass

class MitmServer():

    def __init__(self, port_number):
        atexit.register(self.close_connection)
        self.client_socket = None
        self.server_socket = None
        self.input_stream = None
        self.output_stream = None
        self.port_number = port_number

        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        self.server_socket.bind(("0.0.0.0", self.port_number))

    def close_client_connection(self):
        # print "closing client connection"
        # print traceback.print_exc()
        if self.client_socket is not None:
            try:
                if self.input_stream is not None:
                    self.input_stream.close()
                else:
                    print "input stream was none"
                if self.output_stream is not None:
                    self.output_stream.close()
                else:
                    print "output stream was none"
                self.client_socket.shutdown(0)
                self.client_socket.close()
            except socket.error as e:
                print e.message
                pass
            finally:
                self.input_stream = None
                self.output_stream = None
                self.client_socket = None
        else:
            print "client connection was none"

    def close_connection(self):

       #Close the client socket (if there is one connected)
       # print "closing connection...."
       self.close_client_connection()

       # Now close our server socket
       if self.server_socket is not None:
           self.server_socket.close()
           self.server_socket = None
       else:
           print "server socket was none"

    def get_client_connection(self):

        self.server_socket.listen(1)

        while (True):
            try:
              (self.client_socket, address) = self.server_socket.accept()
              yield self.client_socket
            except socket.error as e:
              pass

    def receive_bytes_until_full(self, size):
        message = ""
        while(len(message) != size):
            tempMessage =  self.client_socket.recv(size - len(message))
            if tempMessage == "": # This is empty if the connection has reset.
                print "raising restart exception"
                raise RestartException("connection closed by client")
            message += tempMessage

        return message

    def get_function_parameters(self):
        for client_socket in self.get_client_connection():
            if self.client_socket is not None:
                self.input_stream = self.client_socket.makefile('r')
                self.output_stream = self.client_socket.makefile('w')

                while (True):
                    try:
                        byte_count = struct.unpack(">I", str(self.receive_bytes_until_full(4)))[0]
                        data = self.receive_bytes_until_full(byte_count)
                        func_name, args = MitmServer.decode_message(data)
                        yield (func_name, args)
                    except ValueError as e:
                        print e.message
                        self.close_client_connection()
                    except socket.error as e:
                        print e.message
                        self.close_client_connection()
                    except RestartException as e:
                        print e.message
                        self.close_client_connection()

                    # We'll reset the client socket if the connection is broken.
                    # So if's it is None, then we need to wait for the next client.
                    if self.client_socket is None:
                        break

    @staticmethod
    def decode_message(fullReceivedMessage):
        message_parts = fullReceivedMessage.split(',')
        func_name = base64.b64decode(message_parts[0])

        args = [base64.b64decode(x) for x in message_parts[1:]]
        args = [x[1:-1] if not x == "null" else None for x in args]

        return (func_name, args)

    @staticmethod
    def encode_message(new_args):
        return ",".join(base64.b64encode('"' + x + '"' if x is not None else "null") for x in new_args)

    def send_params(self, new_args):
        try:
            if self.output_stream is not None:
                message = MitmServer.encode_message(new_args)

                byte_count = struct.pack(">I", len(message))
                self.output_stream.write(byte_count)
                self.output_stream.flush()

                self.output_stream.write(message)
                self.output_stream.flush()
        except socket.error as e:
            print e.message
            # traceback.print_exc()
            self.close_client_connection()

