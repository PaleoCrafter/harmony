export default {
  data () {
    return {
      alignment: 'center',
      verticalAlignment: 'top'
    }
  },
  inject: {
    alignmentBounds: {
      default () {
        return () => undefined
      }
    }
  },
  methods: {
    calculateAlignment (hook) {
      if (this.$refs.aligned === undefined) {
        if (hook !== undefined) {
          hook()
        }

        return
      }

      const element = this.$refs.aligned.$el ?? this.$refs.aligned

      this.alignment = this.defaultAlignment
      this.verticalAlignment = this.defaultVerticalAlignment

      this.$nextTick(() => {
        const initialDisplay = element.style.display ?? null
        const initialTransform = element.style.transform ?? null
        element.style.display = 'flex'
        element.style.mozTransform = 'scale(1) translate(0, 0)'
        element.style.webkitTransform = 'scale(1) translate(0, 0)'
        element.style.transform = 'scale(1) translate(0, 0)'

        let bounds = this.alignmentBounds()
        if (bounds === undefined) {
          bounds = new DOMRect(0, 0, window.innerWidth, window.innerHeight)
        }
        const { left, right, top, bottom, height } = element.getBoundingClientRect()
        if (this.alignment === 'center') {
          this.alignment = left < bounds.left ? 'left' : right > bounds.right ? 'right' : 'center'
        } else if (this.alignment === 'left') {
          this.alignment = left < bounds.left ? 'right' : right > bounds.right ? 'center' : 'left'
        } else if (this.alignment === 'right') {
          this.alignment = left < bounds.left ? 'center' : right > bounds.right ? 'left' : 'right'
        }

        if (this.verticalAlignment === 'middle') {
          this.verticalAlignment = top < bounds.top ? 'bottom' : bottom > bounds.bottom ? 'top' : 'middle'
        } else if (this.verticalAlignment === 'top') {
          const middlePreference = (bottom + height / 2 < bounds.bottom) ? 'middle' : 'bottom'
          this.verticalAlignment = top < bounds.top ? middlePreference : bottom > bounds.bottom ? 'middle' : 'top'
        } else if (this.verticalAlignment === 'bottom') {
          const middlePreference = (top - height / 2 > bounds.top) ? 'middle' : 'top'
          this.verticalAlignment = top < bounds.top ? 'middle' : bottom > bounds.bottom ? middlePreference : 'bottom'
        }

        element.style.transform = initialTransform
        element.style.display = initialDisplay

        if (hook !== undefined) {
          this.$nextTick(hook)
        }
      })
    }
  }
}
