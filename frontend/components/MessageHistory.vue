<template>
  <section class="message-history">
    <Markdown v-for="(version, index) in message.versions" :key="index" :content="version.content" :context="message">
      <span class="message-history__timestamp--float" aria-hidden="true">
        {{ formatTimestampRelative(version.timestamp) }}
      </span>
      <time :datetime="version.timestamp" :title="formatTimestamp(version.timestamp)" class="message-history__timestamp">
        {{ formatTimestampRelative(version.timestamp) }}
      </time>
    </Markdown>
  </section>
</template>

<script>
import formatRelative from 'date-fns/formatRelative'
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
    formatTimestampRelative (timestamp) {
      return formatRelative(Date.parse(timestamp), new Date())
    },
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
  overflow-y: scroll;
  flex: 1;
  padding-right: 0.5rem;

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
    white-space: normal;

    &--float {
      float: right;
      visibility: hidden;
      margin-top: 0.25rem;
      margin-left: 0.25rem;
      white-space: normal;
    }
  }
}
</style>
