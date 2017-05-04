FROM maven:3.3.9-jdk-8

LABEL Description="Repairnator Checkbranch docker image" Vendor="Spirals" Version="0.0.0"
COPY check_branches.sh /root/

RUN echo "Europe/Paris" > /etc/timezone && chmod +x /root/*.sh

ENV REPOSITORY=

WORKDIR /root
ENTRYPOINT /root/check_branches.sh $REPOSITORY /tmp/outputcheck.txt