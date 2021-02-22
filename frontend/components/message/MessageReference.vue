<script>
import detailsQuery from '~/apollo/queries/message-details.gql'
import UserName from '~/components/UserName.vue'
import LoadingSpinner from '~/components/LoadingSpinner.vue'
import Markdown from '~/components/message/Markdown.vue'
import BotTag from '~/components/BotTag.vue'

export default {
  name: 'MessageReference',
  components: { LoadingSpinner, UserName, Markdown, BotTag },
  props: {
    referencingMessage: {
      type: Object,
      required: true
    }
  },
  apollo: {
    referenceData: {
      fetchPolicy: 'cache-and-network',
      query: detailsQuery,
      variables () {
        return {
          message: this.referencingMessage.ref
        }
      },
      update: data => data.messageDetails.referencedMessage
    }
  },
  computed: {
    message () {
      return this.referenceData?.message
    }
  },
  render (h) {
    if (this.referenceData === undefined) {
      return h('div', { class: 'message-reference' }, [h(LoadingSpinner)])
    } else if (this.referenceData === null) {
      return h('div', { class: 'message-reference' }, [h('em', ['Original message was not archived.'])])
    } else if (this.message === undefined || this.message === null) {
      return h('div', { class: 'message-reference' }, [h('em', ['Original message was deleted.'])])
    }

    const slotContent = []
    const properties = [
      this.message.editedAt !== null ? h('time', { attrs: { datetime: this.message.editedAt } }, ['edited']) : undefined,
      this.message.deletedAt !== null ? h('time', { attrs: { datetime: this.message.deletedAt } }, ['deleted']) : undefined
    ].filter(prop => prop !== undefined)
      .reduce((acc, prop, index) => [...acc, index === 0 ? '(' : ', ', prop], [])

    if (properties.length > 0) {
      slotContent.push(
        ' ',
        h('span', { class: 'message-reference__note' }, [...properties, ')'])
      )
    }

    const prefix = []
    if (this.message.author.bot) {
      prefix.push(h(BotTag, this.message.isCrosspost ? ['Server'] : undefined))
    }

    return h(
      'div',
      { class: ['message-reference', { 'message-reference--deleted': this.message.deletedAt !== null }] },
      [
        ...prefix,
        h(
          UserName,
          {
            props: {
              server: this.message.server,
              user: this.message.author,
              useRoleColor: true
            }
          }
        ),
        h(
          Markdown,
          {
            props: {
              content: this.message.versions[0].content,
              context: this.message,
              embed: this.message.author.discriminator === 'HOOK'
            },
            class: 'message-reference__content'
          },
          slotContent
        )
      ]
    )
  }
}
</script>

<style lang="scss">
.message-reference {
  opacity: 0.8;
  font-size: 0.9em;

  &:hover {
    opacity: 1;
  }

  &--deleted {
    .message-reference__content, .message-reference__note {
      color: #f04747;
    }
  }

  & > .bot-tag {
    margin-right: 0.25rem;
  }

  &__content {
    display: inline-block;
    margin-left: 0.5rem;
  }
}
</style>
