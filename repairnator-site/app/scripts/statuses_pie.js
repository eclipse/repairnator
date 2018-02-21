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

var filterPatchedAndTestReproduced = function (dataArray) {
  var successReproduction = 'Bug reproduction without patch';
  var patchCreated = 'Bug reproduction and patch created';

  var statusMap = {
    'PATCHED': patchCreated,
    'test failure': successReproduction,
    'test errors': successReproduction
  };

  var dataNewName = dataArray.reduce(function (acc, elem) {
    var status = statusMap[elem._id];
    if (status !== undefined) {
      if (acc[status] === undefined) {
        acc[status] = elem.counted;
      } else {
        acc[status] += elem.counted;
      }
      return acc;
    } else {
      return acc;
    }
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

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats', function (data) {
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
      type: 'pie',
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

$.get('https://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats', function (data) {
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
      type: 'pie',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Successful Reproduction Builds (all times - '+total+' builds)'
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
      data: filterPatchedAndTestReproduced(data)
    }]
  });
});

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats/14', function (data) {

  var total = 0;
  data.forEach(element => {total += element.counted});

  var htmlElement = $('<div style="display: inline-block; width: 30%"></div>');
  $('#charts').append(htmlElement);
  Highcharts.chart({
    chart: {
      plotBackgroundColor: null,
      plotBorderWidth: null,
      plotShadow: false,
      type: 'pie',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Build statuses (last 2 weeks - '+total+' builds)'
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
          format: '<b>{point.name}</b>: {point.percentage:.1f} %',
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

$.get('https://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats/1', function (data) {

  var total = 0;
  data.forEach(element => {total += element.counted});

  var htmlElement = $('<div style="display: inline-block; width: 30%"></div>');
  $('#charts').append(htmlElement);
  Highcharts.chart({
    chart: {
      plotBackgroundColor: null,
      plotBorderWidth: null,
      plotShadow: false,
      type: 'pie',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Build statuses (last 24 hours - '+total+' builds)'
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
          format: '<b>{point.name}</b>: {point.percentage:.1f} %',
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
