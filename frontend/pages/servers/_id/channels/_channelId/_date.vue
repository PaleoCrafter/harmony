<template>
  <div
    ref="container"
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
    <div v-if="!$apollo.loading && (endReached && isToday || messages !== undefined && messages.length === 0)" class="channel__info">
      <div v-if="messages !== undefined && messages.length === 0" class="channel__empty">
        There are currently no messages in this channel for the selected date.
      </div>
      <div v-if="(endReached || messages !== undefined && messages.length === 0) && isToday" class="channel__more">
        <button @click="loadMore" :disabled="autoRefresh" class="channel__button">
          Refresh
        </button>
        <Divider />
        <span class="channel__auto-refresh">
          <input id="channel__auto-refresh" v-model="autoRefresh" type="checkbox">
          <label for="channel__auto-refresh">Auto-refresh every 30 seconds</label>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import messagesQuery from '@/apollo/queries/channel-messages.gql'
import MessageList from '@/components/MessageList.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Divider from '@/components/Divider.vue'

export default {
  components: { Divider, LoadingSpinner, MessageList },
  validate ({ params: { date } }) {
    if (date === undefined) {
      return true
    }

    return /\d+-\d{2}-\d{2}/.test(date)
  },
  props: {
    date: {
      type: Date,
      required: true
    }
  },
  data () {
    return {
      ...this.getInitialDates(),
      endReached: false,
      autoRefresh: false,
      refreshHandle: null
    }
  },
  computed: {
    mayNotLoad () {
      return this.$apollo.loading || this.endReached || this.messages === undefined || this.messages?.length === 0
    },
    isToday () {
      const today = new Date()

      return this.date.getFullYear() === today.getFullYear() && this.date.getMonth() === today.getMonth() && this.date.getDate() === today.getDate()
    }
  },
  watch: {
    '$route.params.channelId': {
      handler () {
        this.messages = undefined
      }
    },
    autoRefresh (refresh) {
      if (!refresh && this.refreshHandle !== null) {
        clearTimeout(this.refreshHandle)
      } else if (refresh) {
        this.refreshHandle = setTimeout(this.performAutoRefresh, 30000)
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
    getInitialDates () {
      const startDate = new Date(this.date.getTime())
      startDate.setHours(23, 59, 59, 59)
      startDate.setDate(startDate.getDate() - 1)
      const endDate = new Date(this.date.getTime())
      endDate.setHours(0, 0, 0, 0)
      endDate.setDate(endDate.getDate() + 1)
      return { startDate, endDate }
    },
    async loadMore () {
      const lastMessage = this.messages[this.messages.length - 1]
      if (lastMessage !== undefined) {
        this.startDate = new Date(lastMessage.createdAt)
      }
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
                messages: [...(previousResult?.messages ?? []), ...newMessages]
              }
            }
          }
        )
      } catch {
      }
    },
    async performAutoRefresh () {
      await this.loadMore()
      this.refreshHandle = setTimeout(this.performAutoRefresh, 30000)
    }
  },
  provide () {
    const self = this
    return {
      tooltipBounds () {
        return self.$refs.container.getBoundingClientRect()
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

  &__info {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    flex-grow: 1;
    padding: 1rem
  }

  &__empty, &__more {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
  }

  &__empty {
    margin-bottom: 1rem;
  }

  &__button {
    position: relative;
    color: #fff;
    background: #7289da;
    cursor: pointer;
    text-decoration: none;
    border: none;
    font-size: 1rem;
    border-radius: 3px;
    padding: 0.5rem 1rem;
    z-index: 1;

    &:hover, &:active, &:focus {
      background-color: #677bc4;
    }

    &:disabled {
      cursor: not-allowed;
      background-color: #7984ad;
      color: #cfcfcf;
    }
  }

  &__auto-refresh {
    display: flex;
    align-items: center;
    line-height: 1;

    input {
      margin-right: 0.5rem;
    }
  }
}
</style>
