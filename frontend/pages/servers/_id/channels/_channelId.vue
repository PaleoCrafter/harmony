<template>
  <div :class="['channel', { 'channel--search': modalSearchActive }]">
    <header v-if="channel" class="channel__header">
      <button class="channel__header-menu-toggle" aria-label="Toggle menu" @click.prevent="$store.commit('openSidebar')">
        <MenuIcon />
      </button>
      <h3>
        <ChannelName :channel="channel" />
      </h3>
      <Divider />
      <client-only>
        <div class="channel__date-selection">
          <button class="channel__date-button" aria-label="Previous day" @click="prevDay">
            <ChevronLeftIcon />
          </button>
          <DatePicker
            :value="date"
            :max-date="new Date()"
            :update-on-input="false"
            :popover="{ keepVisibleOnInput: false, placement: 'bottom', visibility: 'focus' }"
            :input-props="{ class: 'channel__date-input', 'aria-label': 'Date' }"
            color="gray"
            is-dark
            is-required
            class="channel__date"
            @input="updateDate"
          />
          <button :disabled="nextDayDisabled" class="channel__date-button" aria-label="Next day" @click="nextDay">
            <ChevronRightIcon />
          </button>
        </div>
      </client-only>
      <portal-target name="search-box" class="channel__search-box" />
      <button class="channel__search-toggle" aria-label="Toggle search" @click.prevent="toggleModalSearch">
        <SearchIcon v-if="!modalSearchActive" />
        <ArrowLeftIcon v-else />
      </button>
    </header>
    <nuxt-child v-if="!loadingLatest" :date="date" :may-load="!loadingLatest" class="channel__child" />
    <portal-target name="search-results" class="channel__search-results" />
  </div>
</template>

<script>
import { ArrowLeftIcon, ChevronLeftIcon, ChevronRightIcon, MenuIcon, SearchIcon } from 'vue-feather-icons'
import { toDate, utcToZonedTime } from 'date-fns-tz'
import { startOfDay } from 'date-fns'
import { mapState } from 'vuex'
import channelQuery from '@/apollo/queries/channel.gql'
import latestMessageQuery from '@/apollo/queries/latest-channel-message.gql'
import ChannelName from '@/components/ChannelName.vue'
import Divider from '@/components/Divider.vue'

export default {
  components: { Divider, ChannelName, MenuIcon, ChevronLeftIcon, ChevronRightIcon, SearchIcon, ArrowLeftIcon },
  data () {
    return {
      loadingLatest: false
    }
  },
  computed: {
    ...mapState(['timezone', 'modalSearchActive']),
    date () {
      if (this.$route.params.date === undefined) {
        const today = utcToZonedTime(Date.now(), this.timezone)
        return this.latestMessage ? startOfDay(utcToZonedTime(Number.parseInt(this.latestMessage.createdAt, 10), this.timezone)) : today
      }

      return toDate(this.$route.params.date, { timeZone: this.timezone })
    },
    nextDayDisabled () {
      const today = utcToZonedTime(Date.now(), this.timezone)

      return this.date.getFullYear() === today.getFullYear() && this.date.getMonth() === today.getMonth() && this.date.getDate() === today.getDate()
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
    latestMessage: {
      query: latestMessageQuery,
      fetchPolicy: 'network-only',
      variables () {
        return {
          channel: this.$route.params.channelId,
          before: null
        }
      },
      watchLoading (loading) {
        this.loadingLatest = loading
      }
    }
  },
  watch: {
    $route () {
      this.$store.commit('stopModalSearch')
    }
  },
  methods: {
    prevDay () {
      const newDate = new Date(this.date.getTime())
      newDate.setDate(newDate.getDate() - 1)
      this.updateDate(newDate)
    },
    nextDay () {
      const newDate = new Date(this.date.getTime())
      newDate.setDate(newDate.getDate() + 1)
      this.updateDate(newDate)
    },
    updateDate (date) {
      if (date === this.date || date === null) {
        return
      }

      const year = date.getFullYear().toString()
      const month = (date.getMonth() + 1).toString()
      const day = date.getDate().toString()
      const paddedMonth = month.length === 1 ? `0${month}` : month
      const paddedDay = day.length === 1 ? `0${day}` : day
      const isoDate = `${year}-${paddedMonth}-${paddedDay}`
      this.$router.push(`/servers/${this.$route.params.id}/channels/${this.$route.params.channelId}/${isoDate}`)
    },
    toggleModalSearch () {
      this.$store.commit(this.modalSearchActive ? 'stopModalSearch' : 'startModalSearch')
    }
  },
  head () {
    return {
      title: this.channel ? `#${this.channel.name}` : null
    }
  }
}
</script>

<style lang="scss">
.channel {
  flex-grow: 1;
  display: grid;
  grid-template-rows: auto 1fr;
  grid-template-columns: 1fr auto;
  align-items: stretch;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
    height: 4rem;
    line-height: 1;
    z-index: 51;
    grid-column: 1/span 2;

    @media (max-width: 499px) {
      display: grid;
      grid-template-columns: auto 1fr auto;
      height: auto;
      row-gap: 0.5rem;
      padding-bottom: 0.5rem;

      .divider {
        display: none;
      }
    }

    h3 {
      font-size: 1rem;
    }

    &-menu-toggle {
      color: white;
      background: none;
      border: none;
      outline: none;
      cursor: pointer;
      margin-right: 0.25rem;

      @media (min-width: 768px) {
        display: none;
      }
    }
  }

  &__date {
    &-selection {
      display: flex;
      align-items: stretch;

      @media (max-width: 499px) {
        grid-column: 1/span 3;
        justify-content: center;
      }
    }

    &-button {
      background: #202225;
      color: #dcddde;
      border: none;
      padding: 0 0.25rem;
      line-height: 1rem;
      border-radius: 4px;
      font-size: 1rem;

      &:hover, &:focus {
        cursor: pointer;
        background: darken(#202225, 5%);
      }

      &:disabled {
        cursor: not-allowed;
        background: lighten(#202225, 5%);
      }
    }

    &-input {
      background: #202225;
      color: #dcddde;
      border: none;
      padding: 0.5rem;
      line-height: 1rem;
      border-radius: 4px;
      font-size: 1rem;
      margin: 0 0.5rem;
    }

    & > .vc-popover-content-wrapper > .vc-popover-content {
      background: #18191c !important;
      border-radius: 0.25rem !important;
      box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.25) !important;
      border: none;

      & > .vc-container {
        background: none;
      }

      .vc-day-content {
        border-radius: 4px;

        &:hover {
          background: rgba(79, 84, 92, 0.16);
        }
      }

      .vc-highlight {
        border-radius: 4px;
        background: rgba(#7289DA, 0.5);
      }

      .vc-day:hover .vc-highlight {
        background: rgba(#7289DA, 0.7);
      }

      .vc-day .vc-opacity-0 {
        opacity: 0.5;
      }

      .vc-day .vc-pointer-events-none {
        pointer-events: all;
      }
    }
  }

  &__search-box {
    margin-left: auto;
  }

  &__search-results {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    overflow: hidden;
    background: #2F3136;
    grid-row: 2;
    grid-column: 2;
    width: 360px;

    &:empty {
      display: none;
    }
  }

  &__search-toggle {
    display: none;
    color: white;
    background: none;
    border: none;
    outline: none;
    margin-left: auto;
    cursor: pointer;

    @media (max-width: 499px) {
      grid-row: 1;
      grid-column: 3;
    }
  }

  @media (max-width: 959px) {
    .channel__search-box, .channel__search-results {
      display: none;
    }

    .channel__search-toggle {
      display: block;
    }

    &--search {
      .channel__header {
        display: grid !important;
        grid-template-columns: auto minmax(0, 1fr) !important;
        padding: 1rem;

        &-menu-toggle, h3, .channel__date-selection, .divider {
          display: none;
        }
      }

      .channel__child {
        display: none;
      }

      .channel__search-toggle {
        grid-column: 1 !important;
        grid-row: 1;
        padding: 0 0.5rem 0 0;
        margin-left: 0;
      }

      .channel__search-box {
        grid-column: 2 !important;
        grid-row: 1;
        display: block;
        flex: 1;
        margin-left: 0;
      }

      .channel__search-results {
        display: flex;
        width: 100%;
        grid-column: 1/span 2;

        &:empty {
          display: flex;

          &:before {
            content: 'Enter your search terms in the field above.';
            display: block;
            text-align: center;
            padding: 1rem;
          }
        }
      }
    }
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
}
</style>
