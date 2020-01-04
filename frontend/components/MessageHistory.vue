<template>
  <section class="message-history">
    <Markdown v-for="(version, index) in message.versions" :key="index" :content="version.content" :context="message">
      <span class="message-history__timestamp--float" aria-hidden="true">{{ formatTimestamp(version.timestamp) }}</span>
      <time :datetime="version.timestamp" class="message-history__timestamp">{{ formatTimestamp(version.timestamp) }}</time>
    </Markdown>
  </section>
</template>

<script>
import Markdown from '@/components/message/Markdown.vue'

export default {
  name: 'MessageHistory',
  components: { Markdown },
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  methods: {
    formatTimestamp (timestamp) {
      const format = new Intl.DateTimeFormat(undefined, {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      })
      return format.format(Date.parse(timestamp))
    }
  }
}
</script>

<style lang="scss">
.message-history {
  display: flex;
  flex-direction: column;

  .markdown {
    padding: 0.75rem 0.5rem;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    position: relative;

    &:last-child {
      border-bottom: none;
    }
  }

  &__timestamp, &__timestamp--float {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.75rem;
  }

  &__timestamp {
    position: absolute;
    bottom: 0;
    right: 0;
    padding: 0.75rem 0.5rem;

    &--float {
      float: right;
      visibility: hidden;
      margin-top: 0.25rem;
      margin-left: 0.25rem;
    }
  }
}
</style>
