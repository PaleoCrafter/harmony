<template>
  <div
    v-infinite-scroll="loadMore"
    infinite-scroll-disabled="mayNotLoad"
    infinite-scroll-distance="100"
    infinite-scroll-listen-for-event="reset-infinite"
    class="channel__messages"
  >
    <MessageList :messages="messages || []" />
    <div v-if="$apollo.loading || messages === undefined" class="channel__loading">
      <LoadingSpinner />
    </div>
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import messagesQuery from '@/apollo/queries/channel-messages.gql'
import MessageList from '@/components/MessageList.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  components: { LoadingSpinner, MessageList },
  validate ({ params: { date } }) {
    if (date === undefined) {
      return true
    }

    return /\d{4}-\d{2}-\d{2}/.test(date)
  },
  data () {
    return {
      ...this.getInitialDates(),
      endReached: false
    }
  },
  computed: {
    mayNotLoad () {
      return this.$apollo.loading || this.endReached || this.messages === undefined || this.messages?.length === 0
    }
  },
  watch: {
    '$route.params.channelId': {
      handler () {
        this.messages = undefined
      }
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
        const { startDate, endDate } = this.getInitialDates()
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
    getBaseDate () {
      if (this.$route.params.date === undefined) {
        return new Date()
      }

      const [, year, month, day] = this.$route.params.date.match(/(\d{4})-(\d{2})-(\d{2})/)
      return new Date(year, parseInt(month) - 1, day)
    },
    getInitialDates () {
      const startDate = this.getBaseDate()
      startDate.setHours(23, 59, 59, 59)
      startDate.setDate(startDate.getDate() - 1)
      const endDate = this.getBaseDate()
      endDate.setHours(0, 0, 0, 0)
      endDate.setDate(endDate.getDate() + 1)
      return { startDate, endDate }
    },
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
