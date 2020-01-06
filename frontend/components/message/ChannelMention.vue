<template>
  <span class="channel-mention">
    <template v-if="channel === null || channel === undefined">#{{ channelName }}</template>
    <nuxt-link v-else :to="`/servers/${channel.server}/channels/${id}`">#{{ channelName }}</nuxt-link>
  </span>
</template>

<script>
import channelQuery from '@/apollo/queries/channel.gql'

export default {
  name: 'ChannelMention',
  props: {
    id: {
      type: String,
      required: true
    }
  },
  computed: {
    channelName () {
      return this.channel?.name ?? this.id
    }
  },
  apollo: {
    channel: {
      query: channelQuery,
      variables () {
        return {
          id: this.id
        }
      }
    }
  }
}
</script>

<style lang="scss">
.channel-mention {
  color: #7289da;
  background-color: rgba(114, 137, 218, .1);
  transition: background-color 50ms ease-out, color 50ms ease-out;
  cursor: pointer;

  &:hover {
    color: white;
    background-color: rgba(114, 137, 218, 0.7);
  }

  a {
    color: #7289da !important;
    text-decoration: none !important;
    transition: color 50ms ease-out;

    &:hover {
      color: white !important;
    }
  }
}
</style>
