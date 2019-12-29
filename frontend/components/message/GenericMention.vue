<template>
  <span class="generic-mention">{{ text }}</span>
</template>

<script>
export default {
  name: 'GenericMention',
  props: {
    text: {
      type: String,
      required: true
    }
  }
}
</script>

<style lang="scss">
.generic-mention {
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
