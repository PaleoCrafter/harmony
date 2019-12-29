<script>
import { parser, toHTML } from 'discord-markdown'
import renderNode from '@/components/message/message-renderer'

export default {
  name: 'Message',
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  computed: {
    annotatedVersions () {
      const properties = [
        this.message.editedAt !== null ? `<time datetime="${this.message.editedAt}">edited</time>` : undefined,
        this.message.deletedAt !== null ? `<time datetime="${this.message.deletedAt}">deleted</time>` : undefined
      ].filter(prop => prop !== undefined)

      return this.message.versions.map(version => ({
        ...version,
        parsed: parser(version.content),
        content: toHTML(
          version.content
        ) + (properties.length > 0 ? ` <span class="message__note">(${properties.join(', ')})</span>` : '')
      }))
    }
  },
  render (h) {
    const content = this.annotatedVersions[0].parsed

    return h(
      'article',
      {
        class: ['message', { 'message--deleted': this.message.deletedAt !== null }]
      },
      content.map(node => renderNode(node, h, this.message))
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

  &__note {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.625rem;
  }

  &--deleted {
    color: #f04747;
  }
}
</style>
