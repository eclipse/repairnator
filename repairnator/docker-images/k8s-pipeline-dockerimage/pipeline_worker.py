#!/usr/bin/env python
# This script listen to the activeMQ queue. When a message , a build id arrives
# it will pull and run repairnator as a subprocess with the provided build id.

#encoding=utf-8

import time, os, tempfile, subprocess, signal, logging
import stomp

# Global variables
host=os.getenv("ACTIVEMQ_HOST") or "activemq"
port=61613
# Uncomment next line if you do not have Kube-DNS working.
# host = os.getenv("REDIS_SERVICE_HOST")
SLEEP_TIME = os.getenv("SLEEP_TIME") or 0
QUEUE_NAME = os.getenv("ACTIVEMQ_QUEUE_NAME")
LISTENER_NAME = 'BuildIdListener'
TIMEOUT = 360 # timeout for executing submissions

# This class listen to the queue and catch and any incomming message if not already taken by other.
class BuildIdListener(object):
    def __init__(self, conn):
        self.conn = conn
        self.count = 0
        self.start = time.time()
    def print_output(self, type, data):
        logging.warning(type)
        for line in data:
            logging.warning(line)
    def on_message(self, headers, message):
        logging.warning("New build id arrived: %s" % message)
        self.conn.ack(headers.get('message-id'),headers.get('subscription'))
        time.sleep(float(SLEEP_TIME)) # This is used to test out the queue on ActiveMQ have no meaning in usual case as SLEEP_TIME=0 as default
        # Send build to pipeline when found
        with tempfile.NamedTemporaryFile(mode="w+t") as stdout_f, tempfile.NamedTemporaryFile(mode="w+t") as stderr_f:
            p = subprocess.Popen("BUILD_ID=%s ./pipeline_launcher.sh"%(message), stdout=stdout_f.fileno(), stderr=stderr_f.fileno(), close_fds=True, shell=True, preexec_fn=os.setsid)
            exit_code = p.wait()
            stdout_f.flush()
            stderr_f.flush()
            stdout_f.seek(0, os.SEEK_SET)
            stderr_f.seek(0, os.SEEK_SET)
            stdoutdata = stdout_f.readlines().rstrip()
            stderrdata = stderr_f.readlines().rstrip()
            self.print_output("stdout", stdoutdata)
            self.print_output("stderr", stderrdata)
        logging.warning("PIPELINE MESSAGE: DONE REPAIRING , AWAITING FOR NEW BUILD ID")

# Make connection to queue and listen for incomming messages
conn = stomp.Connection10([(host,port)])
conn.set_listener(LISTENER_NAME, BuildIdListener(conn))
conn.start()
conn.connect()
conn.subscribe(QUEUE_NAME,ack='client',headers={'activemq.prefetchSize': 1})
logging.warning("PIPELINE MESSAGE: CONNECTED TO QUEUE , AWAITING FOR BUILD ID")
while True:
    time.sleep(3)
conn.disconnect()