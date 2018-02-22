$.get('https://repairnator.lille.inria.fr/repairnator-mongo-api/scanners/', function (data) {
  var htmlElement = $('<div></div>');
  $('#charts').append(htmlElement);

  var endOfDecember = moment('01/01/2018','DD/MM/YYYY');
  data = data.filter(function (value) {
    return (moment(value.dateLimit).isBefore(endOfDecember));
  });
  var currentWeek = moment().isoWeek();
  var currentYear = moment().year();
  var currentWeekYear = currentWeek+1000*currentYear;

  var reducedData = data.reduce(function (accumulator, currentValue) {
    var currentValueWeek = moment(currentValue.dateLimit).isoWeek();
    var currentValueYear = moment(currentValue.dateLimit).year();
    var currentValueWeekYear = currentValueWeek+1000*currentValueYear;

    if (moment(currentValue.dateLimit).months() == 7 || moment(currentValue.dateLimit).months() == 8) {
      if (currentValue.hostname == 'spirals-repairnator') {
        return (accumulator.length == 0) ? [] : accumulator;
      }
    }

    if (accumulator.length == 0) {
      if (currentValueWeekYear === currentWeekYear) {
        return [];
      } else {
        currentValue.dateLimit = moment().year(currentValueYear).isoWeek(currentValueWeek).toISOString();
        return [ currentValue ];
      }
    } else {
      var previousValue = accumulator[accumulator.length - 1];
      var previousValueWeek = moment(previousValue.dateLimit).isoWeek();
      var previousValueYear = moment(previousValue.dateLimit).year();
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
    colors: ['#7cb5ec', '#90ed7d', '#f7a35c'],
    chart: {
      type: 'spline',
      renderTo: htmlElement[0],
      width: 1000,
      height: 768
    },
    title: {
      text: ''
    },
    xAxis: {
      type: 'datetime',
      dateTimeLabelFormats: { // don't display the dummy year
        week:'%e %b %Y',
      },
      title: {
        text: 'Date'
      },
      tickInterval: 30*24*3600*1000
    },
    yAxis: {
      tickInterval: 250,
      title: {
        text: 'Number of builds'
      },
      min: 0,
      max: 15000
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
        name: 'Collected builds to be analyzed',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'Builds identified as Java with CI failure',
        data: reducedData.map( function (d) {
          return [
            moment(d.dateLimit).hour(0).minute(0).second(0).valueOf(),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'Builds with JUnit test failure (called “interesting builds”). ',
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
