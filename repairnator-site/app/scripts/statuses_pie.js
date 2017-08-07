$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats', function (data) {
  var htmlElement = $('<div style="float: left; width: 49%;"></div>');

  var total = 0;
  data.forEach(element => {total += element.counted});

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
      data: data.map(function (d) {
        return {
          name: d._id,
          y: d.counted
        }
      })
    }]
  });
});

$.get('http://repairnator.lille.inria.fr/repairnator-mongo-api/inspectors/statusStats/2', function (data) {

  var total = 0;
  data.forEach(element => {total += element.counted});

  var htmlElement = $('<div style="float: right; width: 49%;"></div>');
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
