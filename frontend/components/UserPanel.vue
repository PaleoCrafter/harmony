<template>
  <div @click.self="expanded = !expanded" :class="['user-panel', { 'user-panel--expanded': expanded }]">
    <UserIcon size="2x" class="user-panel__icon" />
    <div class="user-panel__info">
      <span class="user-panel__name">{{ user.name }}</span>
      <span class="user-panel__discriminator">#{{ user.discriminator }}</span>
    </div>
    <transition-group class="user-panel__icon" name="user-panel__icon">
      <ChevronUpIcon key="expand" v-if="!expanded" class="user-panel__icon--expand" size="1.5x" />
      <XIcon key="close" v-else size="1.5x" class="user-panel__icon--close" />
    </transition-group>
    <transition name="user-panel__dropdown">
      <div v-if="expanded" class="user-panel__dropdown">
        Test
      </div>
    </transition>
  </div>
</template>

<script>
import { ChevronUpIcon, UserIcon, XIcon } from 'vue-feather-icons'

export default {
  name: 'UserPanel',
  components: { UserIcon, ChevronUpIcon, XIcon },
  props: {
    user: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      expanded: false
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
    padding: 1rem;
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
  }
}
</style>
