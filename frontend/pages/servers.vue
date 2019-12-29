<template>
  <div class="index">
    <div class="index__sidebar">
      <ServerList :servers="servers" />
      <UserPanel :user="identity" />
    </div>
    <nuxt-child />
  </div>
</template>

<script>
import identityQuery from '@/apollo/queries/identity.gql'
import serversQuery from '@/apollo/queries/servers.gql'
import ServerList from '~/components/ServerList.vue'
import UserPanel from '@/components/UserPanel.vue'

export default {
  components: { UserPanel, ServerList },
  apollo: {
    identity: {
      query: identityQuery
    },
    servers: {
      query: serversQuery
    }
  }
}
</script>

<style lang="scss">
.index {
  display: flex;
  align-items: stretch;
  flex-grow: 1;

  &__sidebar {
    display: flex;
    flex-direction: column;
    background: #202225;
  }
}
</style>
