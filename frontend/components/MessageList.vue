<template>
  <div class="message-list">
    <MessageGroup v-for="group in groupedMessages" :group="group" :key="group.firstTimestamp.toISOString()" />
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
        return undefined
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
  padding: 0 1rem 1rem;
  overflow-y: auto;
  flex: 1;
  flex-basis: 0;
}
</style>
