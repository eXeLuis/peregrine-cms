<template>
  <div class="container" v-bind:data-per-path="model.path">
    <div class="row">
      <div class="col-sm-8 blog-main">
        <pagerendervue-components-placeholder v-bind:model="{ path: '/jcr:content/main/article', component: model.component, location: 'before' }"></pagerendervue-components-placeholder>
        <template v-for="child in namedChildren('article')">
          <component v-bind:is="child.component" v-bind:model="child"></component>
        </template>
        <pagerendervue-components-placeholder v-bind:model="{ path: '/jcr:content/main/article', component: model.component, location: 'after' }"></pagerendervue-components-placeholder>
      </div>
      <!---postblog-main ---->
      <div class="col-sm-3 offset-sm-1 blog-sidebar">
        <pagerendervue-components-placeholder v-bind:model="{ path: '/jcr:content/main/sidebar', component: 'sidebar', location: 'before' }"></pagerendervue-components-placeholder>
        <template v-for="child in namedChildren('sidebar')">
          <component v-bind:is="child.component" v-bind:model="child"></component>
        </template>
        <pagerendervue-components-placeholder v-bind:model="{ path: '/jcr:content/main/sidebar', component: 'sidebar', location: 'after' }"></pagerendervue-components-placeholder>
      </div>
    </div>
    <!-- /.row -->
  </div>
</template>

<script>
    export default {
        props: ['model'],
        methods: {
            namedChildren(name) {
                for(let i = 0; i < this.model.children.length; i++) {
                    if(this.model.children[i].path.startsWith(this.model.path+'/'+name)) return this.model.children[i].children
                }
                return []
            }
        }
    }
</script>

