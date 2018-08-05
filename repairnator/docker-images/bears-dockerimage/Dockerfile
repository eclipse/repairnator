FROM maven:3.3.9-jdk-8

LABEL Description="Repairnator Pipeline docker image" Vendor="Spirals" Version="0.0.0"

VOLUME ["/var/log/"]

COPY z3_for_linux /root/
COPY logback.xml /root/
COPY configure_git.sh /root/
COPY build_repairnator4Bears.sh /root/
COPY pipeline_launcher4Bears.sh /root/
COPY version.ini /root/

RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone 
RUN apt-get update 
RUN apt-get install cloc -y 
RUN /root/configure_git.sh 
RUN /root/build_repairnator4Bears.sh

ENV M2_HOME=$MAVEN_HOME
ENV GITHUB_OAUTH=
ENV REPAIR_MODE=
ENV BUILD_ID=
ENV NEXT_BUILD_ID=
ENV LOG_FILENAME=
ENV LOG_LEVEL=
ENV RUN_ID=
ENV MONGODB_HOST=
ENV MONGODB_NAME=
ENV OUTPUT=
ENV PUSH_URL=
ENV SMTP_SERVER=
ENV NOTIFY_TO=
ENV GITHUB_USERNAME=
ENV GITHUB_USEREMAIL=

WORKDIR /root
ENTRYPOINT /root/pipeline_launcher4Bears.sh
