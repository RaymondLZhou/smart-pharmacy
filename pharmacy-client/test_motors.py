#!/usr/bin/env python3

"""
Test turns some motors and prints to standard output.
Helps with setting up the automated pharmacy.
DINs of drugs on this pharmacy should be specified in din.cfg
"""

# libraries we use here
import logging
import asyncio

# the already written pharmacy client code
import main as client

async def test_motors():
    """
    Tests each motor separately.
    """

    for din in sorted(client.din_to_motor):
        turns, *pins = client.din_to_motor[din]

        logging.log(logging.INFO, 'Now dispensing DIN ' + din + ' controlled by pins ' + str(pins) + ' doing ' + str(turns) + ' turns')

        succeeded = await client.dispense(din)

        logging.log(logging.INFO, 'Dispense reported ' + ('success' if succeeded else 'failure'))

        logging.log(logging.INFO, 'Pausing before the next')

        await asyncio.sleep(5)

def main():
    """
    Main function.
    """
    logging.basicConfig(level=logging.DEBUG)

    client.load_din_config()

    client.setup_pins()

    while True:
        asyncio.run(test_motors())

        logging.log(logging.INFO, 'Again? (enter a blank line to exit')

        if not input():
            break

if __name__ == '__main__':
    main()
