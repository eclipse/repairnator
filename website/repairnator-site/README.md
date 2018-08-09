# librepair-site

All stuff necessary to librepair website.

## Disclaimer

This website was created to display statistics on Repairnator, not to be something really easy to maintain.
Then currently all the call to the REST API are done with hard-coded URLs in the scripts. 
Pay attention of changing those URLs if you want to deploy this website elsewhere.

## Deployment

```
# install gulp
$ npm install -g gulp

# run gulp
$ gulp

# Copy the dist directory content
$ cp dist/* /var/www/website
```