<template>
  <div class="search-results">
    <div class="search-results__header">
      <span v-if="!$apollo.loading" class="search-results__number">
        {{ formattedTotal }} {{ result.total !== 1 ? 'Results' : 'Results' }}
      </span>
      <span v-else class="search-results__number">
        Searching...
        <LoadingSpinner />
      </span>
      <ul class="search-results__orders">
        <li class="search-results__order search-results__order--active">
          Newest
        </li>
        <li class="search-results__order">
          Oldest
        </li>
        <li class="search-results__order">
          Most Relevant
        </li>
      </ul>
    </div>
    <ul ref="items" class="search-results__items">
      <template v-for="group in groupedEntries">
        <li class="search-results__item-header">
          <nuxt-link :to="`/servers/${$route.params.id}/channels/${group.channel.id}`">
            {{ `#${group.channel.name}` }}
          </nuxt-link>
          <hr>
        </li>
        <li v-for="entry in group.entries" class="search-results__item">
          <MessageGroup v-if="entry.previous" :group="entry.previous" class="search-results__item-context" relative-time />
          <MessageGroup :group="entry.message" class="search-results__item-message" relative-time>
            <nuxt-link slot="header" :to="`/messages/${entry.id}`" class="search-results__item-jump">
              Jump
            </nuxt-link>
          </MessageGroup>
          <MessageGroup v-if="entry.next" :group="entry.next" class="search-results__item-context" relative-time />
        </li>
      </template>
    </ul>
  </div>
</template>

<script>
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
  components: { LoadingSpinner, MessageGroup },
  props: {
    query: {
      type: String,
      required: true
    }
  },
  apollo: {
    result: {
      query: searchQuery,
      update: data => data.search,
      variables () {
        return {
          server: this.$route.params.id,
          parameters: {
            query: this.query
          }
        }
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
          id: rawEntry.message.id,
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
    background-color: #181a1d !important;;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track-piece {
    background-color: #26282c !important;
    border: 3px solid #2F3136 !important;;
    border-radius: 7px;
  }
}
</style>
