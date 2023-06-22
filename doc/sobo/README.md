# SOBO: automatic feedback bot

SOBO is an automatic feedback bot that provides hints on improving code quality. It is built on top of the Repairnator infrastructure.

See  [SOBO: A Feedback Bot to Nudge Code Quality in Programming Courses](http://arxiv.org/pdf/2303.07187) (Sofia Bobadilla, Richard Glassey, Alexandre Bergel and Martin Monperrus), Technical report 2303.07187, arXiv, 2023.


### Pre Requisites
So far SOBO can handeling the following language programs: 
- Java

So far SOBO can handeling the following version control systems:
- Git



### Installation

You must do the following steps to install SOBO:

- Configure a MONGODB database following this [tutorial](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-windows/).
- On teh DB you must have the next tables:
  - minedViolations: to save all violations found by SOBO
    - _id: ObjectId
    - repo: String
    - commit: String
    - rule: String
    - line: String
    - file: String
  - commands: to save all commands executed by SOBO
    - _id: ObjectId
    - repo: String
    - commit: String
    - command: String
    - result: String
    - date: String
  - soboAM: to save all Automatic messages from SOBO
  - soboCL: to save all command responses from SOBO
  - Users:  to storage the users of the bot
    - _id: ObjectId
    - name: String
    - task x : boolean --> to indicate if the student stopped or not the service 



Execution example:

``` python
import subprocess
import sys
import os
import multiprocessing as mp
import time
from pathlib import Path

os.environ["launcherMode"] = "FEEDBACK"
os.environ["dbUser"] = name.of-mongodb-user
os.environ["pwd"] = password-for-the-mongo-db-user
os.environ["GOAUTH"] = github-token-with-acess-to-students-repo
os.environ["IP"] = ip-of-the-mongodb-server
os.environ["REPOS_PATH"] =  txt-of-repos-to-monitor // example "testing_repos_path.txt"
os.environ["login"] = github-login //only necessary for Enterprise GitHub
os.environ["SCAN_START_TIME"] = date-to-start // example "01/17/2023"
os.environ["FETCH_MODE"] = "all" // defines if you want projects with failing tests
os.environ["FEEDBACK_TOOL"] = "SoboBot"
os.environ["SONAR_RULES"] = "S1481,S1155,S1213,S2119" 
os.environ["commandFrequency"]= "5000" // on miliseconds
os.environ["collection"]="sobodb"  // collection name of the database
os.environ["command"]= "true" // if you want to run the command functionality or not
date = time.localtime


def process():
    print(f"Working on: {date}")
    print(f"Working on: {os.path.dirname(os.path.realpath(__file__))}")
    os.system(f"java -jar repairnator1118.jar 1>> testCLlog.log 2>> testCLerr.err")    

def main():

    process()


if __name__ == "__main__":
    main()
    
```

### Usage
If you find errors or similar please open an issue and/or refer to sofbob@kth.se