<template>
  <span :class="['spoiler', { 'spoiler--visible': visible }]" @click.stop="visible = !visible">
    <span class="spoiler__content">
      <slot />
    </span>
  </span>
</template>

<script>
export default {
  name: 'Spoiler',
  data () {
    return {
      visible: false
    }
  }
}
</script>

<style lang="scss">
.spoiler {
  background: #202225;
  border-radius: 3px;
  transition: all .1s ease;
  cursor: pointer;

  &:hover {
    background: rgba(32, 34, 37, .8);
  }

  &__content {
    opacity: 0;
    transition: all .1s ease;
  }

  &--visible {
    background-color: hsla(0, 0%, 100%, .1);

    .spoiler__content {
      opacity: 1;
    }
  }
}
</style>
