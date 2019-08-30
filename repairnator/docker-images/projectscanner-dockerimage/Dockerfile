FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image for k8s" Vendor="Spirals" Version="0.0.0"
LABEL maintainer="Henry Luong <tailp@kth.se>"

VOLUME ["/var/log/"]

RUN touch /root/project_list.txt
RUN touch /root/z3_for_linux
RUN echo "Europe/Paris" > /etc/timezone
RUN apt-get update
RUN apt-get install cloc -y
# Configure git 
RUN git config --global core.whitespace trailing-space,space-before-tab
RUN git config --global apply.whitespace fix
# Build scanner
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-scanner:LATEST:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots/ -Ddest=/root/repairnator-projectscanner.jar

# Possible values: NOOP (does nothing, usual project scanner), KUBERNETES (receive/send to ActiveMQ)
ENV PIPELINE_MODE=NOOP

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

## Optional can either be changed or left as it is in repairnator-scanner.yaml 
# Number of hours to inspect to get last builds (e.g. 4 will mean 4 hours in the past)
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
ENTRYPOINT java $JAVA_OPTS -jar /root/repairnator-projectscanner.jar -d -i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID --pipelinemode $PIPELINE_MODE --activemqurl $ACTIVEMQ_URL --activemqlistenqueuename $ACTIVEMQ_LISTEN_QUEUE_NAME --activemqqueuename $ACTIVEMQ_SUBMIT_QUEUE_NAME --dbhost $MONGODB_HOST --dbname $MONGODB_NAME -l $SCANNER_NB_HOURS --smtpTLS