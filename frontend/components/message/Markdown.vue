<script>
import { parse, parseEmbed } from '@/components/message/message-parser.js'
import renderNode, { expandUnicodeEmojis } from '@/components/message/message-renderer'

export default {
  name: 'Markdown',
  props: {
    tag: {
      type: String,
      default: () => 'div'
    },
    attributes: {
      type: Object,
      default: () => {
      }
    },
    content: {
      type: String,
      required: true
    },
    embed: {
      type: Boolean,
      default: () => false
    },
    context: {
      type: Object,
      default: () => {
      }
    }
  },
  computed: {
    parsed () {
      return this.embed ? parseEmbed(this.content) : parse(this.content)
    }
  },
  render (h) {
    const content = this.parsed.flatMap(expandUnicodeEmojis)
    const emojisOnly = content.every(n => n.type === 'discordEmoji' || n.type === 'emoji' || (n.type === 'text' && n.content === ' '))
    const children = content.map(node => renderNode(node, h, this.context, emojisOnly))

    children.push(...(this.$slots.default || []))

    return h(this.tag, { class: 'markdown', attrs: this.attributes }, children)
  }
}
</script>

<style lang="scss">
.markdown {
  line-height: 1.57;
  white-space: pre-wrap;
  word-wrap: break-word;
  vertical-align: baseline;
  unicode-bidi: plaintext;
  color: #dcddde;

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
    font-size: 1em;
    font-family: Consolas, Andale Mono WT, Andale Mono, Lucida Console, Lucida Sans Typewriter, DejaVu Sans Mono, Bitstream Vera Sans Mono, Liberation Mono, Nimbus Mono L, Monaco, Courier New, Courier, monospace;
    text-indent: 0;
    border: none;
    white-space: pre-wrap;
    background: #2f3136;
    word-break: break-word;
  }
}
</style>
