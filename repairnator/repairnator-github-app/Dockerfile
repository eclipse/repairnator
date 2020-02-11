FROM node:12

LABEL Description="github-app docker image" Vendor="repairnator" Version="1.0.0"

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

RUN echo "Europe/Paris" > /etc/timezone
RUN npm install
# If you are building your code for production
# RUN npm ci --only=production

# Bundle app source
COPY . .

EXPOSE 3000
CMD [ "node", "start" ]

# ${pwd} is the absolute path where <.env> and <.private-key.pem> exist
# ${pwd}=</home/repairnator/repairnator-bot> if it runs on the same machine
# docker run -v ${pwd}:/usr/src/app -p 3000:3000 -d repairnator/github-app:latest

# repairnator/github-app:latest
# https://hub.docker.com/repository/docker/repairnator/github-app
