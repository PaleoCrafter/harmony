<template>
  <div class="search-results">
    <div class="search-results__header">
      <span v-if="!$apollo.loading && result" class="search-results__number">
        {{ formattedTotal }} {{ result.total !== 1 ? 'Results' : 'Results' }}
      </span>
      <span v-else class="search-results__loading">
        Searching...
        <LoadingSpinner />
      </span>
      <ul class="search-results__orders">
        <li
          v-for="entry in sortOptions"
          :key="entry.order"
          :class="['search-results__order', { 'search-results__order--active': sortOrder === entry.order }]"
          @click="sortOrder = entry.order"
        >
          {{ entry.label }}
        </li>
      </ul>
    </div>
    <div v-if="result && result.error" class="search-results__error">
      Could not complete search: {{ result.error }}
    </div>
    <ul ref="items" class="search-results__items">
      <template v-for="group in groupedEntries">
        <li :key="`${group.channel.id}-${group.entries[0].id}`" class="search-results__item-header">
          <nuxt-link :to="`/servers/${$route.params.id}/channels/${group.channel.id}`">
            {{ `#${group.channel.name}` }}
          </nuxt-link>
          <hr>
        </li>
        <li v-for="entry in group.entries" :key="entry.id" class="search-results__item">
          <MessageGroup
            v-if="entry.previous"
            :group="entry.previous"
            class="search-results__item-context search-results__item-context--previous"
            relative-time
          />
          <MessageGroup :group="entry.message" class="search-results__item-message" relative-time>
            <nuxt-link slot="header" :to="`/messages/${entry.id}`" class="search-results__item-jump">
              Jump
            </nuxt-link>
          </MessageGroup>
          <MessageGroup
            v-if="entry.next"
            :group="entry.next"
            class="search-results__item-context search-results__item-context--next"
            relative-time
          />
        </li>
      </template>
    </ul>
    <nav v-if="result" class="search-results__pagination">
      <button :disabled="page === 0" class="search-results__pagination-button" aria-label="Previous page" @click="changePage(-1)">
        <ChevronLeftIcon size="1x" stroke-width="3" />
      </button>
      Page {{ page + 1 }} of {{ result.totalPages }}
      <button
        :disabled="page === result.totalPages - 1"
        class="search-results__pagination-button"
        aria-label="Next page"
        @click="changePage(1)"
      >
        <ChevronRightIcon size="1x" stroke-width="3" />
      </button>
    </nav>
  </div>
</template>

<script>
import { ChevronLeftIcon, ChevronRightIcon } from 'vue-feather-icons'
import searchQuery from '@/apollo/queries/search.gql'
import MessageGroup from '@/components/MessageGroup.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

function prepareMessage (message) {
  if (message === null) {
    return null
  }

  const clone = { ...message }
  clone.createdAt = new Date(clone.createdAt)

  return { author: clone.author, firstTimestamp: clone.createdAt, messages: [clone] }
}

export default {
  name: 'SearchResults',
  components: { LoadingSpinner, MessageGroup, ChevronLeftIcon, ChevronRightIcon },
  props: {
    query: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      sortOrder: 'DESCENDING',
      sortOptions: [
        {
          label: 'Newest',
          order: 'DESCENDING'
        },
        {
          label: 'Oldest',
          order: 'ASCENDING'
        },
        {
          label: 'Most Relevant',
          order: null
        }
      ],
      page: 0
    }
  },
  apollo: {
    result: {
      query: searchQuery,
      update: (data) => {
        return data.search
      },
      variables () {
        return {
          server: this.$route.params.id,
          parameters: {
            query: this.query,
            sort: this.sortOrder,
            page: this.page
          }
        }
      },
      fetchPolicy: 'network-only',
      result () {
        this.$refs.items.scrollTop = 0
      }
    }
  },
  computed: {
    formattedTotal () {
      if (!this.result) {
        return null
      }

      const formatter = new Intl.NumberFormat(undefined, { useGrouping: true })

      return formatter.format(this.result.total)
    },
    groupedEntries () {
      if (!this.result) {
        return []
      }

      const groups = []

      let currentGroup = null
      let lastEntry = null

      this.result.entries.forEach((rawEntry) => {
        const entry = {
          id: rawEntry.message.ref,
          channel: rawEntry.channel,
          previous: prepareMessage(rawEntry.previous),
          message: prepareMessage(rawEntry.message),
          next: prepareMessage(rawEntry.next)
        }

        if (
          (currentGroup === null && lastEntry === null) ||
          rawEntry.channel.id !== lastEntry.channel.id
        ) {
          currentGroup = {
            channel: rawEntry.channel,
            entries: []
          }
          groups.push(currentGroup)
        }

        currentGroup.entries.push(entry)
        lastEntry = entry
      })

      return groups
    }
  },
  methods: {
    changePage (direction) {
      this.page = Math.max(0, Math.min(this.page + direction, this.result.totalPages - 1))
    }
  }
}
</script>

<style lang="scss">
.search-results {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    font-size: 0.9rem;
    color: #72767d;
    padding: 0 1rem;
    border-bottom: 1px solid rgba(0, 0, 0, 0.2);
  }

  &__loading {
    display: flex;
    align-items: center;

    .loading-spinner {
      margin-left: 0.5rem;
    }
  }

  &__orders {
    display: flex;
    align-items: stretch;
    padding: 0;
    margin-left: auto;
    list-style-type: none;
  }

  &__order {
    padding: 1rem 0.25rem;
    margin-right: 1rem;
    border-bottom: 2px solid transparent;

    &:last-child {
      margin-right: 0;
    }

    &:hover {
      cursor: pointer;
      border-bottom-color: #72767d;
    }

    &--active, &--active:hover {
      color: white;
      border-bottom-color: white;
    }
  }

  &__items {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    padding: 0 0.5rem;
    list-style-type: none;
    flex: 1;
    overflow-y: scroll;
    overflow-x: hidden;
  }

  &__item {
    padding: 0.5rem 0;

    &-header {
      display: flex;
      align-items: center;
      padding: 0.25rem 0;
      font-size: 0.9rem;
      font-weight: 600;

      a {
        color: inherit;
        text-decoration: none;

        &:hover, &:active, &:focus {
          text-decoration: underline;
        }
      }

      hr {
        flex: 1;
        margin-left: 0.5rem;
        border: none;
        border-top: 1px solid rgba(255, 255, 255, 0.05);
      }
    }

    .message-group {
      border: none !important;
      padding: 0.7rem 0.5rem !important;
    }

    &-message.message-group {
      border: 2px solid rgba(28, 36, 43, .6) !important;
      border-radius: 4px;
      background-color: #36393f;
      box-shadow: 0 0 10px 6px #2f3136;
    }

    &-context {
      opacity: 0.3;
      max-height: 48px;
      overflow: hidden;
      pointer-events: none;
      position: relative;

      &--next {
        &:after {
          position: absolute;
          content: '';
          height: 10px;
          bottom: 0;
          left: 0;
          right: 0;
          background: linear-gradient(to top, #2f3136, rgba(47, 49, 54, 0));
          z-index: 2;
        }
      }
    }

    &-jump {
      display: none;
      align-items: center;
      justify-content: center;
      position: absolute;
      right: 0.5rem;
      top: 0.5rem;
      background: #535559;
      color: rgba(255, 255, 255, 0.6);
      font-size: 0.7rem;
      border-radius: 0.15rem;
      padding: 0.2rem 0.6rem;
      text-decoration: none;

      &:hover, &:active, &:focus {
        color: white;
      }

      &:active {
        margin-top: 1px;
      }
    }

    &-message:hover .search-results__item-jump {
      display: flex;
    }
  }

  &__error {
    color: #f04747;
    padding: 0.5rem;
    text-align: center;
  }

  &__pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.5rem;
    color: #dcddde;
    font-size: 0.9rem;

    &-button {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #dcddde;
      border: 1px solid rgba(255, 255, 255, 0.2);
      background: none;
      border-radius: 3px;
      padding: 2px 2px;
      margin: 0 2rem;
      cursor: pointer;
      outline: none;
      font-size: 1rem;

      &:not(:disabled):hover, &:focus {
        color: white;
        border-color: rgba(255, 255, 255, 0.5);
      }

      &:disabled {
        cursor: not-allowed;
        opacity: 0.5;
      }
    }
  }

  ::-webkit-scrollbar-track {
    border-width: initial;
    background-color: transparent;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track, ::-webkit-scrollbar-thumb {
    background-clip: padding-box;
    border-width: 3px;
    border-style: solid;
    border-radius: 7px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #181a1d !important;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track-piece {
    background-color: #26282c !important;
    border: 3px solid #2F3136 !important;
    border-radius: 7px;
  }
}
</style>
