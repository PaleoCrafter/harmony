<template>
  <span :style="userStyle" class="user-name">
    {{ userName }}
  </span>
</template>

<script>
export default {
  name: 'UserName',
  props: {
    user: {
      type: Object,
      required: true
    }
  },
  computed: {
    userName () {
      return this.user?.nickname ?? this.user.name
    },
    userStyle () {
      const defaultColor = [255, 255, 255]
      let [r, g, b] = defaultColor

      if (this.user.color !== undefined && this.user.color !== null) {
        ({ r, g, b } = this.user.color)

        if (r === 0 && g === 0 && b === 0) {
          [r, g, b] = defaultColor
        }
      }

      return {
        '--user-color': `rgb(${r}, ${g}, ${b})`
      }
    }
  }
}
</script>

<style lang="scss">
.user-name {
  color: var(--user-color)
}
</style>
