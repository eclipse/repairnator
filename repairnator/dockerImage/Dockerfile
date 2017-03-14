FROM maven:3.3.9-jdk-8

LABEL Description="Repairnator Pipeline docker image" Vendor="Spirals" Version="0.0.0"

VOLUME ["/var/log/"]

RUN mkdir /root/.credentials
ADD .credentials /root/.credentials
ADD client_secret.json /root
ADD repairconfig.ini /root
ADD repairnator-pipeline.jar /root
ADD z3_for_linux /root
ADD logback.xml /root

ENV M2_HOME=$MAVEN_HOME
ENV GITHUB_LOGIN=
ENV GITHUB_OAUTH=
ENV BUILD_ID=
ENV PREVIOUS_BUILD_ID=
ENV LOG_FILENAME=

WORKDIR /root
ENTRYPOINT java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar -Dlogback.configurationFile=/root/logback.xml fr.inria.spirals.repairnator.pipeline.Launcher -m repair -d -b $BUILD_ID
