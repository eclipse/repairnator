# librepair-site

All stuff necessary to librepair website.

## Disclaimer

This website was created to display statistics on Repairnator, not to be something really easy to maintain.
Pay attention of changing the API URL if you want to deploy this website elsewhere.

## Deployment

```
# install gulp and bower
$ npm install -g gulp bower

# install dependencies
$ npm install
$ bower install

# run gulp
$ gulp

# Copy the dist directory content
$ cp dist/* /var/www/website
```
