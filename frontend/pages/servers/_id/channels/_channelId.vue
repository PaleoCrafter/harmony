<template>
  <div class="channel">
    <header v-if="channel" class="channel__header">
      <ChannelName :channel="channel" />
      <Divider />
      <client-only>
        <DatePicker
          @input="updateDate"
          :value="date"
          :max-date="new Date()"
          :update-on-input="false"
          :popover="{ keepVisibleOnInput: false, placement: 'bottom' }"
          :input-props="{ class: 'channel__date-input' }"
          color="gray"
          is-dark
          is-required
          class="channel__date"
        />
      </client-only>
    </header>
    <nuxt-child :date="date" />
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import ChannelName from '@/components/ChannelName.vue'
import Divider from '@/components/Divider.vue'

export default {
  components: { Divider, ChannelName },
  computed: {
    date () {
      if (this.$route.params.date === undefined) {
        return new Date()
      }

      const [, year, month, day] = this.$route.params.date.match(/(\d+)-(\d{2})-(\d{2})/)
      return new Date(year, parseInt(month) - 1, day)
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
    }
  },
  methods: {
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
    align-items: center;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
    height: 4rem;
  }

  &__date {
    &-input {
      background: #202225;
      color: #dcddde;
      border: none;
      padding: 0.5rem;
      line-height: 1rem;
      border-radius: 4px;
      font-size: 1rem;
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
