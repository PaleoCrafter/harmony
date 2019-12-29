<template>
  <div class="channel">
    <header v-if="channel" class="channel__header">
      <ChannelName :channel="channel" />
    </header>
    <MessageList :messages="messages" />
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import messagesQuery from '@/apollo/queries/channel-messages.gql'
import ChannelName from '@/components/ChannelName.vue'
import MessageList from '@/components/MessageList.vue'

export default {
  components: { MessageList, ChannelName },
  apollo: {
    channel: {
      query: channelQuery,
      variables () {
        return {
          id: this.$route.params.channelId
        }
      }
    },
    messages: {
      query: messagesQuery,
      variables () {
        return {
          channel: this.$route.params.channelId,
          after: '2019-12-28T23:59:59+01:00',
          before: '2019-12-30T00:00:00+01:00'
        }
      }
    }
  }
}
</script>

<style lang="scss">
.channel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;

  &__header {
    display: flex;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
  }
}
</style>
