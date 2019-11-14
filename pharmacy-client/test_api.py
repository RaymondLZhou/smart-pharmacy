#!/usr/bin/env python3

"""
Interactive test program for the web API.
Meant to be run in an interactive shell.
Does not need to run on the Raspberry Pi.

WARNING
This program is capable of violating the digital contract for the pharmacy client.
The developers are not liable for damage caused by your improper use of the program.
"""

# these libraries come with python
import datetime
import struct
import asyncio
import json
import base64
import shlex
import pprint

# please run setup.sh first to install these libraries
import numpy as np
import aiohttp

# constant: endpoint for the web API
api_endpoint = 'https://us-central1-smart-pharmacy-f818a.cloudfunctions.net'
# constant: current api version
api_version = '0'

def to_bytes_directly(s):
    return bytes(map(ord,s))

def from_bytes_directly(s):
    return ''.join(map(chr,s))

async def command_get(fingerprint):
    if len(fingerprint) == 512:
        packed_fingerprint = base64.b64encode(bytes.fromhex(to_bytes_directly(fingerprint)))
    elif len(fingerprint) == 344:
        packed_fingerprint = to_bytes_directly(fingerprint)
    else:
        raise ValueError(f'Invalid fingerprint of length {len(fingerprint)}')
    base64.b64decode(packed_fingerprint)

    print(f'Packed base64-encoded fingerprint is {packed_fingerprint}')

    async with aiohttp.ClientSession() as session:

        data_send = {
            'version': api_version,
            'fingerprint': from_bytes_directly(packed_fingerprint)
            }
        data_response = None
        timeout = aiohttp.ClientTimeout(total=10)

        print('Now attempting to connect')

        async with session.get(
            api_endpoint + '/pharmacy_get',
            json = data_send,
            timeout = timeout
            ) as response:
            
            data_response = await response.json()

        if data_response is None:
            print('Got no response')
        else:
            print('Response returned:')
            pprint.pprint(data_response)

async def command_done(auth_token, *dins):
    ts = datetime.datetime.utcnow().timestamp()
    
    async with aiohttp.ClientSession() as session:
        data_send = {
            'version': api_version,
            'id': auth_token,
            'din': dins,
            'timestamp': ts
            }
        data_response = None
        print('Attempting to connect...')
        while data_response is None:
            async with session.get(
                api_endpoint + '/pharmacy_done',
                json = data_send
                ) as response:

                data_response = response.json()

                if data_response['version'] != api_version:
                    raise AssertionError('Incorrect API version encountered in report_dispensed')
                elif not data_response['success']:
                    print('API endpoint confirmed, drug dispensing has been recorded')
                    data_response = None

def main():
    print('Web API Tester\n'+'-'*40+'\nCommands:\n(enter nothing) - exit the program\nget <fingerprint> - perform the pharmacy fetch prescriptions step\ndone <id> <din> <din> ... - perform the pharmacy report finished step\n'+'='*40)
    while True:
        try:
            print('The current UTC time is ' + str(datetime.datetime.utcnow()))
            
            command = input()
            tokens = shlex.split(command)

            if len(tokens) == 0:
                print('Bye bye!')
                return

            first, *tokens = tokens

            if first == 'get':
                asyncio.run(command_get(*tokens))

            elif first == 'done':
                asyncio.run(command_done(*tokens))

            else:
                print('I don\'t know what '+shlex.quote(first)+' is')
        except ValueError as exc:
            print(exc)
            print('Okay, you can enter another command now')

if __name__ == '__main__':
    main()
