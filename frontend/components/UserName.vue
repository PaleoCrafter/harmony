<template>
  <span :class="['user-name', { 'user-name--info-visible': infoVisible }]">
    <span @click.self="toggleInfoBox" :style="userStyle">{{ userName }}</span>

    <UserInfo
      ref="aligned"
      @loaded="calculateAlignment()"
      :load="infoVisible"
      :server="server"
      :user="user"
      :class="['user-name__info', `user-name__info--${alignment}`, `user-name__info--${verticalAlignment}`]"
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
    }
  },
  data () {
    return {
      infoVisible: false,
      defaultAlignment: 'right',
      defaultVerticalAlignment: 'bottom'
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
  },
  mounted () {
    document.addEventListener('click', this.handleOutsideClick)
  },
  destroyed () {
    document.removeEventListener('click', this.handleOutsideClick)
  },
  methods: {
    toggleInfoBox () {
      if (!this.infoVisible) {
        this.calculateAlignment(() => {
          this.infoVisible = true
        })
      } else {
        this.infoVisible = false
      }
    },
    handleOutsideClick (event) {
      if (!this.$el.contains(event.target)) {
        this.infoVisible = false
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
    bottom: 0;
    margin-left: 0.5rem;
    box-shadow: 0 2px 10px 0 rgba(0, 0, 0, .2), 0 0 0 1px rgba(32, 34, 37, .6);
    min-width: 250px;
    max-width: 250px;
    z-index: 2;

    &--right {
      left: 100%;
      --enter-transform: translateX(1rem);
    }

    &--bottom {
      bottom: auto;
      top: 0;
    }

    &--middle {
      bottom: auto;
      top: auto;
    }

    &--left {
      left: 0;
      margin-left: 0;

      &.user-name__info--top {
        bottom: 100%;
        top: auto;
        margin-bottom: 0.5rem;
        --enter-transform: translateY(1rem);
      }

      &.user-name__info--bottom {
        top: 100%;
        bottom: auto;
        margin-top: 0.5rem;
        --enter-transform: translateY(1rem);
      }
    }
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
    transform: var(--enter-transform);
    opacity: 0;
  }
  100% {
    transform: translate(0, 0);
    opacity: 1;
  }
}
</style>
