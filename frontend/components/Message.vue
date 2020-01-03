<script>
import embedsQuery from '@/apollo/queries/embeds.gql'
import Embed from '@/components/message/Embed.vue'
import Markdown from '@/components/message/Markdown.vue'

export default {
  name: 'Message',
  components: { Embed, Markdown },
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  apollo: {
    embeds: {
      query: embedsQuery,
      skip () {
        return !this.message.hasEmbeds
      },
      variables () {
        return {
          message: this.message.id
        }
      }
    }
  },
  render (h) {
    const slotContent = []
    const properties = [
      this.message.editedAt !== null ? h('time', { attrs: { datetime: this.message.editedAt } }, ['edited']) : undefined,
      this.message.deletedAt !== null ? h('time', { attrs: { datetime: this.message.deletedAt } }, ['deleted']) : undefined
    ].filter(prop => prop !== undefined)
      .reduce((acc, prop, index) => [...acc, index === 0 ? '(' : ', ', prop], [])

    if (properties.length > 0) {
      slotContent.push(
        ' ',
        h('span', { class: 'message__note' }, [...properties, ')'])
      )
    }

    return h(
      'article',
      {
        class: ['message', { 'message--deleted': this.message.deletedAt !== null }]
      },
      [
        h(Markdown, { props: { content: this.message.versions[0].content }, class: 'message__content' }, slotContent),
        h('div', { class: 'message__embeds' }, (this.embeds || []).map(embed => h(Embed, { props: { embed } })))
      ]
    )
  }
}
</script>

<style lang="scss">
.message {
  color: #dcddde;

  a {
    color: #00b0f4;
    text-decoration: none;

    &:hover, &:focus {
      text-decoration: underline;
    }
  }

  &__note {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.625rem;
  }

  &__embeds {
    padding: 0.5rem 0;
  }

  &--deleted {
    color: #f04747;
  }
}
</style>
