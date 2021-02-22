<template>
  <section class="message-group">
    <div v-if="!group.messages[0].isCrosspost && group.messages[0].hasReference" class="message-group__reply">
      <MessageReference :referencing-message="group.messages[0]" class="message-group__reply-content" />
    </div>
    <div class="message-group__header">
      <UserName :server="group.messages[0].server" :user="group.author" use-role-color />
      <BotTag v-if="group.author.bot && !group.messages[0].isCrosspost" />
      <BotTag v-else-if="group.author.bot">
        Server
      </BotTag>
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
import MessageReference from '~/components/message/MessageReference.vue'

export default {
  name: 'MessageGroup',
  components: { MessageReference, UserName, Message, BotTag },
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

  &__reply {
    display: flex;
    align-items: stretch;
    padding-left: 0.25rem;
    padding-top: 0.5rem;

    &:before {
      content: '';
      width: 1.5rem;
      min-height: 1rem;
      border-top-left-radius: 0.5rem;
      border-top: 2px solid rgba(255, 255, 255, 0.2);
      border-left: 2px solid rgba(255, 255, 255, 0.2);
      margin-right: 0.5rem;
    }

    &-content {
      margin-top: -0.5rem;
    }
  }

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
