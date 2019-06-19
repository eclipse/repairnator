FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image for k8s" Vendor="Spirals" Version="0.0.0"
LABEL maintainer="Long Zhang <longz@kth.se>"

VOLUME ["/var/log/"]

COPY z3_for_linux /root/
COPY projects_to_ignore.txt /root/
COPY logback.xml /root/
COPY configure_git.sh /root/
COPY build_repairnator.sh /root/
COPY install_python_stomp.sh /root/
COPY pipeline_launcher.sh /root/
COPY pipeline_worker.py /root/
COPY version.ini /root/

RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone
RUN apt-get update
RUN apt-get install cloc -y
RUN /root/configure_git.sh
RUN /root/install_python_stomp.sh
RUN /root/build_repairnator.sh

ENV M2_HOME=$MAVEN_HOME
# This is by default as "mongodb://mongo:27017" if deployed with the provided mongodb.yaml on Kubernetes.
ENV MONGODB_HOST=mongodb://mongo:27017 
# Name of the database you want to store information, can be found by "show dbs" after logging into mongodb
ENV MONGODB_NAME=
# Github information is provided if the repaired builds should be submitted on some repo
ENV GITHUB_OAUTH=
ENV GITHUB_USERNAME=
ENV GITHUB_USEREMAIL=
# Default mode is repair
ENV REPAIR_MODE=repair
# This should be left empty since the build id is provided by the user or the scanner it will change anyway.
ENV BUILD_ID=
# This should be 0 by default but not empty, otherwise it will be error. We don't need to distinguis different repairnator run
# in our case
ENV RUN_ID=0
# This should not be changed. It's hardcoded with pipeline_worker.py to exacly remove the folder pipeline_output after each run
# To avoid piling up stuffs inside the container.
ENV OUTPUT=pipeline_output
# This can be renamed as you like
ENV LOG_FILENAME=LOG
ENV PUSH_URL=
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=
ENV NOTIFY_TO=
# There are more repair tools to use, if interested check repo doc.
ENV REPAIR_TOOLS=NopolAllTests,AstorJMut,NPEFix
# If 1 this will create a PR to the repo associated with the repaired Build Id.
ENV CREATE_PR=0
# This should not be changed, since it's hardcoded in pipeline_worker.py 
ENV ACTIVEMQ_QUEUE_NAME=/queue/pipeline

WORKDIR /root
# ENTRYPOINT /root/pipeline_launcher.sh
ENTRYPOINT ["python","pipeline_worker.py"]