# Repairnator GitHub App

The Repairnator GitHub App provides support to automatically run the Repairnator pipeline on each failing build:

1. Detect failing builds
2. Search for a patch
3. If a valid patch is found, create a pull-request on Github

It does so by pushing failing travisCI build identifiers to an ActiveMQ server. A worker pulls them for the repair attemtps.

## How to Install the Repairnator GitHub App?

Required steps:

1. visit https://github.com/apps/repairnator-bot
2. click on the green button "Install"
3. Activate Travis CI on your repo (<https://travis-ci.org/account/repositories> or <https://travis-ci.com/account/repositories>)

__Permissions__ In order to listen on specific events with this app, it is required to enable permissions of `Issues`, `Pull requests` and `Commit statuses`, to `Read & Write` for all of them at <https://github.com/settings/installations> (click on repairnator-bot).

## How to Use the Repairnator GitHub App?

1. trigger a Travis CI build: for example create a PR, reopen a PR, make commits inside a PR, for example
2. if the travis CI build is __failing__ and the language setting in `.travis.yml` is __java__, then a comment will be posted to the pull request
3. for this failing build, if Repairnator can find one valid patch, it will create one PR for the current PR

## Developer Documentation

### Architecture Overview

The server listens to several GitHub webhooks, and execute corresponding scripts based on the events. The most important script is to request travisCI's Build info via Travis API. For each failing java-language build, the server will push its buildId to Repairnator's ActiveMQ. Repairnator-pipeline will pick up each buildId and invoke repair tools to generate possible patches. If valid patches are found, then corresponding pull-requests will be created on GitHub.

### Run the app on your own

```sh
cd  repairnator/repairnator-github-app/

# Install dependencies
npm install

# Run the bot (see below)
npm start
```

For running the app on you own, you need to fill
* Webhook URL: The webhook  URL is set at `https://github.com/organizations/repairnator/settings/apps/<your-app>`.
* __`.env` file__ This GitHub app requires `.env` file. The detailed introducation is [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Two pieces of information need to be filled: 
  * one TRAVIS API Token
  * the Webhook Secret field from `Settings > Developer > settings > GitHub Apps > repairnator-bot` .

### Commands
How to run the NodeJS server for Repairnator-bot?
1. clone the repairnator-bot repo
2. add `.env` and `.private-key.pem`
3. go to `~/repairnator-bot`
4. run `nohup npm start &`

How to run the ActiveMQ service?
1. download and unzip activemq at the home folder
2. go to `~/apache-activemq-5.15.11/bin` (check `apache-activemq-5.15.11/conf/jetty-realm.properties` to see admin's password)  
3. run `./activemq start`
4. visit the ActiveMQ's web console (http://{ip}:8161/admin/)

How to update the app?
1. make updates inside the project (`git pull origin`)
2. stop the server (`ps -ef | grep npm` and `ps -ef | grep node` to kill processes manually, then re-run the server

What to do when Repairnator GitHub app does not work normally?
One cause is the "403 - access denied" issue
1. edit `~/repairnator-bot/.env`
2. try other TRAVIS tokens (the Travis API is not stable)
3. contact the travis support<support@travis-ci.com>

### Reference

- https://probot.github.io/docs/
- https://developer.travis-ci.com/
