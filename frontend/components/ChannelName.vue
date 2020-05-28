<template>
  <span class="channel-name">
    <HashIcon v-if="channel.type === 'TEXT'" size="1.25x" class="channel-name__icon" />
    <HashIcon v-else size="1x" class="channel-name__icon" />
    <span class="channel-name__text">
      {{ channel.name }}
    </span>
  </span>
</template>

<script>
import { HashIcon } from 'vue-feather-icons'

export default {
  name: 'ChannelName',
  components: { HashIcon },
  props: {
    channel: {
      type: Object,
      required: true
    }
  }
}
</script>

<style lang="scss">
.channel-name {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
  overflow: hidden;

  &__icon {
    margin-right: 0.25rem;
    transition: border-radius 0.2s ease-in-out;
    color: #72767d;
    flex-shrink: 0;
  }

  &__text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
