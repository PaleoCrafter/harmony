<template>
  <div class="server">
    <ChannelList v-if="server" :server="$route.params.id" :channels="server.channels" />
    <nuxt-child v-if="server" />
    <div v-else class="server__error">
      <AlertCircleIcon class="server__error-icon" size="4x" />
      The requested server does not exist!
    </div>
  </div>
</template>

<script>
import { AlertCircleIcon } from 'vue-feather-icons'
import channelsQuery from '@/apollo/queries/server-channels.gql'
import ChannelList from '@/components/ChannelList.vue'

export default {
  components: { ChannelList, AlertCircleIcon },
  apollo: {
    server: {
      query: channelsQuery,
      variables () {
        return {
          id: this.$route.params.id
        }
      },
      result (result) {
        const channels = result.data?.server?.channels
        if (channels && this.$route.params.channelId === undefined) {
          this.$router.replace(`/servers/${this.$route.params.id}/channels/${channels[0].id}`)
        }
      }
    }
  }
}
</script>

<style lang="scss">
.server {
  display: flex;
  align-items: stretch;
  flex-grow: 1;

  &__error {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-grow: 1;
    flex-direction: column;
    font-size: 2rem;

    &-icon {
      opacity: 0.5;
    }
  }
}
</style>
