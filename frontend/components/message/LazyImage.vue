<template>
  <div class="lazy-image">
    <img v-show="!loading && !error" ref="image" :src="src" :alt="alt" v-bind="attributes">
    <LoadingSpinner v-if="loading" class="lazy-image__loading" />
    <img v-if="error" src="https://discordapp.com/assets/e0c782560fd96acd7f01fda1f8c6ff24.svg" alt="Could not load image">
  </div>
</template>

<script>
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
      loading: true,
      error: false
    }
  },
  watch: {
    src () {
      this.error = false
      this.loading = true
    }
  },
  mounted () {
    this.$refs.image.addEventListener('load', () => {
      this.loading = false
      this.error = false
    })
    this.$refs.image.addEventListener('error', () => {
      this.loading = false
      this.error = true
    })
  }
}
</script>

<style lang="scss">
.lazy-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, .05);
  overflow: hidden;

  img {
    max-width: 100%;
  }

  &__loading {
    font-size: 2em;
  }
}
</style>
