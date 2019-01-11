const speedrateChartTemplate = `
<div>
  <div v-bind:id="graphId"></div>
</div>`

Vue.component('speedrate-chart', {
  template: speedrateChartTemplate,
  data: function () {
    return {
      graphId: "speedrateChart",
      series: {
        total: [],
        failed: [],
        inspectors: [],
      },
      graph: null,
    }
  },
  methods: {
    loadData: function (){
      apiGet('/inspectors/speedrate', function(data) {
        this.series.inspectors = data.map((value) => [moment(value._id).valueOf() ,value.counted]);
        this.startDate = moment.min(data.map((value) => moment(value["_id"])));
        this.updateGraph();
      }.bind(this));
      apiGet('/rtscanners/speedrate', function(data) {
        this.series.failed = data.map((value) => [moment(value._id).valueOf() ,value.status.FAILED]);
        this.series.total = data.map((value) => [moment(value._id).valueOf() ,Object.values(value.status).reduce((acc, cur) => acc + cur)]);
        this.updateGraph();
      }.bind(this));
    },
    updateGraph: function (){
      if (this.graph){
        const serie = this.graph.series[0];
        serie.setData(this.series.total);
        const serie1 = this.graph.series[1];
        serie1.setData(this.series.failed);
      }
    },
    renderGraph: function (){
      this.graph = Highcharts.chart(this.graphId, {
        title: {
          text: 'Speedrate during the past 24 hours.'
        },
        yAxis: {
          title: {
            text: 'Number of reproduction'
          }
        },
        legend: {
          layout: 'vertical',
          align: 'right',
          verticalAlign: 'middle'
        },
        plotOptions: {
          series: {
            label: {
              connectorAllowed: false
            },
          }
        },
        series: [
          {
            name: 'Total',
            data: [],
            type: 'spline',
          },
          {
            name: 'Failed',
            data: [],
            type: 'spline',
          }
        ],
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
        responsive: {
          rules: [{
            condition: {
              maxWidth: 500
            },
            chartOptions: {
              legend: {
                layout: 'horizontal',
                align: 'center',
                verticalAlign: 'bottom'
              }
            }
          }]
        }
      });
    },
  },
  mounted: function(){
    this.loadData();
    this.renderGraph();

    this.interval = setInterval(function () {
      this.loadData();
    }.bind(this), 60000);
  },
})
