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

