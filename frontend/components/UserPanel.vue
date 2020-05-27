<template>
  <div :class="['user-panel', { 'user-panel--expanded': expanded }]" @click.self="expanded = !expanded">
    <UserIcon size="2x" class="user-panel__icon" />
    <div v-if="identity !== null" class="user-panel__info">
      <span class="user-panel__name">{{ identity.name }}</span>
      <span class="user-panel__discriminator">#{{ identity.discriminator }}</span>
    </div>
    <transition-group class="user-panel__icon" name="user-panel__icon">
      <ChevronUpIcon v-if="!expanded" key="expand" class="user-panel__icon--expand" size="1.5x" />
      <XIcon v-else key="close" size="1.5x" class="user-panel__icon--close" />
    </transition-group>
    <transition name="user-panel__dropdown">
      <div v-if="expanded" class="user-panel__dropdown">
        <nuxt-link to="/privacy-policy" class="user-panel__dropdown-link">
          Privacy Policy
          <ShieldIcon size="1.6x" class="user-panel__dropdown-link-icon" />
        </nuxt-link>
        <a href="#" class="user-panel__logout" @click="logout">
          Logout
          <LogOutIcon size="1.6x" class="user-panel__logout-icon" />
        </a>
      </div>
    </transition>
  </div>
</template>

<script>
import { ChevronUpIcon, LogOutIcon, ShieldIcon, UserIcon, XIcon } from 'vue-feather-icons'
import { mapState } from 'vuex'

export default {
  name: 'UserPanel',
  components: { UserIcon, ChevronUpIcon, XIcon, LogOutIcon, ShieldIcon },
  data () {
    return {
      expanded: false
    }
  },
  computed: mapState(['identity']),
  watch: {
    $route () {
      this.expanded = false
    }
  },
  mounted () {
    document.addEventListener('click', this.handleOutsideClick)
  },
  destroyed () {
    document.removeEventListener('click', this.handleOutsideClick)
  },
  methods: {
    logout () {
      this.$apolloHelpers.onLogout()
      window.location.href = '/api/auth/logout'
    },
    handleOutsideClick (event) {
      if (!this.$el.contains(event.target)) {
        this.expanded = false
      }
    }
  }
}
</script>

<style lang="scss">
.user-panel {
  display: flex;
  position: relative;
  margin-top: auto;
  background: #2b292f;
  padding: 1rem 1rem;
  align-items: center;

  &:hover, &--expanded {
    background: #2e3136;
    cursor: pointer;
  }

  &__info {
    display: flex;
    flex-direction: column;
    margin-left: 0.25rem;
    line-height: 1.1;
    font-size: 0.9rem;
    margin-right: auto;
    pointer-events: none;
  }

  &__icon {
    pointer-events: none;
    position: relative;

    &--expand, &--close {
      transform-origin: center center;
    }

    &-enter-active, &-leave-active {
      transition: transform .2s ease-in-out, opacity .2s ease-in-out;
    }

    &--close.user-panel__icon-enter-active {
      transition: transform .15s ease-in-out, opacity .15s ease-in-out;
      transition-delay: 0.05s, 0.05s;
    }

    &--expand.user-panel__icon-leave-to {
      transform: rotate(-90deg) translateY(2px);
    }

    &--close.user-panel__icon-leave-to {
      transform: rotate(90deg);
      opacity: 0;
    }

    &-enter {
      opacity: 0;
      transform: scale(0);
    }

    &-leave-active {
      position: absolute;
    }
  }

  &__name {
    font-weight: 600;
  }

  &__dropdown {
    position: absolute;
    bottom: 100%;
    margin-bottom: 1rem;
    background: #0b0b0d;
    left: 0.75rem;
    right: 0.75rem;
    padding: 0.5rem 0.5rem;
    transform-origin: center bottom;
    border-radius: 0.25rem;
    box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.25);
    cursor: default;

    &-enter-active {
      transition: transform .12s ease-in-out;
    }

    &-enter, &-leave-to {
      transform: scale(0);
    }

    &-link {
      display: flex;
      align-items: center;
      padding: 0.5rem 0.5rem;
      margin: 0.125rem 0;
      border-radius: 2px;
      color: rgba(213, 236, 255, 0.8);
      text-decoration: none;
      font-size: 0.9rem;

      &:hover, &:active, &:focus {
        background: rgba(79, 84, 92, 0.16);
      }

      &-icon {
        margin-left: auto;
      }
    }
  }

  &__logout {
    display: flex;
    align-items: center;
    padding: 0.5rem 0.5rem;
    margin: 0.125rem 0;
    border-radius: 2px;
    color: #f04747;
    text-decoration: none;
    font-size: 0.9rem;

    &:hover, &:active, &:focus {
      background: rgba(79, 84, 92, 0.16);
    }

    &-icon {
      margin-left: auto;
    }
  }
}
</style>
