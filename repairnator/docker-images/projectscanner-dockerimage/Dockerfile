FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image for k8s" Vendor="Spirals" Version="0.0.0"
LABEL maintainer="Henry Luong <tailp@kth.se>"

VOLUME ["/var/log/"]

COPY configure_git.sh /root/
COPY build_scanner.sh /root/
COPY scanner_launcher.sh /root/

RUN touch /root/project_list.txt
RUN touch /root/z3_for_linux
RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone
RUN apt-get update
RUN apt-get install cloc -y
RUN /root/configure_git.sh
RUN /root/build_scanner.sh

# Possible values: NOOP (does nothing, usual project scanner), KUBERNETES (receive/send to ActiveMQ)
ENV PIPELINE_MODE=NOOP

# Github information is provided if the repaired builds should be submitted on some repo
# optional for something can be empty
# Number of hours to inspect to get last builds (e.g. 4 will mean 4 hours in the past)

# Can be specified later in repairnator-scanner.yaml file later when deploying
ENV MONGODB_HOST=mongodb://mongo:27017 
ENV MONGODB_NAME=repairnator

# Fixed values does not need to be changed
ENV M2_HOME=$MAVEN_HOME
ENV REPAIRNATOR_RUN_DIR=/root/ 
ENV LOG_DIR=/root/ 
ENV REPAIR_PROJECT_LIST_PATH=/root/project_list.txt   
ENV REPAIRNATOR_BUILD_LIST=/root/build_list.txt	 
ENV RUN_ID=0
ENV ACTIVEMQ_LISTEN_QUEUE_NAME=scanner
ENV ACTIVEMQ_SUBMIT_QUEUE_NAME=pipeline
ENV ACTIVEMQ_URL=tcp://activemq:61616

# Optional can either be changed or left as it is in repairnator-scanner.yaml 
ENV SCANNER_NB_HOURS=1      
ENV LOOK_FROM_DATE=
ENV JAVA_OPTS=  
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=1
ENV NOTIFY_TO=
ENV NOTIFY_ENDPROCESS=0	

WORKDIR /root
ENTRYPOINT /root/scanner_launcher.sh