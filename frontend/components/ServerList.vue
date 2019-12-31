<template>
  <ul class="server-list">
    <li v-for="server in servers" :key="server.id">
      <a
        v-if="isServerActive(server.id)"
        @click.prevent="switchTab"
        href="#"
        class="server-list__item server-list__item--active"
      >
        <img :src="server.iconUrl" :alt="server.name" class="server-list__icon">
        {{ server.name }}
      </a>
      <nuxt-link v-else :to="`/servers/${server.id}`" class="server-list__item">
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
    },
    index: {
      type: Boolean,
      required: true
    }
  },
  methods: {
    switchTab () {
      if (this.index || window?.matchMedia('(min-width: 768px)')?.matches) {
        this.$store.commit('closeSidebar')
      }
      this.$store.commit('setSidebarTab', 'channels')
    },
    isServerActive (id) {
      return this.$route.fullPath.startsWith(`/servers/${id}`)
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
  max-width: calc(100vw - 2rem);
  flex: 1;
  flex-basis: 0;
  overflow-y: auto;

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

    &:hover, &--active {
      background: rgba(255, 255, 255, 0.05);

      .server-list__icon {
        border-radius: 1rem;
      }
    }
  }
}
</style>
