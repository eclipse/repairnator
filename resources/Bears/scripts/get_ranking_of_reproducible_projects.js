db.inspector.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			    $and: [
			        {
			            "buildFinishedDate": {
			                  $gte: ISODate("2018-01-01T00:00:00.000+0000")
			            }
			        },
			        {
			            "buildFinishedDate": {
			                  $lt: ISODate("2018-05-01T00:00:00.000+0000")
			            }
			        },
			        {
			            "status": {
			                $in: ["NOTFAILING", "test failure", "test errors"]
			            }
			        }
			    ]
			}
		},

		// Stage 2
		{
			$group: {
			    _id: "$repositoryName", count: {$sum: 1}
			}
		},

		// Stage 3
		{
			$sort: {
			    count: -1
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
