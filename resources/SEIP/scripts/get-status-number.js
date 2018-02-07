db.inspector.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
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
			}
		},

		// Stage 2
		{
			$group: {
				_id: "$status",
				count: { $sum: 1}
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
