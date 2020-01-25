<script>
import { ClockIcon } from 'vue-feather-icons'
import { mapGetters, mapState } from 'vuex'
import detailsQuery from '@/apollo/queries/message-details.gql'
import Embed from '@/components/message/Embed.vue'
import Markdown from '@/components/message/Markdown.vue'
import Attachment from '@/components/message/Attachment.vue'
import Reaction from '@/components/message/Reaction.vue'
import MessageHistory from '@/components/MessageHistory.vue'
import MessageReactors from '@/components/MessageReactors.vue'

export default {
  name: 'Message',
  components: { Embed, Attachment, Markdown, ClockIcon, MessageHistory, MessageReactors },
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      historyActive: false,
      selectedReaction: null
    }
  },
  computed: {
    ...mapState(['highlightedMessage']),
    ...mapGetters(['modalOpen'])
  },
  apollo: {
    details: {
      query: detailsQuery,
      skip () {
        return !this.message.hasEmbeds && !this.message.hasAttachments && !this.message.hasReactions
      },
      variables () {
        return {
          message: this.message.ref
        }
      },
      update: data => data.messageDetails
    }
  },
  watch: {
    modalOpen (newState) {
      if (newState === false) {
        this.historyActive = false
        this.selectedReaction = null
      }
    }
  },
  methods: {
    openHistory () {
      this.$store.commit('openModal', { title: 'Message History' })
      this.historyActive = true
    },
    openReactors (reaction) {
      this.$store.commit('openModal', { title: 'Reactions' })
      this.selectedReaction = reaction
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
    const reactions = details.reactions || []
    const embeds = details.embeds || []
    const attachments = details.attachments || []

    return h(
      'article',
      {
        class: [
          'message',
          {
            'message--deleted': this.message.deletedAt !== null,
            'message--highlighted': this.highlightedMessage === this.message.ref
          }
        ]
      },
      [
        h(
          'span',
          {
            class: 'message__anchor',
            attrs: {
              'data-message-id': this.message.ref
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
            class: 'message__content'
          },
          slotContent
        ),
        this.message.versions.length > 1 ? h(
          'div',
          { class: 'message__actions' },
          [
            h(
              'button',
              {
                class: 'message__history-button',
                on: { click: this.openHistory },
                attrs: { 'aria-label': 'Message History', title: 'Message History' }
              },
              [h(ClockIcon, { props: { size: '1x' } })]
            )
          ]
        ) : undefined,
        embeds.length > 0
          ? h('div', { class: 'message__embeds' }, embeds.map(embed => h(Embed, { props: { embed, message: this.message } })))
          : undefined,
        attachments.length > 0
          ? h('div', { class: 'message__attachments' }, attachments.map(attachment => h(Attachment, { props: { attachment } })))
          : undefined,
        reactions.length > 0 ? h(
          'div',
          { class: 'message__reactions' },
          reactions.map(reaction => h(Reaction, { props: { reaction }, on: { click: this.openReactors } }))
        ) : undefined,
        this.historyActive
          ? h('portal', { props: { to: 'modal' } }, [h(MessageHistory, { props: { message: this.message } })])
          : undefined,
        this.selectedReaction !== null ? h(
          'portal',
          { props: { to: 'modal' } },
          [h(MessageReactors, { props: { message: this.message, reactions, initialSelection: this.selectedReaction } })]
        ) : undefined
      ]
    )
  }
}
</script>

<style lang="scss">
.message {
  display: grid;
  align-items: stretch;
  color: #dcddde;
  flex-basis: 0;
  grid-template-columns: 1fr minmax(0, auto);
  grid-auto-rows: auto;
  grid-column-gap: 0.25rem;
  position: relative;

  &__anchor {
    position: absolute;
    top: calc(-50vh + 50%);
  }

  &__content {
    grid-column: 1;
    grid-row: 1;
  }

  &__note {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.625rem;
  }

  &__reactions, &__embeds, &__attachments {
    padding: 0.25rem 0;
    flex-basis: 0;

    @media (max-width: 768px) {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      max-width: 100%;
    }
  }

  &__embeds {
    grid-column: 1/span 2;
    grid-row: 2;
  }

  &__attachments {
    display: flex;
    flex-direction: column;
    grid-column: 1/span 2;
    grid-row: 3;
  }

  &__reactions {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    grid-column: 1/span 2;
    grid-row: 4;

    @media (max-width: 768px) {
      flex-direction: row;
    }
  }

  &__history-button {
    font-size: 1.25rem;
    border: none;
    background: none;
    color: rgba(255, 255, 255, 0.6);
    cursor: pointer;
    line-height: 0;
    outline: none;
    grid-row: 1;
    grid-column: 2;
    padding: 0;

    &:hover {
      color: white;
    }
  }

  &--deleted {
    .message__content, .message__note {
      color: #f04747;
    }
  }
}
</style>
