const inspectorDetails = new Vue({
  el: '#inspector-details',
  data: {
    inspector: null,
    inspectoKeys: null,
    pipelineError: null,
    pipelineErrorKeys: null,
    buildId: 440200939,
  },
  watch: {
    buildId: function (val) {
      this.updateValues()
    }
  },
  methods: {
    updateValues: function(){
      apiGet(`/inspectors/${this.buildId}`, (response) => {
        this.inspector = response;
        this.inspectorKeys = Object.keys(response);
      })
      apiGet(`/pipeline-errors/${this.buildId}`, (response) => {
        if ( response.TestProject ){
          // response.TestProject = response.TestProject.reduce((acc, curr) => acc + '\n' + curr, '')
        }
        this.pipelineError = response;
        this.pipelineErrorKeys = Object.keys(response);
      })
    }
  },
  mounted () {
    this.updateValues()
  }
})

function openInspectorInfoModal(buildId){
  inspectorDetails.buildId = buildId;
  $('#inspectorModal').modal('show');
}

let lastBuild;

const updateInspectors = function(component, page){
  const pageSize = 50;
  const fieldNames = [
    {id:'buildFinishedDate', readable: 'Original date'},
    {id: 'buildReproductionDate', readable: 'Date of the reproduction'},
    {id: 'buildId', readable: 'Build ID'},
    {id: 'repositoryName', readable: 'Github Repository'},
    {id: 'status', readable: 'Status'},
    {id: 'prNumber', readable: 'Pull Request ID'},
    {id: 'travisURL', readable: 'URL of Travis build'},
    {id: 'typeOfFailures', readable: 'Type of failures'},
    {id: 'branchURL', readable: 'URL of the branch'}
  ];
  apiGet('/inspectors/count', function(count) {
    console.log(Math.floor(count / pageSize))
    component.pageCount = Math.floor(count / pageSize);
  })
  apiGet(`/inspectors/?skip=${pageSize * page}`, function(datas) {
    component.gridColumns = fieldNames;

    if (page == 0){
      if (lastBuild){
        if (datas[0].buildId != lastBuild){
          lastBuild = datas[0].buildId;
          notify(`New build found ${datas[0].status}`)
        }
      } else {
        lastBuild = datas[0].buildId;
      }
    }

    component.gridData = datas.map(function (data) {
      let line = {
        status: '',
        data: {},
      };
      fieldNames.forEach(function (column) {
        const fieldName = column.id;

        let dataValue = data[fieldName];

        if (fieldName == 'buildId') {
          dataValue = `<a onClick='openInspectorInfoModal(${dataValue})'>${dataValue}</a>`
        }

        if (fieldName == 'buildFinishedDate') {
          dataValue = moment(dataValue).subtract(1, 'hours').fromNow();
        }

        if (fieldName == 'buildReproductionDate') {
          dataValue = moment(dataValue).subtract(1, 'hours').fromNow();
        }

        if (fieldName == 'status') {
          if (data[fieldName] == 'PATCHED') {
            line.status = 'success';
          } else if (data[fieldName] == 'test failure' || data[fieldName] == 'test errors') {
            line.status = 'warning';
          }
        }

        if (fieldName == 'prNumber') {
          if (dataValue != 0) {
            dataValue = '<a href="https://github.com/'+data['repositoryName']+'/pull/'+data[fieldName]+'"><img src="images/github-logo.svg" style="width: 40px; height: 40px" alt="'+dataValue+'" /></a>';
          } else {
            dataValue = '';
          }
        }

        if (fieldName == 'typeOfFailures' && dataValue != null) {
          dataValue = dataValue.split(',').join(' ');
        }

        if (fieldName == 'travisURL') {
          dataValue = '<a href="'+dataValue+'"><img src="images/travis-ci.png" style="width: 40px; height: 40px" alt="'+dataValue+'" /></a>';
        }

        if (fieldName == 'branchURL') {
          if (dataValue != undefined && dataValue != null) {
            dataValue = '<a href="'+dataValue+'"><img src="images/github-logo.svg" style="width: 40px; height: 40px" alt="Go to branch" /></a>';
          } else {
            dataValue = 'N/A';
          }
        }

        line.data[fieldName] = dataValue;
      });
      return line;
    });
  });
}

const realtime = new Vue({
  el: '#realtimedata',
  data: function() {
    return {
      gridColumns: [],
      gridData: [],
      page: 0,
      pageCount: 0,
    };
  },
  methods: {
    loadData: function (){
      updateInspectors(this, this.page)
    },
    onPageChange (newPage) {
      this.page = newPage;
      this.loadData();
    }
  },
  mounted: function(){
    this.loadData();
    this.interval = setInterval(function () {
      this.loadData();
    }.bind(this), 30000);
  }
});

const chartsVue = new Vue({
  el: '#charts-vue',
  data: function() {
    return {
    };
  },
});
