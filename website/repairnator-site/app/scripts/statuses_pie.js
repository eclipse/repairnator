var simplifyStatuses = function (dataArray) {
  var success = 'Successful Bug Reproduction';
  var withoutFailure = 'Test without failure';
  var errorTesting = 'Error when testing';
  var errorCompiling = 'Error when compiling';
  var errorCheckout = 'Error when checking out';
  var errorCloning = 'Error when cloning';

  var statusMap = {
    'PATCHED': success,
    'test failure': success,
    'test errors': success,
    'NOTBUILDABLE': errorCompiling,
    'SOURCEDIRNOTCOMPUTED': errorCompiling,
    'NOTCLONABLE': errorCloning,
    'NOTFAILING': withoutFailure,
    'NOTTESTABLE': errorTesting,
    'TESTDIRNOTCOMPUTED': errorTesting,
    'TESTABLE': errorTesting,
    'BUILDNOTCHECKEDOUT': errorCheckout
  };

  var dataNewName = dataArray.reduce(function (acc, elem) {
    var status = statusMap[elem._id];
    if (acc[status] === undefined) {
      acc[status] = elem.counted;
    } else {
      acc[status] += elem.counted;
    }
    return acc;
  }, {});

  var finalArray = Object.keys(dataNewName).reduce(function (acc, id) {
    acc.push({
      'name': id,
      'y': dataNewName[id]
    });
    return acc;
  }, []);

  return finalArray;
};

apiGet( '/inspectors/statusStats', function (data) {
  console.log('fsdf')
  var htmlElement = $('<div style="display: inline-block; width: 30%"></div>');

  var total = 0;
  data.forEach(element => {total += element.counted});


  // return acc.set(item._id, item.counted + acc.get(item._id));

  $('#charts').append(htmlElement);
  Highcharts.chart({
    chart: {
      plotBackgroundColor: null,
      plotBorderWidth: null,
      plotShadow: false,
      type: 'bar',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Build statuses (all times - '+total+' builds)'
    },
    tooltip: {
      pointFormat: '{series.name}: <b>{point.percentage:.1f}% ({point.y})</b>'
    },
    plotOptions: {
      pie: {
        allowPointSelect: true,
        cursor: 'pointer',
        dataLabels: {
          enabled: true,
          format: '<b>{point.name}</b>: {point.percentage:.1f}% ({point.y})',
          style: {
            color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
          }
        }
      }
    },
    series: [{
      name: 'Statuses',
      colorByPoint: true,
      data: simplifyStatuses(data)
    }]
  });
});
