<template>
  <div class="site">
    <transition name="site__sidebar">
      <div v-if="sidebarOpen" @click.self="$store.commit('closeSidebar')" class="site__sidebar">
        <div class="site__sidebar-container">
          <portal-target name="sidebar" multiple class="site__sidebar-entries" />
          <UserPanel />
        </div>
      </div>
    </transition>
    <nuxt />
  </div>
</template>

<script>
import { mapState } from 'vuex'
import UserPanel from '@/components/UserPanel.vue'

export default {
  components: { UserPanel },
  computed: mapState(['sidebarOpen']),
  watch: {
    $route () {
      this.$store.commit('closeSidebar')
    }
  },
  mounted () {
    this.$store.commit('populateCategories')
  }
}
</script>

<style lang="scss">
html {
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  -ms-text-size-adjust: 100%;
  -webkit-text-size-adjust: 100%;
  -moz-osx-font-smoothing: grayscale;
  -webkit-font-smoothing: antialiased;
  box-sizing: border-box;
}

*,
*:before,
*:after {
  box-sizing: border-box;
  margin: 0;
}

body {
  width: 100%;
  height: 100vh;
  background: #36393F;
  color: white;
  overflow: hidden;
}

.site {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: stretch;
  justify-items: stretch;

  &__sidebar {
    position: fixed;
    display: flex;
    align-items: stretch;
    left: 0;
    top: 0;
    bottom: 0;
    right: 0;
    z-index: 100;
    background: rgba(0, 0, 0, 0.5);

    @media (min-width: 1200px) {
      display: none;
    }

    &-container {
      display: flex;
      flex-direction: column;
      position: relative;
      z-index: 101;
    }

    &-entries {
      display: flex;
      flex-direction: column;
      flex: 1;
    }

    &-enter-active, &-leave-active {
      transition: background .3s ease-in-out;

      .site__sidebar-container {
        transition: transform .3s ease-in-out;
      }
    }

    &-enter, &-leave-to {
      background: rgba(0, 0, 0, 0);

      .site__sidebar-container {
        transform: translateX(-100%);
      }
    }
  }
}
</style>
