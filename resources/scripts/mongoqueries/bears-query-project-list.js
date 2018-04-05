db.inspector.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  $and: [ 
			  	{
				   "buildFinishedDate": {
				     $gt: ISODate("2018-01-01T00:00:00.000+0000")
				   }
			  	},
			  	{
			  	  "status": {
			                $in: ["PATCHED", "test errors", "test failure", "NOTFAILING"]
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
			    counted: {$sum: 1},
			    PR: { $sum: "$isPR" }
			}
		},

		// Stage 4
		{
			$sort: {
			    counted: -1
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
