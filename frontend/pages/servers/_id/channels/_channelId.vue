<template>
  <div class="channel">
    <header v-if="channel" class="channel__header">
      <ChannelName :channel="channel" />
    </header>
    <nuxt-child />
  </div>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'
import ChannelName from '@/components/ChannelName.vue'

export default {
  components: { ChannelName },
  apollo: {
    channel: {
      query: channelQuery,
      variables () {
        return {
          id: this.$route.params.channelId
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
  overflow: hidden;

  &__header {
    display: flex;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
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
