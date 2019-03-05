#!/usr/bin/env python
#encoding=utf-8

import time, os, tempfile, subprocess, signal, logging
import stomp

# host="activemq"
host="172.17.0.1"
port=61613
# Uncomment next line if you do not have Kube-DNS working.
# host = os.getenv("REDIS_SERVICE_HOST")

QUEUE_NAME = os.getenv("ACTIVEMQ_QUEUE_NAME")
LISTENER_NAME = 'BuildIdListener'
TIMEOUT = 360 # timeout for executing submissions

class BuildIdListener(object):
    def print_output(self, type, data):
        logging.warning(type)
        for line in data:
            logging.warning(line)
    def on_message(self, headers, message):
        logging.warning("New build id arrived: %s" % message)
        with tempfile.NamedTemporaryFile(mode="w+t") as stdout_f, tempfile.NamedTemporaryFile(mode="w+t") as stderr_f:
            p = subprocess.Popen("BUILD_ID=%s ./pipeline_launcher.sh"%(message), stdout=stdout_f.fileno(), stderr=stderr_f.fileno(), close_fds=True, shell=True, preexec_fn=os.setsid)
            exit_code = p.wait()
            stdout_f.flush()
            stderr_f.flush()
            stdout_f.seek(0, os.SEEK_SET)
            stderr_f.seek(0, os.SEEK_SET)
            stdoutdata = stdout_f.readlines()
            stderrdata = stderr_f.readlines()
            self.print_output("stdout", stdoutdata)
            self.print_output("stderr", stderrdata)

conn = stomp.Connection10([(host,port)])
conn.set_listener(LISTENER_NAME, BuildIdListener())
conn.start()
conn.connect()
conn.subscribe(QUEUE_NAME)
while True:
    time.sleep(3)
conn.disconnect()