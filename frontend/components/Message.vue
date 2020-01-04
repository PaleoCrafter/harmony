<script>
import detailsQuery from '@/apollo/queries/details.gql'
import Embed from '@/components/message/Embed.vue'
import Markdown from '@/components/message/Markdown.vue'
import Attachment from '@/components/message/Attachment.vue'

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
    details: {
      query: detailsQuery,
      skip () {
        return !this.message.hasEmbeds && !this.message.hasAttachments
      },
      variables () {
        return {
          message: this.message.id
        }
      },
      update: data => data.messageDetails
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

    const details = this.details ?? {}
    const embeds = details.embeds || []
    const attachments = details.attachments || []

    return h(
      'article',
      {
        class: ['message', { 'message--deleted': this.message.deletedAt !== null }]
      },
      [
        h(
          Markdown,
          {
            props: { content: this.message.versions[0].content, context: this.message },
            class: 'message__content'
          },
          slotContent
        ),
        embeds.length > 0
          ? h('div', { class: 'message__embeds' }, embeds.map(embed => h(Embed, { props: { embed } })))
          : undefined,
        attachments.length > 0
          ? h('div', { class: 'message__attachments' }, attachments.map(attachment => h(Attachment, { props: { attachment } })))
          : undefined
      ]
    )
  }
}
</script>

<style lang="scss">
.message {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  color: #dcddde;
  flex-basis: 0;

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

  &__embeds, &__attachments {
    padding: 0.5rem 0;
    flex-basis: 0;

    @media (max-width: 768px) {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      max-width: 100%;
    }
  }

  &__attachments {
    display: flex;
    flex-direction: column;
  }

  &--deleted {
    color: #f04747;
  }
}
</style>
