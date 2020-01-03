<script>
import { parser } from 'discord-markdown'
import renderNode, { expandUnicodeEmojis } from '@/components/message/message-renderer'

export default {
  name: 'Message',
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  computed: {
    parsedVersions () {
      return this.message.versions.map(version => ({
        ...version,
        content: parser(version.content)
      }))
    }
  },
  render (h) {
    const content = this.parsedVersions[0].content.flatMap(expandUnicodeEmojis)
    const emojisOnly = content.every(n => n.type === 'discordEmoji' || n.type === 'emoji' || (n.type === 'text' && n.content === ' '))
    const children = content.map(node => renderNode(node, h, this.message, emojisOnly))

    const properties = [
      this.message.editedAt !== null ? h('time', { attrs: { datetime: this.message.editedAt } }, ['edited']) : undefined,
      this.message.deletedAt !== null ? h('time', { attrs: { datetime: this.message.deletedAt } }, ['deleted']) : undefined
    ].filter(prop => prop !== undefined)
      .reduce((acc, prop, index) => [...acc, index === 0 ? '(' : ', ', prop], [])

    if (properties.length > 0) {
      children.push(
        ' ',
        h('span', { class: 'message__note' }, [...properties, ')'])
      )
    }

    return h(
      'article',
      {
        class: ['message', { 'message--deleted': this.message.deletedAt !== null }]
      },
      children
    )
  }
}
</script>

<style lang="scss">
.message {
  line-height: 1.57;
  white-space: pre-wrap;
  vertical-align: baseline;
  unicode-bidi: plaintext;
  color: #dcddde;

  a {
    color: #00b0f4;
    text-decoration: none;

    &:hover, &:focus {
      text-decoration: underline;
    }
  }

  blockquote {
    position: relative;
    padding: 0 0.5rem 0 1rem;
    margin: 0.5rem 0;
    max-width: 90%;

    &:before {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      content: '';
      background-color: #4f545c;
      border-radius: 4px;
      width: 4px;
    }
  }

  &__code {
    width: auto;
    height: auto;
    padding: .2em;
    margin: -.2em 0;
    border-radius: 3px;
    font-size: 1rem;
    font-family: Consolas, Andale Mono WT, Andale Mono, Lucida Console, Lucida Sans Typewriter, DejaVu Sans Mono, Bitstream Vera Sans Mono, Liberation Mono, Nimbus Mono L, Monaco, Courier New, Courier, monospace;
    text-indent: 0;
    border: none;
    white-space: pre-wrap;
    background: #2f3136;
  }

  &__note {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.625rem;
  }

  &--deleted {
    color: #f04747;
  }
}
</style>
