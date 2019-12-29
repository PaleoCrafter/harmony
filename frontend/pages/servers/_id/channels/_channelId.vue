<template>
  <div class="channel">
    <header v-if="channel" class="channel__header">
      <ChannelName :channel="channel" />
    </header>
    <div
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="mayNotLoad"
      infinite-scroll-distance="100"
      infinite-scroll-listen-for-event="reset-infinite"
      class="channel__messages"
    >
      <MessageList :messages="messages || []" />
      <div v-if="$apollo.loading" class="channel__loading">
        <LoadingSpinner />
      </div>
    </div>
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import messagesQuery from '@/apollo/queries/channel-messages.gql'
import ChannelName from '@/components/ChannelName.vue'
import MessageList from '@/components/MessageList.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  components: { LoadingSpinner, MessageList, ChannelName },
  data () {
    const startDate = new Date()
    startDate.setHours(23, 59, 59, 59)
    startDate.setDate(startDate.getDate() - 1)
    const endDate = new Date()
    endDate.setHours(0, 0, 0, 0)
    endDate.setDate(endDate.getDate() + 1)
    return {
      startDate,
      endDate,
      endReached: false
    }
  },
  computed: {
    mayNotLoad () {
      return this.$apollo.loading || this.endReached || this.messages.length === 0
    }
  },
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
        const startDate = new Date()
        startDate.setHours(23, 59, 59, 59)
        startDate.setDate(startDate.getDate() - 1)
        const endDate = new Date()
        endDate.setHours(0, 0, 0, 0)
        endDate.setDate(endDate.getDate() + 1)
        return {
          channel: this.$route.params.channelId,
          after: startDate.toISOString(),
          before: endDate.toISOString()
        }
      },
      prefetch: false,
      fetchPolicy: 'network-only'
    }
  },
  methods: {
    async loadMore () {
      this.startDate = new Date(this.messages[this.messages.length - 1].createdAt)
      // Fetch more data and transform the original result
      try {
        await this.$apollo.queries.messages.fetchMore(
          {
            variables: {
              channel: this.$route.params.channelId,
              after: this.startDate.toISOString(),
              before: this.endDate.toISOString()
            },
            updateQuery: (previousResult, { fetchMoreResult }) => {
              const newMessages = fetchMoreResult.messages
              const hasMore = newMessages.length > 0

              this.endReached = !hasMore

              return {
                messages: [...previousResult.messages, ...newMessages]
              }
            }
          }
        )
      } catch {
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
  overflow: hidden;

  &__header {
    display: flex;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
  }

  &__messages {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0 1rem 1rem;
    position: relative;
    flex: 1;
    flex-basis: 0;
    overflow-y: scroll;
  }

  ::-webkit-scrollbar {
    width: 0.9rem;
    height: 0.9rem;
  }

  ::-webkit-scrollbar-corner {
    background-color: transparent;
  }

  ::-webkit-scrollbar-track {
    border-width: initial;
    background-color: transparent;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track, ::-webkit-scrollbar-thumb {
    background-clip: padding-box;
    border-width: 3px;
    border-style: solid;
    border-radius: 7px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #202225;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track-piece {
    background-color: #2f3136;
    border: 3px solid #36393f;
    border-radius: 7px;
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-grow: 1;
    flex-direction: column;
    font-size: 4rem;
    padding: 1rem
  }
}
</style>
