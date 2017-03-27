FROM maven:3.3.9-jdk-8

LABEL Description="Repairnator Pipeline docker image" Vendor="Spirals" Version="0.0.0"

VOLUME ["/var/log/"]
RUN echo "Europe/Paris" > /etc/timezone
COPY repairconfig.ini /root/
COPY repairnator-pipeline.jar /root/
COPY z3_for_linux /root/
COPY logback.xml /root/
COPY configure_git.sh /root/

RUN chmod +x /root/configure_git.sh
RUN /root/configure_git.sh

ENV M2_HOME=$MAVEN_HOME
ENV GITHUB_LOGIN=
ENV GITHUB_OAUTH=
ENV BUILD_ID=
ENV LOG_FILENAME=
ENV RUN_ID=
ENV GOOGLE_ACCESS_TOKEN

WORKDIR /root
ENTRYPOINT java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar -Dlogback.configurationFile=/root/logback.xml fr.inria.spirals.repairnator.pipeline.Launcher -m repair -d -b $BUILD_ID --runId $RUN_ID -g $GOOGLE_ACCESS_TOKEN
