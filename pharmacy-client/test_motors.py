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

    for din in din_to_motor:
        turns, *pins = din_to_motor[din]

        logging.log(logging.INFO, 'Now dispensing DIN ' + din + ' controlled by pins ' + str(pins) + ' doing ' + str(turns) + ' turns')

        succeeded = await dispense(din)

        logging.log(logging.INFO, 'Dispense reported ' + ('success' if succeeded else 'failure'))

        logging.log(logging.INFO, 'Pausing before the next')

        await asyncio.sleep(5)

def main():
    """
    Main function.
    """
    logging.basicConfig(level=logging.DEBUG)

    client.load_din_config()

    asyncio.run(test_motors())

if __name__ == '__main__':
    main()
