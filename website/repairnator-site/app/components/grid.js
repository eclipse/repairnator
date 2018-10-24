const gridTemplate = `
<div class="table-responsive">
  <table class="table table-striped">
    <thead>
      <tr>
        <th v-for="key in columns">
          {{ key.readable }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="entry in data" v-bind:class="entry.status">
        <td v-for="key in columns">
          <span v-html="entry.data[key.id]"> </span>
        </td>
      </tr>
    </tbody>
  </table>
</div>`

Vue.component('grid', {
  template: gridTemplate,
  props: {
    data: Array,
    columns: Array,
    filterKey: String,
  }
})
