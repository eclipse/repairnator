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
			$addFields: {
			    isPR: { $cond: { if: { $gt: ["$prNumber", 0] }, then: 1, else: 0 }}
			}
		},

		// Stage 3
		{
			$group: {
				_id: "$repositoryName",
				count: { $sum: 1},
				nbPR: { $sum: "$isPR" }
			}
		},

		// Stage 4
		{
			$sort: {
				count: -1
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
