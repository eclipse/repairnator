#!/usr/bin/env python
#encoding=utf-8

import time, os, tempfile, subprocess, signal, logging
import stomp

host=os.getenv("ACTIVEMQ_HOST") or "localhost"
#host="localhost"
port=61613
# Uncomment next line if you do not have Kube-DNS working.
# host = os.getenv("REDIS_SERVICE_HOST")
SLEEP_TIME = os.getenv("SLEEP_TIME") or 0
QUEUE_NAME = os.getenv("ACTIVEMQ_QUEUE_NAME") or "/queue/scanner"
LISTENER_NAME = 'ScannerIdListener'
TIMEOUT = 360 # timeout for executing submissions
#dest = ["/queue/event"]
#destination = destination[0]
class ScannerIdListener(object):
    def __init__(self, conn_listen):
        self.conn_listen = conn_listen
        self.count = 0
        self.start = time.time()
    def print_output(self, type, data):
        logging.warning(type)
        for line in data:
            logging.warning(line)
    def on_message(self, headers, message):
        logging.warning("New Repo information arrived: %s" % message)
        self.conn_listen.ack(headers.get('message-id'),headers.get('subscription'))
        with tempfile.NamedTemporaryFile(mode="w+t") as stdout_f, tempfile.NamedTemporaryFile(mode="w+t") as stderr_f:
            os.system("echo %s > project_list.txt" % message) # this replaces the content in project_list.txt with the message
            time.sleep(float(SLEEP_TIME))
            p = subprocess.Popen("./scanner_launcher.sh", stdout=stdout_f.fileno(), stderr=stderr_f.fileno(), close_fds=True, shell=True, preexec_fn=os.setsid)
            exit_code = p.wait()
            stdout_f.flush()
            stderr_f.flush()
            stdout_f.seek(0, os.SEEK_SET)
            stderr_f.seek(0, os.SEEK_SET)
            stdoutdata = stdout_f.readlines()
            stderrdata = stderr_f.readlines()
            self.print_output("stdout", stdoutdata)
            self.print_output("stderr", stderrdata)

        # publish buildids to pipeline queue, first etablish a new connection to send.
        with open('build_list.txt') as f:
            content = f.readlines()
        conn_publish = stomp.Connection10([(host,port)])
        conn_publish.start()
        conn_publish.connect()
        for msg in content:
            conn_publish.send("/queue/pipeline", msg.strip(), persistent='true') 

        # Cleaning up other files, since we do not want them to pile up inside the container
        os.system("rm scanner.*")

conn_listen = stomp.Connection10([(host,port)])
conn_listen.set_listener(LISTENER_NAME, ScannerIdListener(conn_listen))
conn_listen.start()
conn_listen.connect()
conn_listen.subscribe(QUEUE_NAME,ack='client',headers={'activemq.prefetchSize': 1})
while True:
    time.sleep(3)
conn_listen.disconnect()