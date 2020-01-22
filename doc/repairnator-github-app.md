# Repairnator GitHub App

The Repairnator GitHub App can be used:

1. to push failing travisCI buildId to ActiveMQ
2. to run Repairnator pipeline on each failing build
3. to create pull-requests on Github when valid patches are found by Repairnator's repair tools

## How to Install?

1. visit https://github.com/apps/repairnator-bot
2. click on the green button "Install"

## How to Use?

1. trigger one travisCI build on GitHub PR, such as create PR, reopen PR, make commits inside PR
2. if the travisCI build is __failing__ and the language setting in `.travis.yml` is __java__, then a comment will be posted above the info board of current travisCI build
3. for this failing build, if Repairnator can found one valid patch, it will create one PR for current PR

## Architecture Overview (for developers)

The server listens to several GitHub webhooks, and execute corresponding scripts for different events. The most important script is to request travisCI's Build info via Travis API. For each failing java-language build, the server will push its buildId to Repairnator's ActiveMQ. Repairnator-pipeline will pick up each buildId and invoke repair tools to generate possible patches. If valid patches are found, then corresponding pull-requests will be created on GitHub.

### Setup Server

```sh
# Install dependencies
npm install

# Run the bot
npm start
```

__where to run__ It is required to run this NodeJS server at the same machine where the Repairnator pipeline runs with its ActiveMQ.

### Key Points

__Permissions__ In order to listen on specific events with this app, it is recommended to enable permissions of `Issues`, `Pull requests` and `Commit statuses`, Read & Write for all of them.

__Webhook URL__ It is using webhook payload delivery service from https://smee.io, learn more [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Some alternatives are listed [here](https://probot.github.io/docs/deployment/#deploy-the-app). The webhook URL is required at `Settings > Developer > settings > GitHub Apps > repairnator-bot`.

__`.env` file__ This GitHub app is based on Probot, and this framework requires `.env` file. The detailed introducation is [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Meanwhile, one TRAVIS API Token needs to be there. In addition, because of requirements of Probot, the Webhook Secret field at `Settings > Developer > settings > GitHub Apps > repairnator-bot` needs to be filled.

### Reference

- https://probot.github.io/docs/
- https://developer.travis-ci.com/
