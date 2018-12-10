# Manage Database

Current DB is saved on librepair machine.

## Save MongoDB on librepair

Current backups are saved on `/mnt/backup/repairnator/db` through a cron every 24 hours.
The current script for saving database is located in `/root/backup-repairnator.sh`. 

The script backup is just a command on the form: 
```bash
mongodump -u *** -p *** --db repairnator --gzip --archive=/mnt/backup/repairnator/db/repairnator_`date +"%y-%m-%d"`.gz
```

### Restoring backup

In order to restore the backup, a command on the following form should be used:

```bash
mongorestore -u *** -p *** --authenticationDatabase "***"  --gzip --archive=/tmp/repairnator_18-03-23.gz
```

### Make a backup remotely 

In order to be sure of having the exact same version of mongodb we recommend to use a docker image.
```bash
docker run --rm -v $(pwd):/workdir/ -w /workdir/ -it mongo:4.0.1 bash    
```

Then you can dump the database :
```bash
mongodump --uri mongodb://[user]:[password]@130.239.81.199/[database]
```
