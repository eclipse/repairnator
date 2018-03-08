db.inspector4bears.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			"status": /.*fixer.*/i
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
