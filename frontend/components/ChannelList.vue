<template>
  <ul class="channel-list">
    <li v-for="channel in channels" :key="channel.id" class="channel-list__root-channel">
      <a
        v-if="isChannelActive(channel.id)"
        href="#"
        class="channel-list__item channel-list__item--active"
        @click.prevent="$store.commit('closeSidebar')"
      >
        <ChannelName :channel="channel" />
      </a>
      <nuxt-link v-else :to="`/servers/${server}/channels/${channel.id}`" class="channel-list__item">
        <ChannelName :channel="channel" />
      </nuxt-link>
    </li>
  </ul>
</template>

<script>
import ChannelName from '@/components/ChannelName.vue'

export default {
  name: 'ChannelList',
  components: { ChannelName },
  props: {
    server: {
      type: String,
      required: true
    },
    channels: {
      type: Array,
      required: true
    }
  },
  methods: {
    isChannelActive (id) {
      return this.$route.fullPath.startsWith(`/servers/${this.server}/channels/${id}`)
    }
  }
}
</script>

<style lang="scss">
.channel-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 240px;
  max-width: calc(100vw - 2rem);
  flex: 1;
  flex-basis: 0;
  overflow-y: auto;
  list-style-type: none;
  padding: 0;
  margin: 0;

  &:before {
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.3);
    font-size: 1rem;
    font-weight: 700;
    display: block;
    padding: 1rem 1rem 0 0.375rem;
    content: 'Archived Channels';
  }

  &__root-channel {
    padding: 0.0625rem 0.5rem;
  }

  &__item {
    display: flex;
    align-items: center;
    padding: 0.5rem 0.5rem;
    color: #72767d;
    text-decoration: none;
    border-radius: 0.25rem;

    &:hover {
      background: rgba(160, 210, 230, 0.05);
      color: white;
    }

    &--active {
      background: rgba(255, 255, 255, 0.05);
      color: white;
    }
  }
}
</style>
