<template>
  <Emoji
    @click.native="$emit('click', reaction)"
    :url="emoji.url"
    :name="emoji.name"
    :class="['reaction', { 'reaction--deleted': reaction.count === 0 }]"
  >
    <span class="reaction__count">{{ reaction.count }}</span>
  </Emoji>
</template>

<script>
import { parse } from 'twemoji-parser'
import emojiNames from '@/components/message/emoji-names'

export default {
  name: 'Reaction',
  props: {
    reaction: {
      type: Object,
      required: true
    }
  },
  computed: {
    emoji () {
      if (this.reaction.type === 'CUSTOM') {
        return {
          url: `https://cdn.discordapp.com/emojis/${this.reaction.emojiId}.${this.reaction.emojiAnimated ? 'gif' : 'png'}?v=1`,
          name: `:${this.reaction.emoji}:`
        }
      } else {
        const [emoji] = parse(this.reaction.emoji)
        const name = emojiNames[emoji.text]

        return {
          url: emoji.url,
          name: name !== undefined ? `:${name}:` : emoji.text
        }
      }
    }
  }
}
</script>

<style lang="scss">
.reaction {
  font-size: 0.8rem;
  margin: 0.125rem;
  background: rgba(255, 255, 255, 0.06);
  padding: 0.125em 0.375em;
  border-radius: 0.25rem;

  &__count {
    font-size: 1rem;
    color: #72767d;
    margin-left: 0.4rem;
  }

  &:hover {
    cursor: pointer;

    .reaction__count {
      color: #dcddde;
    }
  }

  &--deleted {
    .reaction__count {
      color: #c75252;
    }

    &:hover .reaction__count {
      color: #f04747;
    }
  }
}
</style>
