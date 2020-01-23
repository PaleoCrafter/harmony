<template>
  <div ref="container" class="lazy-image">
    <img
      ref="image"
      v-show="!loading && !error"
      @click="openOverlay"
      :src="visible ? src : undefined"
      :alt="alt"
      v-bind="attributes"
      class="lazy-image__content"
    >
    <LoadingSpinner v-if="loading" class="lazy-image__loading" />
    <img
      v-if="error"
      src="https://discordapp.com/assets/e0c782560fd96acd7f01fda1f8c6ff24.svg"
      alt="Could not load image"
      class="lazy-image__error"
    >
    <portal v-if="overlayActive" to="modal">
      <div class="lazy-image__overlay">
        <img :src="src" :alt="alt">
      </div>
    </portal>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  name: 'LazyImage',
  components: { LoadingSpinner },
  props: {
    src: {
      type: String,
      required: true
    },
    alt: {
      type: String,
      required: true
    },
    attributes: {
      type: Object,
      default: () => {
      }
    }
  },
  data () {
    return {
      visible: false,
      loading: true,
      error: false,
      intersectionObserver: null,
      overlayActive: false,
      aspect: 1
    }
  },
  computed: mapGetters(['modalOpen']),
  watch: {
    src () {
      this.error = false
      this.loading = true
    },
    modalOpen (newState) {
      if (newState === false) {
        this.overlayActive = false
      }
    }
  },
  mounted () {
    this.intersectionObserver = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          this.visible = true
        }
      },
      {
        threshold: 0.25
      }
    )
    this.intersectionObserver.observe(this.$refs.container)
    this.$refs.image.addEventListener('load', () => {
      this.loading = false
      this.error = false
      this.aspect = this.$refs.image.width / this.$refs.image.height
    })
    this.$refs.image.addEventListener('error', () => {
      this.loading = false
      this.error = true
    })
  },
  methods: {
    openOverlay () {
      this.$store.commit('openModal', { type: 'simple' })
      this.overlayActive = true
    }
  }
}
</script>

<style lang="scss">
.lazy-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, .05);
  overflow: hidden;
  max-width: 100%;

  img {
    position: absolute;
    max-width: 100%;
  }

  &__content {
    top: 0;
    cursor: pointer;
  }

  &__loading {
    position: absolute !important;
    top: 50%;
    margin-top: -0.5em;
    font-size: 2em;
  }

  &__overlay {
    max-width: 100%;
    align-self: center;
    display: flex;
    justify-content: stretch;
    align-items: stretch;
    overflow: hidden;

    img {
      max-width: 100%;
      max-height: 600px;
      object-fit: contain;
      border-radius: 0.25rem;
    }
  }
}
</style>
