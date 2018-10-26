const speedrateChartTemplate = `
<div>
  <div v-bind:id="graphId"></div>
</div>`

Vue.component('speedrate-chart', {
  template: speedrateChartTemplate,
  data: function () {
    return {
      graphId: "nameOfTheChart",
      dataToPlot: [],
      graph: null,
    }
  },
  methods: {
    loadData: function (){
      apiGet('/inspectors/speedrate', function(data) {
        this.dataToPlot = data.map((value) => [moment(value._id).valueOf() ,value.counted]);
        this.startDate = moment.min(data.map((value) => moment(value["_id"])));
        this.updateGraph();
      }.bind(this));
    },
    updateGraph: function (){
      if (this.graph){
        const serie = this.graph.series[0];
        serie.setData(this.dataToPlot);
      }
    },
    renderGraph: function (){
      console.log("render");
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
        series: [{
          name: 'Speedrate',
          data: [],
          type: 'spline',
        }],
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
