<template>
  <ul class="channel-list">
    <li v-for="channel in channels" :key="channel.id">
      <nuxt-link :to="`/servers/${server}/channels/${channel.id}`" class="channel-list__item">
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
  }
}
</script>

<style lang="scss">
.channel-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  list-style-type: none;
  padding: 0;
  margin: 0;
  width: 240px;

  li {
    padding: 0.0625rem 0.5rem;
  }

  &:before {
    content: 'Channels';
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.5);
    font-size: 0.8rem;
    font-weight: 600;
    display: block;
    padding: 1rem 1rem 0.5rem;
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

    &.nuxt-link-active {
      background: rgba(255, 255, 255, 0.05);
      color: white;
    }
  }
}
</style>
