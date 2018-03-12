db.inspector.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$addFields: {
			    isPR: { $cond: { if: { $gt: ["$prNumber", 0] }, then: 1, else: 0 }}
			}
		},

		// Stage 2
		{
			$group: {
				_id: "$repositoryName",
				counted: {$sum: 1},
				PR: { $sum: "$isPR" }
			}
		},

		// Stage 3
		{
			$sort: {
				counted: -1
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
