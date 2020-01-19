<template>
  <span :class="['user-name', { 'user-name--info-visible': infoVisible }]">
    <span @click.self="toggleInfoBox" :style="userStyle">{{ prefix }}{{ userName }}</span>

    <UserInfo
      ref="aligned"
      :load="infoVisible"
      :server="server"
      :user="user"
      class="user-name__info"
    />
  </span>
</template>

<script>
import alignment from '@/components/alignment-mixin'
import UserInfo from '@/components/UserInfo.vue'

export default {
  name: 'UserName',
  components: { UserInfo },
  mixins: [alignment],
  props: {
    server: {
      type: String,
      required: true
    },
    user: {
      type: Object,
      required: true
    },
    prefix: {
      type: String,
      default: () => ''
    },
    useRoleColor: {
      type: Boolean,
      default: () => false
    }
  },
  data () {
    return {
      infoVisible: false,
      alignment: 'right-start',
      overflowAlignments: ['bottom-start', 'top-start']
    }
  },
  computed: {
    userName () {
      return this.user?.nickname ?? this.user.name
    },
    userStyle () {
      if (!this.useRoleColor) {
        return undefined
      }

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
  },
  mounted () {
    document.addEventListener('click', this.handleOutsideClick)
  },
  destroyed () {
    document.removeEventListener('click', this.handleOutsideClick)
  },
  methods: {
    toggleInfoBox () {
      if (this.infoVisible) {
        this.stopAlignment()
      } else {
        this.startAlignment()
      }
      this.infoVisible = !this.infoVisible
    },
    handleOutsideClick (event) {
      if (!this.$el.contains(event.target)) {
        this.infoVisible = false
        this.stopAlignment()
      }
    }
  }
}
</script>

<style lang="scss">
.user-name {
  position: relative;
  display: inline-flex;
  align-items: center;

  & > span {
    font-weight: 600;
    color: var(--user-color);

    &:hover {
      cursor: pointer;
      text-decoration: underline;
    }
  }

  &__info {
    display: none;
    position: absolute;
    box-shadow: 0 2px 10px 0 rgba(0, 0, 0, .2), 0 0 0 1px rgba(32, 34, 37, .6);
    min-width: 250px;
    max-width: 250px;
    z-index: 50;
  }

  &--info-visible .user-name__info {
    display: flex;
    animation-name: user-name__info--enter;
    animation-duration: 0.12s;
    animation-timing-function: ease-in-out;
  }
}

@keyframes user-name__info--enter {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}
</style>
