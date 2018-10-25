const paginationTemplate = `
<nav aria-label="Page navigation" style="padding: 0px 30px">
  <ul class="pagination">
    <li class="page-item" v-bind:class="{disabled: !page}">
      <a class="page-link" @click="prevPage" aria-label="Previous">
        <span aria-hidden="true">&laquo;</span>
        <span class="sr-only">Previous</span>
      </a>
    </li>
    <li v-if="page" class="page-item"><a class="page-link" @click="setPage(0)"> 0 </a></li>
    <li v-if="page==length" class="page-item"><a class="page-link" @click="setPage(page-1)"> {{page-1}}</a></li>
    <li class="page-item active"><a class="page-link" @click="setPage(page)"> {{page}}</a></li>
    <li v-if="page < length && !page" class="page-item"><a class="page-link" @click="setPage(page+1)"> {{page+1}}</a></li>
    <li v-if="page<length" class="page-item"><a class="page-link" @click="setPage(length)"> {{length}} </a></li>

    <li class="page-item" v-bind:class="{disabled: page>=length}">
      <a class="page-link" @click="nextPage" aria-label="Next">
        <span aria-hidden="true">&raquo;</span>
        <span class="sr-only">Next</span>
      </a>
    </li>
  </ul>
</nav>`

Vue.component('pagination', {
  template: paginationTemplate,
  props: {
    length: Number,
  },
  data: function () {
    return {
      page: 0,
    }
  },
  methods:{
    prevPage(){
      if ( this.page ){
        this.page--;
        this.$emit('update', this.page);
      }
    },
    nextPage(){
      if ( this.page < this.length ){
        this.page++;
        this.$emit('update', this.page);
      }
    },
    setPage(value){
      this.page = value;
      this.$emit('update', this.page);
    }
  }
})