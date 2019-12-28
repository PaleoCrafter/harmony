<template>
  <ul class="server-list">
    <li v-for="server in servers" :key="server.id">
      <nuxt-link :to="`/servers/${server.id}`" class="server-list__item">
        <img :src="server.iconUrl" :alt="server.name" class="server-list__icon">
        {{ server.name }}
      </nuxt-link>
    </li>
  </ul>
</template>

<script>
export default {
  name: 'ServerList',
  props: {
    servers: {
      type: Array,
      required: true
    }
  }
}
</script>

<style lang="scss">
.server-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  list-style-type: none;
  padding: 0;
  margin: 0;
  width: 240px;

  li {
    padding: 0.5rem 0;
  }

  &:before {
    content: 'Servers';
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.5);
    font-size: 0.8rem;
    font-weight: 600;
    display: block;
    padding: 1rem 1rem 0;
  }

  &__icon {
    width: 3rem;
    border-radius: 1.5rem;
    margin-right: 0.5rem;
    transition: border-radius 0.2s ease-in-out;
  }

  &__item {
    display: flex;
    align-items: center;
    padding: 0.5rem 1rem;
    color: white;
    text-decoration: none;
    font-weight: 600;

    &:hover, &.nuxt-link-active {
      background: rgba(255, 255, 255, 0.05);

      .server-list__icon {
        border-radius: 1rem;
      }
    }
  }
}
</style>
