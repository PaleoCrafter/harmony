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
        element.style.display = 'block'
        element.style.transform = null

        let bounds = this.alignmentBounds()
        if (bounds === undefined) {
          bounds = new DOMRect(0, 0, window.innerWidth, window.innerHeight)
        }
        const { left, right, top, bottom } = element.getBoundingClientRect()
        if (this.alignment === 'center') {
          this.alignment = left < bounds.left ? 'left' : right > bounds.right ? 'right' : 'center'
        } else if (this.alignment === 'left') {
          this.alignment = left < bounds.left ? 'right' : right > bounds.right ? 'center' : 'left'
        } else if (this.alignment === 'right') {
          this.alignment = left < bounds.left ? 'center' : right > bounds.right ? 'left' : 'right'
        }

        if (this.verticalAlignment === 'top' && top < bounds.top) {
          this.verticalAlignment = 'bottom'
        } else if (this.verticalAlignment === 'bottom' && bottom > bounds.bottom) {
          this.verticalAlignment = 'top'
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
