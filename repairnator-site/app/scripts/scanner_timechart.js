$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/scanners/weeksData/2', function (data) {
  console.log(data);
  var htmlElement = $('<div></div>');
  $('#charts').append(htmlElement);

  /*
   {
   '_id': '58e22490cefc9665e4354cde',
   'hostname': 'repairnator',
   'dateBegin': '2017-04-03T12:00:00.000Z',
   'dateLimit': '2017-04-03T08:00:00.000Z',
   'dayLimit': '03/04/2017',
   'totalRepoNumber': 1611,
   'totalRepoUsingTravis': 1601,
   'totalScannedBuilds': 205,
   'totalJavaBuilds': 198,
   'totalJavaPassingBuilds': 152,
   'totalJavaFailingBuilds': 27,
   'totalJavaFailingBuildsWithFailingTests': 8,
   'totalPRBuilds': 52,
   'duration': '0:31:4',
   'runId': '92d87100-e392-4b7c-8f49-2789af76ef7c',
   'dateBeginStr': '03/04/17 12:00',
   'dateLimitStr': '03/04/17 08:00'
   },
   */

  var reducedData = data.reduce(function (accumulator, currentValue) {
    if (accumulator.length == 0) {
      return [ currentValue ];
    } else {
      var previousValue = accumulator[accumulator.length - 1];

      if (previousValue.dayLimit === currentValue.dayLimit) {
        for (var attr in previousValue) {
          if (attr.indexOf('total') != -1 && attr.indexOf('totalRepoNumber') == -1) {
            previousValue[attr] += currentValue[attr];
          }
        }
      } else {
        accumulator.push(currentValue);
      }

      return accumulator;
    }
  }, []);

  Highcharts.chart({
    chart: {
      type: 'spline',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Number of scanned builds'
    },
    xAxis: {
      type: 'datetime',
      dateTimeLabelFormats: { // don't display the dummy year
        day:'%A, %b %e',
      },
      title: {
        text: 'Date'
      }
    },
    yAxis: {
      title: {
        text: 'Number of builds'
      },
      min: 0
    },
    tooltip: {
      headerFormat: '<b>{series.name}</b><br>',
      pointFormat: '{point.x:%A, %b %e}: {point.y} builds'
    },

    plotOptions: {
      spline: {
        marker: {
          enabled: true
        }
      }
    },

    series: [
      {
        name: 'totalRepoNumber',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalRepoNumber
          ]
        })
      },
      {
        name: 'totalScannedBuilds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'totalJavaBuilds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaBuilds
          ]
        })
      },
      {
        name: 'totalJavaPassingBuilds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaPassingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuilds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuildsWithFailingTests',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaFailingBuildsWithFailingTests
          ]
        })
      }
    ]
  });
});

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/scanners/', function (data) {
  var htmlElement = $('<div></div>');
  $('#charts').append(htmlElement);

  /*
   {
   '_id': '58e22490cefc9665e4354cde',
   'hostname': 'repairnator',
   'dateBegin': '2017-04-03T12:00:00.000Z',
   'dateLimit': '2017-04-03T08:00:00.000Z',
   'dayLimit': '03/04/2017',
   'totalRepoNumber': 1611,
   'totalRepoUsingTravis': 1601,
   'totalScannedBuilds': 205,
   'totalJavaBuilds': 198,
   'totalJavaPassingBuilds': 152,
   'totalJavaFailingBuilds': 27,
   'totalJavaFailingBuildsWithFailingTests': 8,
   'totalPRBuilds': 52,
   'duration': '0:31:4',
   'runId': '92d87100-e392-4b7c-8f49-2789af76ef7c',
   'dateBeginStr': '03/04/17 12:00',
   'dateLimitStr': '03/04/17 08:00'
   },
   */

  Highcharts.chart({
    chart: {
      type: 'spline',
      renderTo: htmlElement[0]
    },
    title: {
      text: 'Number of scanned builds'
    },
    xAxis: {
      type: 'datetime',
      dateTimeLabelFormats: { // don't display the dummy year
        day:'%A, %b %e',
      },
      title: {
        text: 'Date'
      }
    },
    yAxis: {
      title: {
        text: 'Number of builds'
      },
      min: 0
    },
    tooltip: {
      headerFormat: '<b>{series.name}</b><br>',
      pointFormat: '{point.x:%A, %b %e}: {point.y} builds'
    },

    plotOptions: {
      spline: {
        marker: {
          enabled: true
        }
      }
    },

    series: [
      {
        name: 'totalRepoNumber',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalRepoNumber
          ]
        })
      },
      {
        name: 'totalScannedBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'totalJavaBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaBuilds
          ]
        })
      },
      {
        name: 'totalJavaPassingBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaPassingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuildsWithFailingTests',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaFailingBuildsWithFailingTests
          ]
        })
      }
    ]
  });
});
