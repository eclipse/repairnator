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
ENV MONGODB_HOST=
ENV MONGODB_NAME=
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=1
ENV NOTIFY_TO=
ENV NOTIFY_ENDPROCESS=0		
# 1 if user want to notify to the email when the scanner ends
ENV REPAIR_PROJECT_LIST_PATH=/root/project_list.txt   
# This is the file containing the repos to be scanned
ENV REPAIRNATOR_BUILD_LIST=/root/build_list.txt	  
# this is the file containing buildids for the pipeline to run
ENV RUN_ID=
ENV GITHUB_OAUTH=
# optional for something can be empty
ENV JAVA_OPTS= 
ENV REPAIRNATOR_RUN_DIR=/root/ 
# this is always /root/ , everything will be deleted afterward by the python script.
ENV LOG_DIR=/root/ 
# also /root/
ENV SCANNER_NB_HOURS=1      
# Number of hours to inspect to get last builds (e.g. 4 will mean 4 hours in the past)
ENV LOOK_FROM_DATE=
# The name of the queue the scanner listens to. By default it should be /queue/scanner
ENV ACTIVEMQ_QUEUE_NAME=/queue/scanner
# Default of this is activemq to connect to the activemq service deployed on Kubernetes
ENV ACTIVEMQ_HOST=activemq


WORKDIR /root
ENTRYPOINT ["python","scanner_worker.py"]