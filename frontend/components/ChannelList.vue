<template>
  <div class="channel-list">
    <ul v-for="type in Object.keys(categories)" :key="type" :class="['channel-list__categories', `channel-list__categories--${type}`]">
      <li
        v-for="category in categories[type]"
        :key="category.name"
        :class="['channel-list__category', { 'channel-list__category--collapsed': category.collapsed }]"
      >
        <span @click="toggleCategory(type, category.name)" class="channel-list__category-header">
          <ChevronRightIcon v-if="category.collapsed" size="1x" class="channel-list__category-chevron" />
          <ChevronDownIcon v-else size="1x" class="channel-list__category-chevron" />
          {{ category.name }}
        </span>
        <ul class="channel-list__channels">
          <li v-for="channel in category.channels" :key="channel.id">
            <a
              v-if="isChannelActive(channel.id)"
              @click.prevent="$store.commit('closeSidebar')"
              href="#"
              class="channel-list__item channel-list__item--active"
            >
              <ChannelName :channel="channel" />
            </a>
            <nuxt-link v-else :to="`/servers/${server}/channels/${channel.id}`" class="channel-list__item">
              <ChannelName :channel="channel" />
            </nuxt-link>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<script>
import { ChevronDownIcon, ChevronRightIcon } from 'vue-feather-icons'
import { mapState } from 'vuex'
import ChannelName from '@/components/ChannelName.vue'

export default {
  name: 'ChannelList',
  components: { ChannelName, ChevronDownIcon, ChevronRightIcon },
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
  computed: {
    ...mapState(['collapsedCategories']),
    categories () {
      const categories = { active: {}, deleted: {} }
      const result = { active: [], deleted: [] }

      this.channels.forEach((channel) => {
        const type = channel.deletedAt !== null ? 'deleted' : 'active'
        let category = categories[type][channel.category]
        if (category === undefined) {
          category = { name: channel.category, collapsed: this.isCollapsed(type, channel.category), channels: [] }
          result[type].push(category)
          categories[type][channel.category] = category
        }
        category.channels.push(channel)
      })

      return result
    }
  },
  methods: {
    toggleCategory (type, category) {
      this.$store.commit('toggleCategory', { server: this.server, type, category })
    },
    isCollapsed (type, category) {
      const serverTypes = this.collapsedCategories[this.server] ?? {}
      const categories = serverTypes[type] ?? {}

      return categories[category] ?? false
    },
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

  &__categories {
    display: flex;
    flex-direction: column;
    align-items: stretch;
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
    }

    &--active:before {
      content: 'Channels';
    }

    &--deleted:before {
      content: 'Deleted Channels';
    }
  }

  &__category {
    &-header {
      display: flex;
      align-items: center;
      padding: 0.5rem 0.25rem;
      text-transform: uppercase;
      color: rgba(255, 255, 255, 0.5);
      font-size: 0.8rem;
      font-weight: 600;

      &:hover {
        color: white;
        cursor: pointer;
      }
    }

    &-chevron {
      margin-right: 0.125rem;
    }

    &--collapsed {
      .channel-list__channels > li {
        padding-top: 0;
        padding-bottom: 0;
      }

      .channel-list__item {
        display: none !important;

        &--active {
          display: flex !important;
          margin: 0.0625rem 0;
        }
      }
    }
  }

  &__channels {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    list-style-type: none;
    padding: 0;
    margin: 0;

    & > li {
      padding: 0.0625rem 0.5rem;
    }
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
