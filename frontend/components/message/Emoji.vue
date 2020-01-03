<template>
  <span :class="['emoji', { 'emoji--large': large }]">
    <img :src="url" :alt="name" class="emoji__image">

    <span ref="name" v-if="name.startsWith(':')" :class="['emoji__name', `emoji__name--${tooltipAlignment}`]">
      <span>{{ name }}</span>
    </span>
  </span>
</template>

<script>
export default {
  name: 'Emoji',
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
  inject: {
    tooltipBounds: {
      default () {
        return () => undefined
      }
    }
  },
  data () {
    return {
      tooltipAlignment: 'center'
    }
  },
  mounted () {
    this.$refs.name.style.display = 'block'
    this.$nextTick(() => {
      let bounds = this.tooltipBounds()
      if (bounds === undefined) {
        bounds = new DOMRect(0, 0, window.innerWidth, window.innerHeight)
      }
      const left = this.$refs.name.getBoundingClientRect().left
      const right = this.$refs.name.getBoundingClientRect().right
      this.tooltipAlignment = left < bounds.left ? 'left' : right > bounds.right ? 'right' : 'center'
      this.$refs.name.style.display = null
    })
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
  }

  &:hover &__name {
    display: flex;
  }
}
</style>
