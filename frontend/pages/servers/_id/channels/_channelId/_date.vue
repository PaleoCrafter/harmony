<template>
  <div ref="container" class="channel__messages">
    <div
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="mayNotLoad"
      infinite-scroll-distance="100"
      infinite-scroll-immediate-check="false"
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
    <transition :duration="400" name="channel__modal">
      <div v-if="$store.state.historyMessage !== null" @click.self="$store.commit('closeMessageHistory')" class="channel__modal-container">
        <div class="channel__modal">
          <div class="channel__modal-header">
            <h4>Message History</h4>
            <XIcon @click="$store.commit('closeMessageHistory')" />
          </div>
          <div ref="modalContent" class="channel__modal-content">
            <MessageHistory :message="$store.state.historyMessage" />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { XIcon } from 'vue-feather-icons'
import channelQuery from '@/apollo/queries/channel.gql'
import messagesQuery from '@/apollo/queries/channel-messages.gql'
import MessageList from '@/components/MessageList.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Divider from '@/components/Divider.vue'
import MessageHistory from '@/components/MessageHistory.vue'

export default {
  components: { MessageHistory, Divider, LoadingSpinner, MessageList, XIcon },
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
      loading: false,
      endReached: false,
      autoRefresh: false,
      refreshHandle: null,
      fetchingMore: false
    }
  },
  computed: {
    mayNotLoad () {
      return this.$apollo.loading || this.endReached || this.messages === undefined || this.messages?.length === 0
    },
    isToday () {
      const today = new Date(Date.now() - this.$store.state.timezone * 60000)

      return this.date.getFullYear() === today.getFullYear() && this.date.getMonth() === today.getMonth() && this.date.getDate() === today.getDate()
    }
  },
  watch: {
    date () {
      this.$store.commit('closeMessageHistory')
    },
    '$route.params.channelId': {
      handler () {
        this.$store.commit('closeMessageHistory')
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
      let startDate = new Date(this.date.getTime() - this.$store.state.timezone * 60000)
      startDate.setUTCHours(23, 59, 59, 999)
      startDate.setUTCDate(startDate.getUTCDate() - 1)
      startDate = new Date(startDate.getTime() + this.$store.state.timezone * 60000)

      let endDate = new Date(this.date.getTime() - this.$store.state.timezone * 60000)
      endDate.setUTCHours(0, 0, 0, 0)
      endDate.setUTCDate(endDate.getUTCDate() + 1)
      endDate = new Date(endDate.getTime() + this.$store.state.timezone * 60000)

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
              const newMessages = fetchMoreResult.messages
              const hasMore = newMessages.length > 0

              this.endReached = !hasMore

              return {
                messages: [...(previousResult?.messages ?? []), ...newMessages]
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
      tooltipBounds () {
        const element = self.$store.state.historyMessage !== null ? self.$refs.modalContent : self.$refs.container
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

  &__modal {
    display: flex;
    flex-direction: column;
    cursor: default;
    max-width: 640px;
    max-height: 90%;
    background: #36393f;
    box-shadow: 0 0 0 1px rgba(32, 34, 37, .6), 0 2px 10px 0 rgba(0, 0, 0, .2);
    border-radius: 0.5rem;
    padding: 1rem;
    align-items: stretch;
    justify-content: stretch;
    overflow: hidden;

    @media (max-width: 1200px) {
      max-width: 90%;
    }

    @media (max-width: 768px) {
      width: 100%;
      max-width: 100%;
      max-height: 90%;
      align-self: flex-end;
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }

    &-container {
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      right: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.7);
      cursor: pointer;
      z-index: 51;
    }

    &-header {
      display: flex;
      align-items: center;
      padding-bottom: 0.5rem;

      h4 {
        font-size: 1.5rem;
        font-weight: bold;
        line-height: 1;
        color: rgba(255, 255, 255, 0.8);
      }

      .feather {
        margin-left: auto;
        cursor: pointer;
      }
    }

    &-content {
      overflow-y: scroll;
      flex: 1;
      padding-right: 0.5rem;
    }

    &-enter-active, &-leave-active {
      transition: background .12s ease-in-out;
    }

    &-enter-active .channel__modal {
      transition: transform .4s ease-in-out;

      @media (max-width: 768px) {
        transition: transform .4s ease-out;
      }
    }

    &-enter, &-leave-to {
      background: rgba(0, 0, 0, 0.0);

      .channel__modal {
        transform: scale(0);

        @media (max-width: 768px) {
          transform: translateY(100%);
        }
      }
    }
  }
}
</style>
