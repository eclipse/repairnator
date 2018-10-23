$.get(getApiUri('/scanners/'), function (dataScanner) {
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

  var currentYear = moment().year();
  var currentWeek = moment().isoWeek();
  var currentWeekYear = currentWeek+1000*currentYear;

  var reducedDataScanner = dataScanner.reduce(function (accumulator, currentValue) {
    var currentValueYear = moment(currentValue.dateLimit).year();
    var currentValueWeek = moment(currentValue.dateLimit).isoWeek();

    var currentValueWeekYear = currentValueWeek+1000*currentValueYear;

    if (accumulator.length == 0) {
      if (!currentValue.dateLimit || currentValueWeekYear === currentWeekYear) {
        return [];
      } else {
        currentValue.dateLimit = moment().year(currentValueYear).isoWeek(currentValueWeek).toISOString();
        return [ currentValue ];
      }
    } else {
      var previousValue = accumulator[accumulator.length - 1];
      if (!currentValue.dateLimit) {
        return accumulator;
      }
      var previousValueYear = moment(previousValue.dateLimit).year();
      var previousValueWeek = moment(previousValue.dateLimit).isoWeek();
      var previousValueWeekYear = previousValueWeek+1000*previousValueYear;

      if (currentValueWeekYear === currentWeekYear) {
        return accumulator;
      }
      if (previousValueWeekYear === currentValueWeekYear) {
        for (var attr in previousValue) {
          if (attr.indexOf('total') != -1 && attr.indexOf('totalRepoNumber') == -1) {
            previousValue[attr] += currentValue[attr];
          }
        }
      } else {
        currentValue.dateLimit = moment().year(currentValueYear).isoWeek(currentValueWeek).toISOString();
        accumulator.push(currentValue);
      }

      return accumulator;
    }
  }, []);

  apiGet('/inspectors/reproducedBuilds/', function (dataInspector) {

    var reducedDataInspector = dataInspector.reduce(function (accumulator, currentValue) {
      var currentValueWeek = moment(currentValue._id, 'YYYY-MM-DD').isoWeek();
      var currentValueYear = moment(currentValue._id, 'YYYY-MM-DD').year();
      var currentValueWeekYear = currentValueWeek+1000*currentValueYear;

      if (accumulator.length == 0) {
        if (currentValueWeekYear === currentWeekYear) {
          return [];
        } else {
          currentValue._id = moment().year(currentValueYear).isoWeek(currentValueWeek).format('YYYY-MM-DD');
          return [ currentValue ];
        }
      } else {
        var previousValue = accumulator[accumulator.length - 1];
        if (currentValueWeekYear === currentWeekYear) {
          return accumulator;
        }
        var previousValueWeek = moment(previousValue._id, 'YYYY-MM-DD').isoWeek();
        var previousValueYear = moment(previousValue._id, 'YYYY-MM-DD').year();
        var previousValueWeekYear = previousValueWeek+1000*previousValueYear;

        if (previousValueWeekYear === currentValueWeekYear) {
          previousValue.counted += currentValue.counted;
        } else {
          currentValue._id = moment().year(currentValueYear).isoWeek(currentValueWeek).format('YYYY-MM-DD');
          accumulator.push(currentValue);
        }

        return accumulator;
      }
    }, []);
    Highcharts.chart({
      colors: ['black'],
      chart: {
        type: 'spline',
        renderTo: htmlElement[0]
      },
      title: {
        text: 'Overall number of scanned and reproduced builds'
      },
      xAxis: {
        type: 'datetime',
        dateTimeLabelFormats: {
          day: '%e. %b',
          week: '%e. %b',
          month: '%b \'%y',
          year: '%Y'
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
