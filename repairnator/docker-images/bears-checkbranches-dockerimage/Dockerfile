FROM maven:3.3.9-jdk-8

LABEL Description="Bears checkbranch docker image" Vendor="Spirals" Version="0.0.0"

COPY check_branches.sh /root/
COPY bears-schema.json /root/

RUN echo "Europe/Paris" > /etc/timezone && chmod +x /root/*.sh
RUN apt-get update
RUN apt-get install curl -y
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -
RUN apt-get install nodejs
RUN npm install -g ajv-cli

ENV M2_HOME=$MAVEN_HOME

ENV REPOSITORY=
ENV BRANCH_NAME=

WORKDIR /root
ENTRYPOINT /root/check_branches.sh $REPOSITORY $BRANCH_NAME