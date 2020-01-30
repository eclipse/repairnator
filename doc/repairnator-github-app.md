# Repairnator GitHub App

The Repairnator GitHub App provides support to automatically run the Repairnator pipeline on each failing build:

1. Detect failing builds
2. Search for a patch
3. If a valid patch is found, create a pull-request on Github

It does so by pushing failing travisCI build identifiers to an ActiveMQ server. A worker pulls them for the repair attemtps.

## How to Install the Repairnator GitHub App?

1. visit https://github.com/apps/repairnator-bot
2. click on the green button "Install"

__Permissions__ In order to listen on specific events with this app, it is required to enable permissions of `Issues`, `Pull requests` and `Commit statuses`, to `Read & Write` for all of them.

## How to Use the Repairnator GitHub App?

1. trigger a Travis CI build: for example create a PR, reopen a PR, make commits inside a PR
2. if the travis CI build is __failing__ and the language setting in `.travis.yml` is __java__, then a comment will be posted to the pull request
3. for this failing build, if Repairnator can find one valid patch, it will create one PR for the current PR

## Architecture Overview (for developers)

The server listens to several GitHub webhooks, and execute corresponding scripts based on the events. The most important script is to request travisCI's Build info via Travis API. For each failing java-language build, the server will push its buildId to Repairnator's ActiveMQ. Repairnator-pipeline will pick up each buildId and invoke repair tools to generate possible patches. If valid patches are found, then corresponding pull-requests will be created on GitHub.

### Run the app on your own

```sh
cd  repairnator/repairnator-github-app/

# Install dependencies
npm install

# Run the bot
npm start
```

For running the app on you own, you need to fill
* Webhook URL: The webhook  URL is set at `https://github.com/organizations/repairnator/settings/apps/<your-app>`.
* __`.env` file__ This GitHub app requires `.env` file. The detailed introducation is [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Two pieces of information need to be filled: 
  * one TRAVIS API Token
  * the Webhook Secret field from `Settings > Developer > settings > GitHub Apps > repairnator-bot` .

### Deployment on VM

* __How to run the NodeJS server for Repairnator-bot?__
1. clone the repairnator-bot repo
2. add `.env` and `.private-key.pem`
3. go to `~/repairnator-bot`
4. run `nohup npm start &`

* __How to run the ActiveMQ service?__
1. download and unzip activemq at the home folder
2. go to `~/apache-activemq-5.15.11/bin`
3. run `./activemq start`

* __How to reproduce app's demo step by step?__
1. make sure the NodeJS server is running and the ActiveMQ service is on
2. fork `https://github.com/jianguda/repairnator-bot-test`
3. install `repairnator-bot` app on your forked repo
4. connect travis-ci.org to your forked repo and open the trigger
5. create one PR from the failure branch to the master branch
6. wait at the PR page for repairnator-bot's comment
7. visit the ActiveMQ's web console (http://{ip}:8161/admin/) (check `apache-activemq-5.15.11/conf/jetty-realm.properties` to see admin's password)  
8. click Queues in the breadcrumbs, then click pipeline and then click the latest message, and check the corresponding buildID at the Message Details

* __What to do when Repairnator GitHub app does not work normally?__
The only possible cause as Jian<jianguda@gmail.com> knows is the "403 - access denied" issue
1. edit `~/repairnator-bot/.env`
2. try other TRAVIS tokens (travis API is not stable)
3. contact the travis support<support@travis-ci.com>

### Reference

- https://probot.github.io/docs/
- https://developer.travis-ci.com/
