<template>
  <div class="server">
    <div v-if="server" class="server__sidebar">
      <header class="server__header">
        {{ server.name }}
      </header>
      <ChannelList :server="$route.params.id" :channels="server.channels" />
    </div>
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

  &__sidebar {
    background: #2F3136;
  }

  &__header {
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
    font-weight: 600;
  }

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
