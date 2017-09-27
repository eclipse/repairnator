$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/scanners/weeksData/2', function (dataScanner) {
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

  var reducedData = dataScanner.reduce(function (accumulator, currentValue) {
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

  $.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/reproducedBuilds/14', function (dataInspector) {
    Highcharts.chart({
      chart: {
        type: 'spline',
        renderTo: htmlElement[0]
      },
      title: {
        text: 'Number of scanned and reproduced builds on the last 2 weeks'
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
          },
          dataLabels: {
            enabled: true
          },
        },
      },

      series: [
        {
          name: 'Builds to reproduce',
          data: reducedData.map( function (d) {
            return [
              moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
              d.totalJavaFailingBuildsWithFailingTests
            ]
          })
        },
        {
          name: 'Builds succeed to reproduce',
          data: dataInspector.map( function (d) {
            return [
              moment(d._id, 'YYYY-MM-DD').valueOf(),
              d.counted
            ]
          })
        }
      ]
    });
  });
});

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/scanners/', function (dataScanner) {
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

  var currentWeek = moment().week();

  var reducedDataScanner = dataScanner.reduce(function (accumulator, currentValue) {
    var currentValueWeek = moment(currentValue.dateLimit).week();

    if (accumulator.length == 0) {
      if (!currentValue.dateLimit || currentValueWeek === currentWeek) {
        return [];
      } else {
        currentValue.dateLimit = moment().year(2017).week(currentValueWeek).toISOString();
        return [ currentValue ];
      }
    } else {
      var previousValue = accumulator[accumulator.length - 1];
      if (!currentValue.dateLimit) {
        return accumulator;
      }
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
        currentValue.dateLimit = moment().year(2017).week(currentValueWeek).toISOString();
        accumulator.push(currentValue);
      }

      return accumulator;
    }
  }, []);

  $.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/reproducedBuilds/', function (dataInspector) {

    var reducedDataInspector = dataInspector.reduce(function (accumulator, currentValue) {
      var currentValueWeek = moment(currentValue._id, 'YYYY-MM-DD').week();

      if (accumulator.length == 0) {
        if (currentValueWeek === currentWeek) {
          return [];
        } else {
          currentValue._id = moment().year(2017).week(currentValueWeek).format('YYYY-MM-DD');
          return [ currentValue ];
        }
      } else {
        var previousValue = accumulator[accumulator.length - 1];
        if (currentValueWeek === currentWeek) {
          return accumulator;
        }
        var previousValueWeek = moment(previousValue._id, 'YYYY-MM-DD').week();
        if (previousValueWeek === currentValueWeek) {
          previousValue.counted += currentValue.counted;
        } else {
          currentValue._id = moment().year(2017).week(currentValueWeek).format('YYYY-MM-DD');
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
        text: 'Overall number of scanned and reproduced builds'
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
          },
          dataLabels: {
            enabled: true
          },
        },
      },

      series: [
        {
          name: 'Builds to reproduce',
          data: reducedDataScanner.map( function (d) {
            return [
              moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
              d.totalJavaFailingBuildsWithFailingTests
            ]
          })
        },
        {
          name: 'Builds succeed to reproduce',
          data: reducedDataInspector.map( function (d) {
            return [
              moment(d._id, 'YYYY-MM-DD').valueOf(),
              d.counted
            ]
          })
        }
      ]
    });
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
        currentValue.dateLimit = moment().year(2017).week(currentValueWeek).toISOString();
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
        currentValue.dateLimit = moment().year(2017).week(currentValueWeek).toISOString();
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
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalRepoNumber
          ]
        })
      },
      {
        name: 'Scanned builds',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'Scanned Java builds',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalJavaBuilds
          ]
        })
      },
      {
        name: 'Scanned Java failing builds',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'Scanned Java failing builds with failing tests',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalJavaFailingBuildsWithFailingTests
          ]
        })
      }
    ]
  });
});
