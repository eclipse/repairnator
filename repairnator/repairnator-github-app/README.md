# repairnator-bot

![repairnator-bot](logo.png)

> A GitHub App built with [Probot](https://github.com/probot/probot) that Software development bot that automatically repairs programs and build failures on Travis Continuous Integration (https://github.com/eclipse/repairnator)

## Setup

```sh
# Install dependencies
npm install

# Run the bot
npm start
```

__where to run__ It is required to run this NodeJS server at the same machine where the Repairnator pipeline runs with its ActiveMQ.

## Summary

__Permissions__ In order to listen on specific events with this app, it is recommended to enable permissions of `Issues`, `Pull requests` and `Commit statuses`, Read & Write for all of them.

__Webhook URL__ It is using webhook payload delivery service from https://smee.io, learn more [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Some alternatives are listed [here](https://probot.github.io/docs/deployment/#deploy-the-app). The webhook URL is required at `Settings > Developer > settings > GitHub Apps > repairnator-bot`.

__`.env` file__ This GitHub app is based on Probot, and this framework requires `.env` file. The detailed introducation is [here](https://probot.github.io/docs/development/#manually-configuring-a-github-app). Meanwhile, one TRAVIS API Token needs to be there. In addition, because of requirements of Probot, the Webhook Secret field at `Settings > Developer > settings > GitHub Apps > repairnator-bot` needs to be filled.

## How to Install

1. visit https://github.com/apps/repairnator-bot
2. click on the green button "Install"

## How to Use

1. trigger one travisCI build on GitHub PR (__failing build__ and __java language__)
2. a comment will be posted below, and Repairnator will take care of that PR

## Reference

- https://probot.github.io/docs/
- https://developer.travis-ci.com/
- https://github.com/jianguda/repairnator-bot/
- https://dribbble.com/shots/3475558-Robo-Squad-CSGO-Team-Logo

## Contributing

If you have suggestions for how repairnator-bot could be improved, or want to report a bug, open an issue! We'd love all and any contributions.

For more, check out the [Contributing Guide](CONTRIBUTING.md).

## License

[ISC](LICENSE) © 2020 Jian GU <jianguda@gmail.com>
