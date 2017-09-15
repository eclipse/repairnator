"""
webRepairnator: A simple continuous integration server for hooks from TravisCI
(inspired by Gakoci see https://github.com/monperrus/gakoci )
Author: Simon Urli
License: MIT
Sept 2017
"""

import json
import socket
import os
from tempfile import mkstemp, mkdtemp
from subprocess import Popen, PIPE, DEVNULL
import subprocess
import time
import uuid
import threading
import glob
from distutils.spawn import find_executable

# non standards, in requirements.txt
from flask import Flask, request, abort
import requests


class EventAction:
    """ abstract class represents an action to be done in response to a Github event. See for instance PushAction """

    def __init__(self):
        self.meta_info = {}

    def get_scripts(self):
        """ returns all scripts to be executed for this event. to be overridden """
        return []

    def add_script(self, script):
        """ adds a script if it exists and is executable """
        globpath = os.path.join(self.hooks_dir, script + '*')
        for s in glob.glob(globpath):
            if os.path.isfile(s) and os.access(s, os.X_OK) and s not in self.scripts:
                self.scripts.append(s)


class OnFailureAction(EventAction):
    """ calls onfailure-<owner>-<repo>-* with 6 arguments """

    def __init__(self, application, payload_path):
        self.scripts = []
        self.hooks_dir = application.hooks_dir
        self.meta_info = get_core_info(payload_path)
        self.payload_path = payload_path

    def get_scripts(self):
        # here it means that we somehow "secure" the CI daemon, because only
        # certain repo will be handled
        self.add_script(
            "failure-")
        print(self.scripts)
        return self.scripts

    def arguments(self):
        return [self.payload_path,  # $1 in shell
                self.meta_info['event_type'],  # $2 in shell
                self.meta_info['owner'],  # $3 in shell
                self.meta_info['repo'],  # $4 in shell
                self.meta_info['branch'],  # $5 in shell
                self.meta_info['commit']  # $6 in shell
                ]

class OnPassedAction(EventAction):
    """ calls onfailure-<owner>-<repo>-* with 6 arguments """

    def __init__(self, application, payload_path):
        self.scripts = []
        self.hooks_dir = application.hooks_dir
        self.meta_info = get_core_info(payload_path)
        self.payload_path = payload_path

    def get_scripts(self):
        # here it means that we somehow "secure" the CI daemon, because only
        # certain repo will be handled
        self.add_script(
            "passed-")
        print(self.scripts)
        return self.scripts

    def arguments(self):
        return [self.payload_path,  # $1 in shell
                self.meta_info['id'],  # $2 in shell
                self.meta_info['owner'],  # $3 in shell
                self.meta_info['repo'],  # $4 in shell
                self.meta_info['state'],  # $5 in shell
                self.meta_info['commit']  # $6 in shell
                ]

def get_core_info(json_path):
    """ extracts the important information from the payload given as path """
    with open(json_path) as path:
        json_data = json.load(path)
        return get_core_info_str(json_data)


def get_core_info_str(json_data):
    """ extracts the important information from the push payload as string"""
    result = {'id': json_data['id'],
              'owner': json_data['repository']['owner_name'],
              'repo': json_data['repository']['name'],
              'state': json_data['status_message'],
              'commit': json_data['commit'],
              'language': json_data['language']
              }

    return result

class WebRepairnator:
    """ 
    The main class of the GakoCI server.
    Usage: WebRepairnator(github_token="khkjhkjf", repos = ["monperrus/test-repo"]).run()
    """

    def __init__(self, repos, host="127.0.0.1", port=5000, hooks_dir='./hooks'):
        self.host = host
        self.port = port
        self.hooks_dir = hooks_dir
        self.application = self.create_flask_application()
        self.set_public_url()
        self.ran = {}

        # used so that only one task is performed at a time
        # otherwise with for CI java, the server goes into out-of-memory
        self.lock = threading.Lock()
        pass  # end __init__

    def set_public_url(self):
        if self.host == "0.0.0.0":
            self.public_url = "http://" + socket.getfqdn() + ":" + str(self.port)
        else:
            self.public_url = "http://" + self.host + ":" + str(self.port)

    def get_core_info_depending_on_event_type(self, event_type, payload_path):
        result = EventAction()
        if event_type == "Failed":
            result = OnFailureAction(self, payload_path)
        if event_type == "Passed":
            result = OnPassedAction(self, payload_path)
        result.meta_info['event_type'] = event_type
        return result

    def perform_tasks(self, event_type, payload_path):
        event_action = self.get_core_info_depending_on_event_type(
            event_type, payload_path)
        for s in event_action.get_scripts():
            # has to be asynchronous, because Github expects a fast response
            threading.Thread(target=WebRepairnator.execute, args=(
                self, s, event_action)).start()

    def get_script_timeout_in_minutes(self):
        """ can be overridden by subclasses """
        return 60*4 # 4 hours

    def execute(self, s, event_action):
        try:
            self.lock.acquire(True)
            cwd = mkdtemp()
            self.ran[os.path.basename(cwd)] = cwd
            command = [s] + event_action.arguments()
            print(" ".join(command))
            proc = Popen(
                executable=os.path.abspath(s),
                args=command,
                shell=False,
                cwd=cwd,
                stdout=DEVNULL, stderr=DEVNULL
            )
            timer = threading.Timer(self.get_script_timeout_in_minutes()*60, proc.kill)
            timer.start()
            proc.wait()
            timer.cancel()
        finally:
            self.lock.release()

    def create_flask_application(self):
        application = Flask(__name__)
        application.log = {}
        application.killurl = str(uuid.uuid4())

        @application.route('/' + application.killurl, methods=['POST'])
        def seriouslykill():
            func = request.environ.get('werkzeug.server.shutdown')
            func()
            return "Shutting down..."

        @application.route('/traces/<trace_id>', methods=['GET'])
        def trace(trace_id):
            return open(self.ran[trace_id] + "/trace.txt").read(), 200, {'Content-Type': 'text/plain; charset=utf-8'}

        @application.route('/', methods=['GET'])
        def about():
            return "running WebRepairnator</a>"

        @application.route('/', methods=['POST'])
        def index():

            payload = request.get_json()
            application.last_payload = payload

            event_type = payload.status_message

            osfd, payloadfile = mkstemp()
            with os.fdopen(osfd, 'w') as pf:
                pf.write(json.dumps(payload))
            self.perform_tasks(event_type, payloadfile)
            return 'OK'  # end INDEX
        return application

    def run(self, **keywords):
        self.application.run(host=self.host, port=self.port, **keywords)