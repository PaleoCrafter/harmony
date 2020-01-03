<template>
  <div :class="['embed', `embed--${embed.type.toLowerCase()}`]" :style="{ '--embed-accent': accentColor }">
    <div v-if="embed.provider !== null" class="embed__provider">
      <a v-if="embed.provider.url !== null" :href="embed.provider.url" target="_blank">{{ embed.provider.name || embed.provider.url }}</a>
      <span v-else>{{ embed.provider.name }}</span>
    </div>
    <div v-if="title !== null" class="embed__title">
      <a v-if="embed.url !== null" :href="embed.url" target="_blank">{{ title }}</a>
      <span v-else>{{ title }}</span>
    </div>
    <Markdown v-if="embed.description !== null" :content="embed.description" class="embed__description" />
    <img v-if="embed.image !== null" :src="embed.image.proxyUrl" alt="image" class="embed__image">
    <Markdown v-if="embed.footer !== null" :content="embed.footer.text" class="embed__footer" />
    <img v-if="embed.thumbnail !== null" :src="embed.thumbnail.proxyUrl" alt="thumbnail" class="embed__thumbnail">
  </div>
</template>

<script>
import Markdown from '@/components/message/Markdown.vue'

export default {
  name: 'Embed',
  components: { Markdown },
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
  max-width: 40%;

  &__provider {
    margin-top: 0.5rem;
    font-size: 0.75rem;

    a, span {
      color: #b9bbbe !important;
    }

    grid-column: 1;
  }

  &__title {
    font-weight: bold;
    margin-top: 0.5rem;
    grid-column: 1;
  }

  &__description {
    margin-top: 0.5rem;
    font-size: 0.9rem;
    line-height: 1.175rem;
    white-space: pre-line;
    grid-column: 1;
  }

  &__footer {
    margin-top: 0.5rem;
    font-size: 0.75rem;
    line-height: 1rem;
    color: #72767d;
    grid-column: 1;
  }

  &__thumbnail, &__image {
    max-width: 100%;
    margin-top: 0.5rem;
    border-radius: 4px;
    grid-column: 1;
  }

  .markdown__code {
    background: #202225 !important;
  }

  &--rich {
    display: grid;
    grid-auto-columns: auto;

    .embed__thumbnail {
      max-width: 80px;
      grid-column: 2;
    }
  }
}
</style>
