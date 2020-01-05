<template>
  <section class="message-group">
    <div class="message-group__header">
      <UserName :server="group.messages[0].server" :user="group.author" />

      <time :datetime="group.firstTimestamp">{{ formattedTime }}</time>
    </div>
    <Message
      v-for="message in group.messages"
      :message="message"
      :key="message.id"
    />
  </section>
</template>

<script>
import Message from '@/components/Message.vue'
import UserName from '@/components/UserName.vue'

export default {
  name: 'MessageGroup',
  components: { UserName, Message },
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
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex-basis: 0;
  padding: 1.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &__header {
    time {
      color: #72767d;
      font-size: 0.8rem;
    }
  }

  @media (max-width: 768px) {
    padding-right: 0;
  }
}
</style>
