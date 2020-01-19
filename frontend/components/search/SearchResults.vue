<template>
  <div class="search-results">
    <div class="search-results__header">
      <span v-if="!$apollo.loading" class="search-results__number">
        {{ formattedTotal }} {{ result.total !== 1 ? 'Results' : 'Results' }}
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
        <li>{{ group.channel.name }}</li>
        <li v-for="entry in group.entries">
          <MessageGroup :group="entry.message" />
        </li>
      </template>
    </ul>
  </div>
</template>

<script>
import searchQuery from '@/apollo/queries/search.gql'
import MessageGroup from '@/components/MessageGroup.vue'

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
  components: { MessageGroup },
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
    padding: 0;
    list-style-type: none;
    flex: 1;
    overflow-y: scroll;
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
