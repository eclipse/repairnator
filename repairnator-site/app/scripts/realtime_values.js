$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/', function (datas) {
  var htmlElement = $('#tablerealtime');

  var headersDisplayed = false;
  datas.forEach(function (data) {
    var row = $('<tr></tr>');
    htmlElement.append(row);

    var fieldName;
    if (!headersDisplayed) {
      for (fieldName in data) {
        var th = $('<th></th>');
        th.text(fieldName);

        row.append(th);
      }

      row = $('<tr></tr>');
      htmlElement.append(row);
      headersDisplayed = true;
    }

    for (fieldName in data) {
      var td = $('<td></td>');
      if (fieldName == 'status') {
        if (data[fieldName] == 'PATCHED') {
          row.addClass('success');
        } else if (data[fieldName] == 'test failure' || data[fieldName] == 'test errors') {
          row.addClass('warning');
        }
      }
      td.text(data[fieldName]);

      row.append(td);
    }
  });
});
