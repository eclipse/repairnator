FROM maven:3.6.1-jdk-8

LABEL Description="Repairnator Pipeline docker image" Vendor="Spirals" Version="0.0.0"

VOLUME ["/var/log/"]

COPY z3_for_linux /root/
COPY projects_to_ignore.txt /root/
COPY logback.xml /root/
COPY configure_git.sh /root/
COPY build_repairnator.sh /root/
COPY pipeline_launcher.sh /root/

RUN apt-get update 
RUN apt-get install xmlstarlet apt-utils cloc -y 
RUN curl -O -s https://oss.sonatype.org/content/repositories/snapshots/fr/inria/repairnator/repairnator-pipeline/maven-metadata.xml
RUN echo `xmlstarlet sel -t -v '//latest' maven-metadata.xml` > /root/version.ini
RUN chmod +x /root/*.sh
RUN echo "Europe/Paris" > /etc/timezone 
RUN /root/configure_git.sh 
RUN /root/build_repairnator.sh

ENV M2_HOME=$MAVEN_HOME
ENV GITHUB_OAUTH=
ENV REPAIR_MODE=
ENV BUILD_ID=
ENV LOG_FILENAME=
ENV RUN_ID=1
ENV MONGODB_HOST=
ENV MONGODB_NAME=
ENV OUTPUT=
ENV PUSH_URL=
ENV SMTP_SERVER=
ENV SMTP_PORT=
ENV SMTP_USERNAME=
ENV SMTP_PASSWORD=
ENV SMTP_TLS=1
ENV NOTIFY_TO=
ENV REPAIR_TOOLS=
ENV GITHUB_USERNAME=
ENV GITHUB_USEREMAIL=
ENV EXPERIMENTAL_PLUGIN_REPOS=
ENV CREATE_PR=0

# NOOP = regular repairnator, KUBERNETES = will fetch build id from activeMQ.
ENV LISTEN_MODE=NOOP
ENV ACTIVEMQ_URL=
ENV ACTIVEMQ_LISTEN_QUEUE=
ENV ACTIVEMQ_USERNAME=
ENV ACTIVEMQ_PASSWORD=

WORKDIR /root
ENTRYPOINT /root/pipeline_launcher.sh
