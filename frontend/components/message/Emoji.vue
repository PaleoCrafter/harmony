<template>
  <span @mouseover="startAlignment" @mouseleave="stopAlignment" :class="['emoji', { 'emoji--large': large }]">
    <span class="emoji__content">
      <img :src="url" :alt="name" class="emoji__image">
      <slot />
    </span>

    <span
      ref="aligned"
      v-if="name.startsWith(':')"
      class="emoji__name"
    >
      <span>{{ name }}</span>
      <span class="emoji__name-arrow" data-popper-arrow />
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
      default: () => false
    }
  },
  data () {
    return {
      alignment: 'top',
      overflowAlignments: ['top-start', 'top-end', 'bottom', 'bottom-start', 'bottom-end', 'left', 'right']
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

  &__content {
    display: inline-flex;
    align-items: center;
    vertical-align: bottom;
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

    &-arrow {
      position: absolute;
      display: block;
      top: 100%;
      width: 0;
      height: 0;
      border-left: 0.4rem solid transparent;
      border-right: 0.4rem solid transparent;
      border-top: 0.4rem solid #0b0b0d;
    }

    &[data-popper-placement='bottom'] {
      .emoji__name-arrow {
        top: -0.4rem;
        border-top: transparent;
        border-bottom: 0.4rem solid #0b0b0d;
      }
    }
  }

  &:hover &__name {
    display: flex;
    opacity: 0;
    animation-name: emoji__name--enter;
    animation-delay: 500ms;
    animation-duration: 0.12s;
    animation-timing-function: ease-in-out;
    animation-fill-mode: forwards;
  }
}

@keyframes emoji__name--enter {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}
</style>
