FROM maven:3.3.9-jdk-8

LABEL Description="Bears checkbranch docker image" Vendor="Spirals" Version="0.0.0"

COPY check_branches.sh /root/
COPY bears-schema.json /root/

RUN echo "Europe/Paris" > /etc/timezone && chmod +x /root/*.sh

ENV M2_HOME=$MAVEN_HOME

ENV REPOSITORY=
ENV BRANCH_NAME=

WORKDIR /root
ENTRYPOINT /root/check_branches.sh $REPOSITORY $BRANCH_NAME