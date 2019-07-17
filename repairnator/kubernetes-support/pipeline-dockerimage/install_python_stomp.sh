#!/usr/bin/env bash
# To communicate with activeMQ we need to install the pip wheel and
# then install stomp.

apt-get install python-pip -y
pip install stomp.py