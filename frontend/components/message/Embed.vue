<template>
  <div class="embed" :style="{ '--embed-accent': accentColor }">
    <div v-if="embed.provider !== null" class="embed__provider">
      <a v-if="embed.provider.url !== null" :href="embed.provider.url" target="_blank">{{ embed.provider.name || embed.provider.url }}</a>
      <span v-else>{{ embed.provider.name }}</span>
    </div>
    <div v-if="title !== null" class="embed__title">
      <a v-if="embed.url !== null" :href="embed.url" target="_blank">{{ title }}</a>
      <span v-else>{{ title }}</span>
    </div>
    <div v-if="embed.description !== null" class="embed__description">{{ embed.description }}</div>
  </div>
</template>

<script>
export default {
  name: 'Embed',
  props: {
    embed: {
      type: Object,
      required: true
    }
  },
  computed: {
    accentColor () {
      const { r, g, b } = this.embed.color ?? { r: 32, g: 34, b: 37 }
      return `rgb(${r}, ${g}, ${b})`
    },
    title () {
      return this.embed.title ?? this.embed.url
    }
  }
}
</script>

<style lang="scss">
.embed {
  display: inline-flex;
  flex-direction: column;
  white-space: normal;
  border-radius: 4px;
  padding: 0.5rem 1rem 1rem;
  background: #2f3136;
  border-left: var(--embed-accent) 3px solid;
  max-width: 33%;

  &__provider {
    margin-top: 0.5rem;
    font-size: 0.75rem;

    a, span {
      color: #b9bbbe !important;
    }
  }

  &__title {
    font-weight: bold;
    margin-top: 0.5rem;
  }

  &__description {
    margin-top: 0.5rem;
    font-size: 0.9rem;
    line-height: 1.175rem;
    white-space: pre-line;
  }
}
</style>
