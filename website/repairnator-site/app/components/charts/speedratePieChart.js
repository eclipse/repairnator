const speedratePieChartTemplate = `
<div>
  <div v-bind:id="graphId"></div>
</div>`

Vue.component('speedrate-pie-chart', {
  template: speedratePieChartTemplate,
  data: function () {
    return {
      graphId: "speedratePieChart",
      pie: [],
      graph: null,
    }
  },
  methods: {
    loadData: function (){
      apiGet('/rtscanners/speedrate', function(result) {
        const names = [ 'ERRORED', 'FAILED', 'PASSED', 'CANCELED'];
        const colors = {
          ERRORED: '#ffee00',
          FAILED: '#2ecc71',
          PASSED: '#e74c3c',
          CANCELED: '#f39c12',
        }
        const data = names.map((name) => {
          const y = result.reduce((acc, cur) => acc + cur.status[name], 0);
          const color = colors[name];
          return  { name, y, color };
        });
        this.pie = data;
        this.updateGraph();
      }.bind(this));
    },
    updateGraph: function (){
      if (this.graph){
        const serie = this.graph.series[0];
        serie.setData(this.pie);
      }
    },
    renderGraph: function (){
      this.graph = Highcharts.chart(this.graphId, {
          chart: {
              plotBackgroundColor: null,
              plotBorderWidth: null,
              plotShadow: false,
              type: 'pie'
          },
          title: {
              text: 'Status share of the rt scanner'
          },
          tooltip: {
              pointFormat: '{series.name}: <b>{point.y}</b>'
          },
          plotOptions: {
              pie: {
                  allowPointSelect: true,
                  cursor: 'pointer',
                  dataLabels: {
                      enabled: true,
                      format: '<b>{point.name}</b>: {point.y}',
                      style: {
                          color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                      }
                  }
              }
          },
          series: [{
              name: 'Statuses',
              colorByPoint: true,
              data: []
          }]
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
