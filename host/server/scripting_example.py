#!/usr/bin/env python

all_args = raw_input()
split_args = all_args.split(',')
decoded_args = [x.decode('base64') for x in split_args]
function_name = decoded_args[0]
function_args = decoded_args[1:]

print ','.join(x.encode('base64') for x in function_args)
