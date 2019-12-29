<template>
  <span :style="roleStyle" class="role-mention">@{{ roleName }}</span>
</template>

<script>
import roleQuery from '@/apollo/queries/role.gql'

export default {
  name: 'UserMention',
  props: {
    server: {
      type: String,
      required: true
    },
    id: {
      type: String,
      required: true
    }
  },
  computed: {
    roleName () {
      return this.role?.name ?? this.id
    },
    roleStyle () {
      const defaultColor = [114, 137, 218, 0.7]
      let [r, g, b, hoverAlpha] = defaultColor

      if (this.role !== undefined && this.role !== null) {
        ({ r, g, b } = this.role.color)
        hoverAlpha = 0.3

        if (r === 0 && g === 0 && b === 0) {
          [r, g, b, hoverAlpha] = defaultColor
        }
      }

      return {
        '--mention-bg-color': `rgba(${r}, ${g}, ${b}, .1)`,
        '--mention-text-color': `rgba(${r}, ${g}, ${b}, 1)`,
        '--mention-hover-bg-color': `rgba(${r}, ${g}, ${b}, ${hoverAlpha})`
      }
    }
  },
  apollo: {
    role: {
      query: roleQuery,
      variables () {
        return {
          server: this.server,
          id: this.id
        }
      }
    }
  }
}
</script>

<style lang="scss">
.role-mention {
  color: var(--mention-text-color);
  background-color: var(--mention-bg-color);
  transition: background-color 50ms ease-out, color 50ms ease-out;
  cursor: pointer;

  &:hover {
    color: white;
    background-color: var(--mention-hover-bg-color);
  }
}
</style>
