<template>
  <div class="channel__messages">
    <div
      v-infinite-scroll="loadMoreForward"
      infinite-scroll-disabled="mayNotLoadForward"
      infinite-scroll-backward="loadMoreBackward"
      infinite-scroll-backward-disabled="mayNotLoadBackward"
      infinite-scroll-distance="100"
      class="channel__messages-container"
    >
      <div v-if="fetchingMoreBackward" class="channel__loading">
        <LoadingSpinner />
      </div>
      <MessageList v-if="fetchingMoreBackward || fetchingMore || !loading" ref="messages" :messages="messages || []" />
      <div
        v-if="(loading && !fetchingMoreBackward) || messages === undefined"
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
          <button :disabled="autoRefresh" class="channel__button" @click="loadMoreForward">
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
    },
    mayLoad: {
      type: Boolean
    }
  },
  data () {
    return {
      ...this.getInitialDates(this.$store.state.timezone),
      loading: false,
      startReached: this.$route.query.message === undefined,
      endReached: false,
      autoRefresh: false,
      refreshHandle: null,
      fetchingMoreBackward: false,
      fetchingMore: false,
      scrolledToMessage: false
    }
  },
  computed: {
    ...mapState(['timezone']),
    mayNotLoadBackward () {
      return this.startReached || this.mayNotLoadCommon
    },
    mayNotLoadForward () {
      return this.endReached || this.mayNotLoadCommon
    },
    mayNotLoadCommon () {
      return this.$apollo.loading || this.messages === undefined || this.messages?.length === 0 ||
        (this.$route.query.message && !this.scrolledToMessage)
    },
    isToday () {
      const today = utcToZonedTime(Date.now(), this.timezone)

      return this.date.getFullYear() === today.getFullYear() && this.date.getMonth() === today.getMonth() && this.date.getDate() === today.getDate()
    }
  },
  watch: {
    date () {
      this.reset()
    },
    '$route.params.channelId': {
      handler () {
        this.reset()
      }
    },
    '$route.query': {
      handler () {
        this.reset()
      }
    },
    autoRefresh (refresh) {
      if (!refresh && this.refreshHandle !== null) {
        clearTimeout(this.refreshHandle)
      } else if (refresh) {
        this.refreshHandle = setTimeout(this.performAutoRefresh, 30000)
      }
    },
    mayLoad (may) {
      if (may) {
        const { startDate, endDate } = this.getInitialDates(this.$store.state.timezone)
        this.startDate = startDate
        this.endDate = endDate
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
        const { query } = this.$route
        const { startDate, endDate } = this.getInitialDates()

        const paginationParameters = {
          paginationMode: 'AFTER',
          paginationReference: {
            minTime: startDate.toISOString(),
            maxTime: endDate.toISOString()
          }
        }
        if (query.message) {
          paginationParameters.paginationMode = 'AROUND'
          paginationParameters.paginationReference.message = query.message
        }

        return {
          channel: this.$route.params.channelId,
          ...paginationParameters
        }
      },
      fetchPolicy: 'cache-and-network',
      watchLoading (loading) {
        this.$nextTick(() => {
          this.loading = loading

          if (process.client) {
            this.$nextTick(() => {
              this.scrollToMessage()
            })
          }
        })
      },
      skip () {
        return !this.mayLoad
      }
    }
  },
  mounted () {
    if (this.$refs.messages) {
      this.scrollToMessage()
    } else {
      this.nextTick(() => {
        this.scrollToMessage()
      })
    }
  },
  methods: {
    scrollToMessage () {
      const { message } = this.$route.query
      if (!this.scrolledToMessage && !this.loading && message) {
        const messageElement = this.$refs.messages.$el.querySelector(`[data-message-id="${message}"]`)
        messageElement.scrollIntoView()
        this.scrolledToMessage = true
        this.$store.commit('setHighlightedMessage', message)
      }
    },
    getInitialDates (timezone) {
      const usedTimezone = this.timezone || timezone
      const zoned = utcToZonedTime(this.date, usedTimezone)
      const startDate = zonedTimeToUtc(endOfDay(addDays(zoned, -1)), usedTimezone)
      const endDate = zonedTimeToUtc(startOfDay(addDays(zoned, 1)), usedTimezone)

      return { startDate, endDate }
    },
    async loadMoreForward () {
      const lastMessage = this.messages[this.messages.length - 1]
      // Fetch more data and transform the original result
      try {
        this.fetchingMore = true
        await this.$apollo.queries.messages.fetchMore(
          {
            variables: {
              channel: this.$route.params.channelId,
              paginationMode: 'AFTER',
              paginationReference: {
                message: lastMessage.ref,
                minTime: this.startDate.toISOString(),
                maxTime: this.endDate.toISOString()
              }
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
    async loadMoreBackward () {
      const firstMessage = this.messages[0]
      // Fetch more data and transform the original result
      try {
        this.fetchingMoreBackward = true
        await this.$apollo.queries.messages.fetchMore(
          {
            variables: {
              channel: this.$route.params.channelId,
              paginationMode: 'BEFORE',
              paginationReference: {
                message: firstMessage.ref,
                minTime: this.startDate.toISOString(),
                maxTime: this.endDate.toISOString()
              }
            },
            updateQuery: (previousResult, { fetchMoreResult }) => {
              const oldMessages = previousResult?.messages ?? []
              const newMessages = [...fetchMoreResult.messages]
              newMessages.reverse()
              const hasMore = newMessages.length > 0

              this.startReached = !hasMore

              return {
                messages: [...newMessages.filter(msg => oldMessages.find(old => old.id === msg.id) === undefined), ...oldMessages]
              }
            }
          }
        )
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error)
      }
      this.$nextTick(() => {
        const messageElement = this.$refs.messages.$el.querySelector(`[data-message-id="${firstMessage.ref}"]`)
        messageElement.scrollIntoView()
        this.fetchingMoreBackward = false
      })
    },
    async performAutoRefresh () {
      await this.loadMoreForward()
      this.refreshHandle = setTimeout(this.performAutoRefresh, 30000)
    },
    reset () {
      this.messages = undefined
      this.scrolledToMessage = false
      const { startDate, endDate } = this.getInitialDates(this.$store.state.timezone)
      this.startDate = startDate
      this.endDate = endDate
      this.$store.commit('resetHighlightedMessage')
      this.$apollo.queries.messages.refresh()
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

    .message--highlighted {
      background: rgba(114, 137, 218, 0.5);
      animation: message--highlight 1s;
      animation-delay: 1s;
      animation-fill-mode: forwards;
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

@keyframes message--highlight {
  0% {
    background: rgba(114, 137, 218, 0.5);
  }
  100% {
    background: rgba(114, 137, 218, 0);
  }
}
</style>
