<template>
<div class="message-list"></div>
</template>

<script>
import { toHTML } from 'discord-markdown'

export default {
  name: 'MessageList',
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
      const format = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: 'numeric' })

      this.messages.forEach((rawMessage) => {
        const msg = { ...rawMessage }

        msg.createdAt = new Date(msg.createdAt)

        const messageProperties = [
          msg.editedAt !== null ? `<time datetime="${msg.editedAt}">edited</time>` : undefined,
          msg.deletedAt !== null ? `<time datetime="${msg.deletedAt}">deleted</time>` : undefined
        ].filter(prop => prop !== undefined)
        msg.versions = msg.versions.map(version => ({
          ...version,
          content: toHTML(version.content) + (messageProperties.length > 0 ? ` <span class="message__note">(${messageProperties.join(', ')})</span>` : '')
        }))

        if (
          (currentGroup === null && lastMessage === null) ||
          msg.author.name !== lastMessage.author.name ||
          msg.author.discriminator !== lastMessage.author.discriminator ||
          msg.createdAt - currentGroup.lastTimestamp >= 5 * 60 * 1000
        ) {
          currentGroup = {
            author: msg.author,
            firstTimestamp: msg.createdAt,
            formattedTime: format.format(msg.createdAt),
            lastTimestamp: msg.createdAt,
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
