import time
import sys
import os
import stomp
import getopt

# Global parameters
user = os.getenv("ACTIVEMQ_USER") or "admin"
password = os.getenv("ACTIVEMQ_PASSWORD") or "admin"
host = os.getenv("ACTIVEMQ_HOST") or "localhost"  # Host will not change since we only need at most one Activemqserver
port = os.getenv("ACTIVEMQ_PORT") or 61613 		  # Same goes for port
dest = "/queue/event"							  # this default queue can be updated if there are different queues			  


# Syntax python publisher.py -d destination string1 string2 string3 ... stringN
# Each string will be sended separately
if __name__ == '__main__':
	try:
		opts, args = getopt.getopt(sys.argv[1:], 'd:',['destination='])
	except getopt.GetoptError:
		print("Input Error")
		sys.exit(2)

	optsdict = dict(opts)
	dest = optsdict.get("-d")
	# Connect to server
	conn = stomp.Connection(host_and_ports = [(host, port)])
	conn.start()
	conn.connect(login=user,passcode=password)

	for msg in args:
		conn.send(dest, msg, persistent='true') 

  	conn.disconnect() # Disconnect from server.