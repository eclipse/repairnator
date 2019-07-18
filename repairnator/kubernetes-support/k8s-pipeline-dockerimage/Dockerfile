FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image for k8s" Vendor="Spirals" Version="0.0.0"
LABEL maintainer="Long Zhang <longz@kth.se>"

VOLUME ["/var/log/"]


COPY projects_to_ignore.txt /root/
COPY logback.xml /root/
COPY configure_git.sh /root/
COPY build_repairnator.sh /root/
COPY install_python_stomp.sh /root/
COPY pipeline_launcher.sh /root/
COPY pipeline_worker.py /root/
COPY version.ini /root/

RUN touch /root/z3_for_linux
RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone
RUN apt-get update
RUN apt-get install cloc -y
RUN /root/configure_git.sh
RUN /root/install_python_stomp.sh
RUN /root/build_repairnator.sh

# Can add more tools or remove some. Need at least 1 tool otherwise can be left as it is.
ENV REPAIR_TOOLS=NopolAllTests,AstorJMut,NPEFix
# Need to be specified if planning to push or create PR to Git with push url later
# Since this will be used to setup Git during container building
ENV GITHUB_USERNAME=Repairnator
ENV GITHUB_USEREMAIL=tailp@kth.se
ENV CREATE_PR=0 

# Can be set in repairnator-pipeline.yaml file
ENV MONGODB_HOST=mongodb://mongo:27017
ENV MONGODB_NAME=admin
ENV GITHUB_OAUTH=
ENV PUSH_URL=
# Optional can be specified in the repairnator-pipeline.yaml file or left as it is
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=1
ENV NOTIFY_TO=

# Fixed values does not need to change
ENV M2_HOME=$MAVEN_HOME
ENV REPAIR_MODE=repair
ENV LOG_FILENAME=pipeline_log
ENV OUTPUT=pipeline_output
ENV ACTIVEMQ_QUEUE_NAME=/queue/pipeline
ENV RUN_ID=0

# Does not need to be specified this will be updated by ActiveMQ message for each new build recieved
ENV BUILD_ID=

WORKDIR /root
ENTRYPOINT ["python","pipeline_worker.py"]


