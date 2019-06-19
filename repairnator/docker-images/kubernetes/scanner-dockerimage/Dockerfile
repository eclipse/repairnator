FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image for k8s" Vendor="Spirals" Version="0.0.0"
LABEL maintainer="Henry Luong <tailp@kth.se>"

VOLUME ["/var/log/"]

COPY z3_for_linux /root/
COPY configure_git.sh /root/
COPY build_scanner.sh /root/
COPY install_python_stomp.sh /root/
COPY scanner_launcher.sh /root/
COPY scanner_worker.py /root/
COPY version.ini /root/
COPY project_list.txt /root/

RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone
RUN apt-get update
RUN apt-get install cloc -y
RUN /root/configure_git.sh
RUN /root/install_python_stomp.sh
RUN /root/build_scanner.sh

ENV M2_HOME=$MAVEN_HOME
# This is by default as "mongodb://mongo:27017" if deployed with the provided mongodb.yaml on Kubernetes.
ENV MONGODB_HOST=mongodb://mongo:27017 
# Name of the database you want to store information, can be found by "show dbs" after logging into mongodb
ENV MONGODB_NAME=
# Github information is provided if the repaired builds should be submitted on some repo
ENV GITHUB_OAUTH=
# These should not be changed, hardcoded in scanner_worker.py .
ENV REPAIR_PROJECT_LIST_PATH=/root/project_list.txt   
ENV REPAIRNATOR_BUILD_LIST=/root/build_list.txt	  
# This should be 0 by default but not empty, otherwise it will be error. We don't need to distinguis different repairnator run
# in our case
ENV RUN_ID=0
# optional for something can be empty
ENV JAVA_OPTS= 
# These by default is /root/ and should not be changed since the scanner_worker.py is hardcoded to delete output
# to avoid piling up things inside the container
ENV REPAIRNATOR_RUN_DIR=/root/ 
ENV LOG_DIR=/root/ 
# Number of hours to inspect to get last builds (e.g. 4 will mean 4 hours in the past)
ENV SCANNER_NB_HOURS=1      
ENV LOOK_FROM_DATE=
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=1
ENV NOTIFY_TO=
ENV NOTIFY_ENDPROCESS=0		
# The name of the queue the scanner listens to. By default it should be /queue/scanner
ENV ACTIVEMQ_QUEUE_NAME=/queue/scanner
# Default of this is activemq to connect to the activemq service deployed on Kubernetes
ENV ACTIVEMQ_HOST=activemq


WORKDIR /root
ENTRYPOINT ["python","scanner_worker.py"]