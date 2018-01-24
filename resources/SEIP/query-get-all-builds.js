db.inspector.find({
    $and: [
        {
            "buildFinishedDate": {
                $gt: ISODate("2017-02-01T00:00:00.000+0000")
            }
        }
        ,
        {
            "buildFinishedDate": {
                $lte: ISODate("2018-01-01T00:00:00.000+0000")
            }
        }
    ]
});