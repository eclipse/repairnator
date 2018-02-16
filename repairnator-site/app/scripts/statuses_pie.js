$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats', function (data) {
  var htmlElement = $('<div style="display: inline-block; width: 30%"></div>');

  var total = 0;
  data.forEach(element => {total += element.counted});

  var success = "Successful Bug Reproduction";
  var withoutFailure = "Test without failure";
  var errorTesting = "Error when testing";
  var errorCompiling = "Error when compiling";
  var errorCheckout = "Error when checking out";
  var errorCloning = "Error when cloning";

  var statusMap = {
    "PATCHED": success,
    "test failure": success,
    "test errors": success,
    "NOTBUILDABLE": errorCompiling,
    "SOURCEDIRNOTCOMPUTED": errorCompiling,
    "NOTCLONABLE": errorCloning,
    "NOTFAILING": withoutFailure,
    "NOTTESTABLE": errorTesting,
    "TESTABLE": errorTesting,
    "BUILDNOTCHECKEDOUT": errorCheckout
  };

  var dataNewName = data.map(function (d) {
    return {
      '_id': statusMap[d._id],
      'counted': d.counted
    }
  });

  var map = dataNewName.reduce((acc, item) => { if (acc != undefined && acc.get(item._id) != undefined) { acc.set(item._id, item.counted + acc.get(item._id)); } else {acc.set(item._id, 0); } }, new Map());

  console.log(map);
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
      data: dataNewName.map(function (d) {
        return {
          name: d._id,
          y: d.counted
        }
      })
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
      data: data.map(function (d) {
        return {
          name: d._id,
          y: d.counted
        }
      })
    }]
  });
});

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats/1', function (data) {

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
      data: data.map(function (d) {
        return {
          name: d._id,
          y: d.counted
        }
      })
    }]
  });
});
