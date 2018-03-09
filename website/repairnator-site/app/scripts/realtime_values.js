$.get('https://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/', function (datas) {
  var htmlElement = $('#tablerealtime');

  var fieldNames = [
    {id:'buildFinishedDateStr', readable: 'Original date'},
    {id: 'buildReproductionDateStr', readable: 'Date of the reproduction'},
    {id: 'buildId', readable: 'Build ID'},
    {id: 'repositoryName', readable: 'Github Repository'},
    {id: 'status', readable: 'Status'},
    {id: 'prNumber', readable: 'Pull Request ID'},
    {id: 'travisURL', readable: 'URL of Travis build'},
    {id: 'typeOfFailures', readable: 'Type of failures'},
    {id: 'branchURL', readable: 'URL of the branch'}
  ];

  var headersDisplayed = false;
  datas.forEach(function (data) {
    var row = $('<tr></tr>');
    htmlElement.append(row);

    if (!headersDisplayed) {
      fieldNames.forEach(function (fieldName) {
        var th = $('<th></th>');
        th.text(fieldName.readable);

        row.append(th);
      });

      row = $('<tr></tr>');
      htmlElement.append(row);
      headersDisplayed = true;
    }

    fieldNames.forEach(function (column) {
      var fieldName = column.id;
      var td = $('<td></td>');

      var dataValue = data[fieldName];

      if (fieldName == 'status') {
        if (data[fieldName] == 'PATCHED') {
          row.addClass('success');
        } else if (data[fieldName] == 'test failure' || data[fieldName] == 'test errors') {
          row.addClass('warning');
        }
      }

      if (fieldName == 'prNumber') {
        if (dataValue != 0) {
          dataValue = '<a href="https://github.com/'+data['repositoryName']+'/pull/'+data[fieldName]+'">'+dataValue+'</a>';
        }
      }

      if (fieldName == 'typeOfFailures' && dataValue != null) {
        dataValue = dataValue.split(',').join(' ');
      }

      if (fieldName == 'travisURL') {
        dataValue = '<a href="'+dataValue+'">'+dataValue+'</a>';
      }

      if (fieldName == 'branchURL') {
        if (dataValue != undefined && dataValue != null) {
          dataValue = '<a href="'+dataValue+'">Go to branch</a>';
        } else {
          dataValue = 'N/A';
        }

      }

      td.html(dataValue);

      row.append(td);
    });
  });
});
