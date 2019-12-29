<template>
  <span :class="['spoiler', { 'spoiler--visible': visible }]" @click="visible = !visible">
    <span class="spoiler__content">
      <slot />
    </span>
  </span>
</template>

<script>
export default {
  name: 'ChannelMention',
  props: {
    id: {
      type: String,
      required: true
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
}
</style>
