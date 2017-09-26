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

  var currentWeek = moment().week();

  var reducedData = data.reduce(function (accumulator, currentValue) {
    var currentValueWeek = moment(currentValue.dateLimit).week();

    if (accumulator.length == 0) {
      if (currentValueWeek === currentWeek) {
        return [];
      } else {
        return [ currentValue ];
      }
    } else {
      var previousValue = accumulator[accumulator.length - 1];
      var previousValueWeek = moment(previousValue.dateLimit).week();

      if (currentValueWeek === currentWeek) {
        return accumulator;
      }
      if (previousValueWeek === currentValueWeek) {
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

  (function (H) {
    // Pass error messages
    H.Axis.prototype.allowNegativeLog = true;

    // Override conversions
    H.Axis.prototype.log2lin = function (num) {
      var isNegative = num < 0,
        adjustedNum = Math.abs(num),
        result;
      if (adjustedNum < 10) {
        adjustedNum += (10 - adjustedNum) / 10;
      }
      result = Math.log(adjustedNum) / Math.LN10;
      return isNegative ? -result : result;
    };
    H.Axis.prototype.lin2log = function (num) {
      var isNegative = num < 0,
        absNum = Math.abs(num),
        result = Math.pow(10, absNum);
      if (result < 10) {
        result = (10 * (result - 1)) / (10 - 1);
      }
      return isNegative ? -result : result;
    };
  }(Highcharts));

  Highcharts.chart({
    chart: {
      type: 'spline',
      renderTo: htmlElement[0],
      width: undefined,
      height: 700
    },
    exporting: {
      enabled: true,
      sourceWidth: 1200,
      sourceHeight: 600
    },
    title: {
      text: 'Number of scanned projects and builds per week'
    },
    xAxis: {
      type: 'datetime',
      dateTimeLabelFormats: { // don't display the dummy year
        week:'%e %b %Y',
      },
      title: {
        text: 'Date'
      }
    },
    yAxis: {
      type: 'logarithmic',
      minorTickInterval: 1,
      title: {
        text: 'Number of builds or projects'
      },
      min: 20
    },
    tooltip: {
      headerFormat: '<b>{series.name}</b><br>',
      pointFormat: '{point.x:%A, %b %e}: {point.y} builds'
    },

    plotOptions: {
      spline: {
        marker: {
          enabled: true
        },
        dataLabels: {
          enabled: false
        },
      },
    },

    series: [
      {
        name: 'Scanned repositories',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalRepoNumber
          ]
        })
      },
      {
        name: 'Scanned builds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'Scanned Java builds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaBuilds
          ]
        })
      },
      {
        name: 'Scanned Java failing builds',
        data: reducedData.map( function (d) {
          return [
            Date.parse(d.dateLimit),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'Scanned Java failing builds with failing tests',
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
