<template>
  <section class="message-group">
    <div class="message-group__header">
      {{ group.author.name }}#{{ group.author.discriminator }}

      <time :datetime="group.firstTimestamp">{{ formattedTime }}</time>
    </div>
    <article
      v-for="message in group.messages"
      v-html="message.versions[0].content"
      :class="['message', { 'message--deleted': message.deletedAt !== null }]"
    />
  </section>
</template>

<script>

export default {
  name: 'MessageGroup',
  props: {
    group: {
      type: Object,
      required: true
    }
  },
  computed: {
    formattedTime () {
      const format = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: 'numeric' })
      return format.format(this.group.firstTimestamp)
    }
  }
}
</script>

<style lang="scss">
.message-group {
  padding: 2rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &__header {
    time {
      color: #72767d;
      font-size: 0.8rem;
    }
  }

  article {
    line-height: 1.57;
    white-space: pre-wrap;
    vertical-align: baseline;
  }

  &__note {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.625rem;
  }

  &--deleted {
    color: #f04747;
  }
}
</style>
