$.get('http://localhost:4040/api/scanners/monthData', function (data) {
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
        minute:"%A, %b %e, %H:%M",
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
      pointFormat: '{point.x:%A, %b %e, %H:%M}: {point.y} builds'
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
        name: 'totalScannedBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateBegin),
            d.totalScannedBuilds
          ]
        })
      },
      {
        name: 'totalJavaBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateBegin),
            d.totalJavaBuilds
          ]
        })
      },
      {
        name: 'totalJavaPassingBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateBegin),
            d.totalJavaPassingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuilds',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateBegin),
            d.totalJavaFailingBuilds
          ]
        })
      },
      {
        name: 'totalJavaFailingBuildsWithFailingTests',
        data: data.map( function (d) {
          return [
            Date.parse(d.dateBegin),
            d.totalJavaFailingBuildsWithFailingTests
          ]
        })
      }
    ]
  });
});
