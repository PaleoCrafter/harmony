<template>
  <section class="message-reactors">
    <div class="message-reactors__reactions">
      <Reaction
        v-for="(reaction, index) in reactions"
        @click="selectReaction"
        :key="index"
        :reaction="reaction"
        :class="[{ 'message-reactors__reactions--active': reaction === selectedReaction }]"
      />
    </div>
    <ul class="message-reactors__list">
      <li v-for="reactor in (reactors || [])" :class="reactor.deletedAt !== null ? 'message-reactors__list--deleted' : undefined">
        <span v-if="reactor.user.nickname !== null" class="message-reactors__user-nickname">
          {{ reactor.user.nickname }}
        </span>
        <span class="message-reactors__user">
          {{ reactor.user.name }}
          <span class="message-reactors__user-discriminator">#{{ reactor.user.discriminator }}</span>
        </span>
        <time :datetime="chooseTimestamp(reactor)" :title="formatTimestamp(reactor)" class="message-reactors__timestamp">
          {{ formatTimestampRelative(reactor) }}
        </time>
      </li>
      <li v-if="$apollo.loading" class="message-reactors__loading">
        <LoadingSpinner />
      </li>
    </ul>
  </section>
</template>

<script>
import formatRelative from 'date-fns/formatRelative'
import Reaction from '@/components/message/Reaction.vue'
import reactorsQuery from '@/apollo/queries/reactors.gql'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  name: 'MessageReactors',
  components: { LoadingSpinner, Reaction },
  props: {
    message: {
      type: Object,
      required: true
    },
    reactions: {
      type: Array,
      required: true
    },
    initialSelection: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      selectedReaction: this.initialSelection
    }
  },
  apollo: {
    reactors: {
      query: reactorsQuery,
      variables () {
        return {
          message: this.message.id,
          type: this.selectedReaction.type,
          emoji: this.selectedReaction.emoji,
          emojiId: this.selectedReaction.emojiId
        }
      },
      fetchPolicy: 'cache-and-network'
    }
  },
  watch: {
    selectedReaction () {
      this.reactors = undefined
    }
  },
  methods: {
    selectReaction (reaction) {
      this.selectedReaction = reaction
    },
    chooseTimestamp (reactor) {
      return reactor.deletedAt ?? reactor.createdAt
    },
    formatTimestampRelative (reactor) {
      return formatRelative(Date.parse(this.chooseTimestamp(reactor)), new Date())
    },
    formatTimestamp (reactor) {
      const format = new Intl.DateTimeFormat(undefined, {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      })
      return format.format(Date.parse(this.chooseTimestamp(reactor)))
    }
  }
}
</script>

<style lang="scss">
.message-reactors {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex: 1;

  &__reactions {
    display: flex;
    overflow-y: visible;

    .reaction {
      background: none;
      font-size: 0.9rem;
      padding: 0.2rem 0.5rem;
    }

    &--active {
      background: rgba(255, 255, 255, 0.06) !important;
    }
  }

  &__list {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    overflow-y: scroll;
    flex: 1;
    list-style-type: none;
    margin: 0;
    padding: 0 0.5rem 0 0;

    li {
      display: flex;
      align-items: baseline;
      padding: 0.5rem 0.5rem;
      border-top: 1px solid rgba(255, 255, 255, 0.05);

      &:first-child {
        border-top: none;
      }
    }
  }

  &__user-nickname {
    font-weight: 600;
    margin-right: 0.5rem;

    & + .message-reactors__user {
      opacity: 0.5;
    }
  }

  &__user {
    display: flex;
    font-weight: 600;
    align-items: baseline;

    &-discriminator {
      font-weight: normal;
      font-size: 0.8rem;
      color: rgba(255, 255, 255, 0.4);
    }
  }

  &__loading {
    justify-content: center;
    padding: 1rem;
    border-top: none !important;
  }

  &__timestamp {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.75rem;
    margin-left: auto;
  }

  &__list--deleted {
    color: #f04747;

    .message-reactors__user, .message-reactors__user-nickname {
      color: #f04747;
    }

    .message-reactors__user-discriminator, .message-reactors__timestamp {
      color: #a14848;
    }
  }
}
</style>
