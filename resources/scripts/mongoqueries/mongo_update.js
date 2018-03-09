// Thanks by A. Veuiller for this script! \o/

function toIsodate(crop, fieldName, collectionName) {
    // regexp format match date like: 07/04/17 13:26
    var re = new RegExp('^([0-9]{2})/([0-9]{2})/([0-9]{2})\\s([0-9]{2}:[0-9]{2})$','g');
    var update = {$set: {}};

    var fieldValue = crop[fieldName];

    if (typeof fieldValue == "string" && fieldValue != "") {
        var replaceDate = fieldValue.replace(re, '20$3-$2-$1T$4:00Z');
        update["$set"][fieldName] = new Date(replaceDate);
        update["$set"][fieldName+"Str"] = fieldValue;

        db[collectionName].update(
            {
                "_id": crop._id // Would be better with $type: "string" but doesn't work for some reason
            },
            update
        );

        print("Date updated!");
    }
}

function updateCollection(collectionName, fieldList) {

    db[collectionName].find().forEach(function (crop) {
        print("### " + crop._id + " ###");
        print("# Update date String to ISODate");

        fieldList.forEach(function (fieldName) {
            toIsodate(crop, fieldName, collectionName);
        });
        print();
    });
}

updateCollection("scanner4bears", ["dateBegin", "dateEnd", "dateLimit"]);
updateCollection("inspector4bears", ["buildFinishedDate", "buildReproductionDate"]);