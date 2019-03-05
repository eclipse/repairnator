FROM maven:3.3.9-jdk-8

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
ENV GITHUB_OAUTH=
ENV REPAIR_MODE=
ENV BUILD_ID=
ENV LOG_FILENAME=
ENV RUN_ID=
ENV MONGODB_HOST=
ENV MONGODB_NAME=
ENV OUTPUT=
ENV PUSH_URL=
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=
ENV NOTIFY_TO=
ENV REPAIR_TOOLS=
ENV GITHUB_USERNAME=
ENV GITHUB_USEREMAIL=
ENV CREATE_PR=0
ENV ACTIVEMQ_QUEUE_NAME=/queue/build_id

WORKDIR /root
# ENTRYPOINT /root/pipeline_launcher.sh
ENTRYPOINT ["python","pipeline_worker.py"]