<template>
  <section class="message-group">
    <div class="message-group__header">
      <UserName :server="group.messages[0].server" :user="group.author" use-role-color />
      <BotTag v-if="group.author.bot" />
      <time :datetime="group.firstTimestamp">{{ formattedTime }}</time>
      <slot name="header" />
    </div>
    <Message
      v-for="message in group.messages"
      :key="message.id"
      :message="message"
    />
  </section>
</template>

<script>
import formatRelative from 'date-fns/formatRelative'
import Message from '@/components/Message.vue'
import UserName from '@/components/UserName.vue'
import BotTag from '@/components/BotTag.vue'

export default {
  name: 'MessageGroup',
  components: { UserName, Message, BotTag },
  props: {
    group: {
      type: Object,
      required: true
    },
    relativeTime: {
      type: Boolean,
      default: () => false
    }
  },
  computed: {
    formattedTime () {
      if (this.relativeTime) {
        return formatRelative(this.group.firstTimestamp, new Date())
      }

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
  position: relative;

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
