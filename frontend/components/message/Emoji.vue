<template>
  <span @mouseover="calculateAlignment()" :class="['emoji', { 'emoji--large': large }]">
    <img :src="url" :alt="name" class="emoji__image">

    <span
      ref="aligned"
      v-if="name.startsWith(':')"
      :class="['emoji__name', `emoji__name--${alignment}`, `emoji__name--${verticalAlignment}`]"
    >
      <span>{{ name }}</span>
    </span>
  </span>
</template>

<script>
import alignment from '@/components/alignment-mixin'

export default {
  name: 'Emoji',
  mixins: [alignment],
  props: {
    url: {
      type: String,
      required: true
    },
    name: {
      type: String,
      required: true
    },
    large: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      defaultAlignment: 'center',
      defaultVerticalAlignment: 'top'
    }
  }
}
</script>

<style lang="scss">
.emoji {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  vertical-align: bottom;

  &--large {
    font-size: 2.5rem;
  }

  &__image {
    object-fit: contain;
    width: 1.375em;
    height: 1.375em;
    vertical-align: bottom;
  }

  &__name {
    display: none;
    position: absolute;
    bottom: 100%;
    margin-bottom: 0.5rem;
    background: #0b0b0d;
    padding: 0.4rem 0.75rem;
    border-radius: 0.25rem;
    box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.25);
    white-space: nowrap;
    align-items: center;
    z-index: 50;
    transform-origin: bottom;

    & > span {
      white-space: nowrap;
      font-size: 0.9rem;
    }

    &:before {
      position: absolute;
      content: '';
      top: 100%;
      left: 50%;
      margin-left: -0.4rem;
      width: 0;
      height: 0;
      border-left: 0.4rem solid transparent;
      border-right: 0.4rem solid transparent;
      border-top: 0.4rem solid #0b0b0d;
    }

    &--left {
      left: 0;

      &:before {
        left: 0.6785em;
      }
    }

    &--right {
      right: 0;

      &:before {
        left: auto;
        right: 0.6785em;
        margin-left: 0;
        margin-right: -0.4rem;
      }
    }

    &--bottom, &--middle {
      bottom: auto;
      top: 100%;
      margin-bottom: 0;
      margin-top: 0.5rem;
      transform-origin: top;

      &:before {
        top: auto;
        bottom: 100%;
        border-top: none;
        border-bottom: 0.4rem solid #0b0b0d;
      }
    }
  }

  &:hover &__name {
    display: flex;
    transform: scale(0);
    animation-name: emoji__name--enter;
    animation-delay: 500ms;
    animation-duration: 0.12s;
    animation-timing-function: ease-in-out;
    animation-fill-mode: forwards;
  }
}

@keyframes emoji__name--enter {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
