<template>
  <div class="message-list">
    <MessageGroup v-for="(group, index) in groupedMessages" :key="group.firstTimestamp.toISOString() + index" :group="group" />
  </div>
</template>

<script>
import MessageGroup from '@/components/MessageGroup.vue'

export default {
  name: 'MessageList',
  components: { MessageGroup },
  props: {
    messages: {
      type: Array,
      required: true
    }
  },
  computed: {
    groupedMessages () {
      if (this.messages === undefined) {
        return []
      }

      const groups = []

      let currentGroup = null
      let lastMessage = null

      this.messages.forEach((rawMessage) => {
        const msg = { ...rawMessage }

        msg.createdAt = new Date(msg.createdAt)

        if (
          (currentGroup === null && lastMessage === null) ||
          msg.author.name !== lastMessage.author.name ||
          msg.author.discriminator !== lastMessage.author.discriminator ||
          msg.createdAt - lastMessage.createdAt >= 5 * 60 * 1000
        ) {
          currentGroup = {
            author: msg.author,
            firstTimestamp: msg.createdAt,
            messages: []
          }
          groups.push(currentGroup)
        }

        currentGroup.messages.push(msg)
        lastMessage = msg
      })

      return groups
    }
  }
}
</script>

<style lang="scss">
.message-list {
  display: flex;
  align-self: stretch;
  flex-direction: column;
  align-items: stretch;
  flex-basis: 0;
}
</style>
