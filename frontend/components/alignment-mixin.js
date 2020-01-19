import { createPopper } from '@popperjs/core'

export default {
  data () {
    return {
      alignment: 'top'
    }
  },
  destroyed () {
    this.stopAlignment()
  },
  methods: {
    startAlignment () {
      if (!this.$refs.aligned) {
        return
      }
      const element = this.$refs.aligned.$el ?? this.$refs.aligned
      this.popper = createPopper(this.$el, element, {
        placement: this.alignment,
        modifiers: [
          {
            name: 'offset',
            options: {
              offset: [0, 8]
            }
          },
          {
            name: 'preventOverflow',
            options: {
              padding: 8
            }
          },
          {
            name: 'flip',
            options: {
              fallbackPlacements: this.overflowAlignments
            }
          }
        ]
      })
      this.$nextTick(() => {
        this.popper.update()
      })
    },
    stopAlignment () {
      if (this.popper) {
        this.popper.destroy()
        this.popper = null
      }
    }
  }
}
