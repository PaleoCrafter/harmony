<template>
  <div ref="container" class="channel__messages">
    <div
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="mayNotLoad"
      infinite-scroll-distance="100"
      class="channel__messages-container"
    >
      <MessageList v-if="fetchingMore || !loading" :messages="messages || []" />
      <div
        v-if="loading || messages === undefined"
        :class="['channel__loading', { 'channel__loading--empty': messages === undefined || messages.length === 0 || !fetchingMore }]"
      >
        <LoadingSpinner />
      </div>
      <div
        v-if="!loading && (endReached && isToday || messages !== undefined && messages.length === 0)"
        :class="['channel__info', { 'channel__info--empty': messages === undefined || messages.length === 0 }]"
      >
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
  </div>
</template>

<script>
import { utcToZonedTime, zonedTimeToUtc } from 'date-fns-tz'
import addDays from 'date-fns/addDays'
import startOfDay from 'date-fns/startOfDay'
import endOfDay from 'date-fns/endOfDay'
import { mapState } from 'vuex'
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
      type: null,
      required: true
    }
  },
  data () {
    return {
      ...this.getInitialDates(),
      loading: false,
      endReached: false,
      autoRefresh: false,
      refreshHandle: null,
      fetchingMore: false
    }
  },
  computed: {
    ...mapState(['timezone']),
    mayNotLoad () {
      return this.$apollo.loading || this.endReached || this.messages === undefined || this.messages?.length === 0
    },
    isToday () {
      const today = utcToZonedTime(Date.now(), this.timezone)

      return this.date.getFullYear() === today.getFullYear() && this.date.getMonth() === today.getMonth() && this.date.getDate() === today.getDate()
    }
  },
  watch: {
    date () {
      this.messages = undefined
    },
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
      fetchPolicy: 'cache-and-network',
      watchLoading (loading) {
        this.$nextTick(() => {
          this.loading = loading
        })
      }
    }
  },
  methods: {
    getInitialDates () {
      const zoned = zonedTimeToUtc(this.date, this.timezone)
      const startDate = zonedTimeToUtc(endOfDay(addDays(zoned, -1)), this.timezone)
      const endDate = zonedTimeToUtc(startOfDay(addDays(zoned, 1)), this.timezone)

      return { startDate, endDate }
    },
    async loadMore () {
      const lastMessage = this.messages[this.messages.length - 1]
      if (lastMessage !== undefined) {
        this.startDate = new Date(lastMessage.createdAt)
      }
      // Fetch more data and transform the original result
      try {
        this.fetchingMore = true
        await this.$apollo.queries.messages.fetchMore(
          {
            variables: {
              channel: this.$route.params.channelId,
              after: this.startDate.toISOString(),
              before: this.endDate.toISOString()
            },
            updateQuery: (previousResult, { fetchMoreResult }) => {
              const oldMessages = previousResult?.messages ?? []
              const newMessages = fetchMoreResult.messages
              const hasMore = newMessages.length > 0

              this.endReached = !hasMore

              return {
                messages: [...oldMessages, ...newMessages.filter(msg => oldMessages.find(old => old.id === msg.id) === undefined)]
              }
            }
          }
        )
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error)
      }
      this.fetchingMore = false
    },
    async performAutoRefresh () {
      await this.loadMore()
      this.refreshHandle = setTimeout(this.performAutoRefresh, 30000)
    }
  },
  provide () {
    const self = this
    return {
      alignmentBounds () {
        let element = self.$refs.container
        if (element === undefined) {
          return undefined
        }

        element = element.$el ?? element

        const scrollbarWidth = element.offsetWidth - element.clientWidth
        const scrollbarHeight = element.offsetHeight - element.clientHeight
        const baseRect = element.getBoundingClientRect()
        return new DOMRect(baseRect.x, baseRect.y, baseRect.width - scrollbarWidth, baseRect.height - scrollbarHeight)
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
    align-items: stretch;
    position: relative;
    flex: 1;
    flex-basis: 0;
    overflow: hidden;

    &-container {
      display: flex;
      flex: 1;
      flex-basis: 0;
      position: relative;
      flex-direction: column;
      padding: 0 1rem 1rem;
      align-items: center;
      overflow-y: scroll;
    }
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    font-size: 4rem;
    padding: 1rem;

    &--empty {
      flex-grow: 1;
    }
  }

  &__info {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    padding: 1rem;
    flex-wrap: wrap;
    flex-basis: 0;

    &--empty {
      flex-grow: 1;
    }
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
