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
    <transition :duration="300" name="modal">
      <div
        v-if="modalOpen"
        @click.self="$store.commit('closeModal')"
        class="modal__container"
      >
        <div :class="['modal', `modal--${modal.type || 'dialog'}`]">
          <div v-if="modal.title" class="modal__header">
            <h4>{{ modal.title }}</h4>
            <XIcon @click="$store.commit('closeModal')" />
          </div>
          <portal-target ref="modalContent" name="modal" class="modal__content" />
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { XIcon } from 'vue-feather-icons'
import { mapGetters, mapState } from 'vuex'
import UserPanel from '@/components/UserPanel.vue'

export default {
  components: { UserPanel, XIcon },
  computed: {
    ...mapState(['sidebarOpen', 'modal']),
    ...mapGetters(['modalOpen'])
  },
  watch: {
    $route () {
      this.$store.commit('closeSidebar')
      this.$store.commit('closeModal')
    }
  },
  mounted () {
    this.$store.commit('populateCategories')
    document.addEventListener('keyup', (event) => {
      if (this.modalOpen && (event.key === 'Escape' || event.keyCode === 27)) {
        this.$store.commit('closeModal')
      }
    })
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

.modal {
  display: flex;
  flex-direction: column;
  cursor: default;

  &--simple {
    max-width: 90%;
    max-height: 90%;
    align-items: center;
  }

  &--dialog {
    max-width: 640px;
    height: 60%;
    max-height: 60%;
    background: #36393f;
    box-shadow: 0 0 0 1px rgba(32, 34, 37, .6), 0 2px 10px 0 rgba(0, 0, 0, .2);
    border-radius: 0.5rem;
    padding: 1rem;
    align-items: stretch;
    justify-content: stretch;
    overflow: hidden;
    flex-grow: 1;

    @media (max-width: 1200px) {
      max-width: 90%;
    }

    @media (max-width: 768px) {
      width: 100%;
      max-width: 100%;
      height: 60%;
      align-self: flex-end;
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }
  }

  &__container {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.7);
    cursor: pointer;
    z-index: 51;
  }

  &__header {
    display: flex;
    align-items: center;
    padding-bottom: 0.5rem;

    h4 {
      font-size: 1.5rem;
      font-weight: bold;
      line-height: 1;
      color: rgba(255, 255, 255, 0.8);
    }

    .feather {
      margin-left: auto;
      cursor: pointer;
    }
  }

  &__content {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    flex: 1;
    overflow: hidden;
  }

  &-enter-active {
    transition: opacity .3s ease-in-out;
  }

  &-leave-active {
    transition: opacity .2s ease-in-out;
  }

  &-enter-active .modal {
    transition: transform .3s cubic-bezier(0.420, 0.000, 0.630, 1.2);

    &--dialog {
      @media (max-width: 768px) {
        transition: transform .3s ease-out;
      }
    }
  }

  &-leave-active .modal {
    transition: transform .2s ease-in-out;
  }

  &-enter, &-leave-to {
    opacity: 0;

    .modal {
      transform: scale(0);

      &--dialog {
        @media (max-width: 768px) {
          transform: translateY(100%);
        }
      }
    }
  }

  ::-webkit-scrollbar {
    width: 0.9rem;
    height: 0.9rem;
  }

  ::-webkit-scrollbar-corner {
    background-color: transparent;
  }

  ::-webkit-scrollbar-track {
    border-width: initial;
    background-color: transparent;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track, ::-webkit-scrollbar-thumb {
    background-clip: padding-box;
    border-width: 3px;
    border-style: solid;
    border-radius: 7px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #202225;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track-piece {
    background-color: #2f3136;
    border: 3px solid #36393f;
    border-radius: 7px;
  }
}
</style>
