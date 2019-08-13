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
# HOW TO RUN
# docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker pipeline-dockerimage
# RUN apt-get install -y libltdl7
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
ENV EXPERIMENTAL_PLUGIN_REPOS=
ENV CREATE_PR=0

WORKDIR /root
ENTRYPOINT /root/pipeline_launcher.sh
