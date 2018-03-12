db.bearmetrics.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
				"FailureNames": { $exists: true }
			}
		},

		// Stage 2
		{
			$group: {
			  _id: "nbFiles",
			  nbFileApp: { $avg: "$NbFileApp" },
			  nbFileTest: { $avg: "$NbFileTests" },
			  nbLibraries: { $avg: "$NbLibraries" },
			  nbRunningTest: { $avg: "$NbRunningTests" }
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
