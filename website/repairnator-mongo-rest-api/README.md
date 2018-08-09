# LibRepair Data API

This repository will contain codes for librepair mongoDB data API. 

The code is directly forked from: https://github.com/KunalKapadia/express-mongoose-es6-rest-api, thanks for the great works done there!

## Deployment

```
# create the proper env file (needed for the MongoDB credentials)
1. cp .env.example .env

# compile to ES5
2. yarn build

# upload dist/ to your server
3. scp -rp dist/ user@dest:/path

# install production dependencies only
4. yarn --production

# Use any process manager to start your services
5. pm2 start dist/index.js
```

More info on the original repository.

## Endpoints

The default API path is `repairnator-mongo-api`.

### /health-check

Check the server health, it returns only "OK" if everything's ok.

### /inspectors/

Return the data of the 50 last data in inspector collection. 

### /inspectors/hostnameStats

This endpoint compute statistics about the hostname used to run the Repairnator inspector.
The returned stats concern all data in the database.
Return an object of the form: 

```json
[
{
  "_id": "xx.xx.host1",
  "counted": 42
}, {
  "_id": "xx.xx.host2",
  "counted": 24
}
]
```

### /inspectors/statusStats

This endpoint compute statistics about the states reached at the end of the Repairnator inspector.
The returned stats concern all data in the database.
Return an object of the form: 

```json
[
{
  "_id": "STATUS1",
  "counted": 18
}, {
  "_id": "STATUS2",
  "counted": 1356
}
]
```

### /inspectors/statusStats/:nbDays

This endpoint is used with an argument `:nbDays` which must be an integer.
The endpoint gives the same answer as the previous one, except the stats are computed for the given number of days.
E.g: use `/inspectors/statusStats/7` to get the stats for a week.

### /inspectors/reproducedBuilds

This endpoint computes the number of reproduced build (test failure, test error or patched) day by day. 
The returned data concern all date in the database.

It returns an object of the form:

```json
[
{
  "_id": "2017-02-01",
  "counted": 2
}, {
  "_id": "2017-02-02",
  "counted": 1
}
]
```

### /inspectors/reproducedBuilds/:nbDays

This endpoint is used with an argument `:nbDays` which must be an integer.
The endpoint gives the same answer as the previous one, except the stats are computed for the given number of days.
E.g: use `/inspectors/reproducedBuilds/7` to get the stats for a week.

### /inspectors/uniqueBuilds

This endpoint returns the number of unique builds computed by Repairnator in all time.

It returns an object of the form:

```json
[{
  "_id": "nbBuilds",
  "count": 14355
}]
```

### /inspectors/:inspectorId

This endpoint allows to request a unique inspector object from its buildId.

E.g: `inspectors/343674821` returns a complete inspector object like this:

```json 
{
  "_id": "5a8ba62046e0fb001046807d",
  "buildId": 343674821,
  "repositoryName": "snuspl/coral",
  "status": "test errors",
  "prNumber": "0",
  "buildFinishedDateStr": "20/02/18 05:32",
  "buildFinishedDate": "Tue Feb 20 2018 06:32:40 GMT+0100 (CET)",
  "buildFinishedDay": "20/02/2018",
  "realStatus": "NOPOL_NOTPATCHED",
  "hostname": "spirals-librepair",
  "buildReproductionDateStr": "20/02/18 05:37",
  "buildReproductionDate": "Tue Feb 20 2018 06:37:52 GMT+0100 (CET)",
  "travisURL": "http://travis-ci.org/snuspl/coral/builds/343674821",
  "typeOfFailures": "java.util.concurrent.ExecutionException",
  "runId": "ee5bc102-7e4b-43dc-8d31-003c869cc1f3_realtime",
  "branchURL": "https://github.com/Spirals-Team/librepair-experiments/tree/snuspl-coral-343674821-20180220-053240"
}
```

### /inspectors/patches

Return all inspector data with a `PATCHED` status. 

### /inspectors/failuresByProject

Return the number of detected failing builds by project and the associated pull request number.

It returns an object of the form:

```json
[
{
  "_id": "prestodb/presto",
  "count": 1144,
  "nbPR": 995
}, {
  "_id": "druid-io/druid",
  "count": 663,
  "nbPR": 518
}
]
```

### /inspectors/reproducedByProject

Return the number of reproduced failing builds by project.

It returns an object of the form:

```json
[
{
  "_id": "apache/flink",
  "count": 406
}, {
  "_id": "druid-io/druid",
  "count": 402
}
]
```

### /scanners/

This endpoint returns the whole data from `scanner` collection.
:warning: this endpoint should be carefully used as it collect and returns a lot of data.

It returns an object of the form:

```json
[
{
  "_id": "5a5f3ccf7bb0706d6090d76f",
  "hostname": "spirals-librepair",
  "dateBeginStr": "17/01/18 13:01",
  "dateBegin": "2018-01-17T13:01:55.625Z",
  "dateLimitStr": "17/01/18 12:01",
  "dateLimit": "2018-01-17T12:01:55.623Z",
  "dayLimit": "17/01/2018",
  "totalRepoNumber": 281,
  "totalRepoUsingTravis": 276,
  "totalScannedBuilds": 35,
  "totalJavaBuilds": 33,
  "totalJavaPassingBuilds": 18,
  "totalJavaFailingBuilds": 5,
  "totalJavaFailingBuildsWithFailingTests": 4,
  "totalPRBuilds": 16,
  "duration": "0:6:52",
  "runId": "b4610a57-1ed2-4204-a6a1-114d6e2bb603"
}, {
  "_id": "5a5f2ea6addee75da1c25332",
  "hostname": "spirals-librepair",
  "dateBeginStr": "17/01/18 12:01",
  "dateBegin": "2018-01-17T12:01:29.830Z",
  "dateLimitStr": "17/01/18 11:01",
  "dateLimit": "2018-01-17T11:01:29.829Z",
  "dayLimit": "17/01/2018",
  "totalRepoNumber": 281,
  "totalRepoUsingTravis": 276,
  "totalScannedBuilds": 70,
  "totalJavaBuilds": 62,
  "totalJavaPassingBuilds": 24,
  "totalJavaFailingBuilds": 4,
  "totalJavaFailingBuildsWithFailingTests": 3,
  "totalPRBuilds": 28,
  "duration": "0:6:52",
  "runId": "bb8f300f-fa61-48a1-a8a8-70f8cf44e6e5"
}
]
```

### scanners/monthData

This endpoint is the same as the previous one but it only returns the value for the last month.
In case no data has been recorded on the last month, it returns an empty array.

### scanners/weeksData/:nbWeeks

This endpoint is the same as the previous ones but it returns the value for the last `nbWeeks` weeks.

### scanners/count

This endpoint returns the total number of data in `scanner` collection. 

It returns an object of the form:

```json
4008
```

### scanners/:scannerId

This endpoint returns a specific scanner entry for the given `scannerId`. The argument must be the `_id` of an object.

For example: `scanners/5a5f3ccf7bb0706d6090d76f` returns:

```json
{
  "_id": "5a5f3ccf7bb0706d6090d76f",
  "hostname": "spirals-librepair",
  "dateBeginStr": "17/01/18 13:01",
  "dateBegin": "2018-01-17T13:01:55.625Z",
  "dateLimitStr": "17/01/18 12:01",
  "dateLimit": "2018-01-17T12:01:55.623Z",
  "dayLimit": "17/01/2018",
  "totalRepoNumber": 281,
  "totalRepoUsingTravis": 276,
  "totalScannedBuilds": 35,
  "totalJavaBuilds": 33,
  "totalJavaPassingBuilds": 18,
  "totalJavaFailingBuilds": 5,
  "totalJavaFailingBuildsWithFailingTests": 4,
  "totalPRBuilds": 16,
  "duration": "0:6:52",
  "runId": "b4610a57-1ed2-4204-a6a1-114d6e2bb603"
}
```
