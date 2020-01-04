import { parse } from 'twemoji-parser'
import Spoiler from '@/components/message/Spoiler.vue'
import UserMention from '@/components/message/UserMention.vue'
import ChannelMention from '@/components/message/ChannelMention.vue'
import GenericMention from '@/components/message/GenericMention.vue'
import RoleMention from '@/components/message/RoleMention.vue'
import CodeBlock from '@/components/message/CodeBlock.vue'
import Emoji from '@/components/message/Emoji.vue'
import emojiNames from '@/components/message/emoji-names'

export function expandUnicodeEmojis (node) {
  switch (node.type) {
    case 'strong':
    case 'em':
    case 'strike':
    case 'u':
    case 'url':
    case 'spoiler':
    case 'blockQuote':
      return { ...node, content: node.content.flatMap(child => expandUnicodeEmojis(child)) }
    case 'text':
      const emojis = parse(node.content)
      return emojis.reduce(
        ({ nodes, offset }, emoji) => {
          const emojiStart = emoji.indices[0] - offset
          const emojiEnd = emoji.indices[1] - offset
          const text = nodes.pop().content
          const leftText = text.substring(0, emojiStart)
          const rightText = text.substring(emojiEnd)
          const left = leftText.length > 0 ? [{ type: 'text', content: leftText }] : []
          const right = rightText.length > 0 ? [{ type: 'text', content: rightText }] : []
          const name = emojiNames[emoji.text]
          const emojiNode = { type: 'emoji', url: emoji.url, name: name !== undefined ? `:${name}:` : emoji.text }

          return { nodes: [...nodes, ...left, emojiNode, ...right], offset: offset + emojiEnd }
        },
        { nodes: [node], offset: 0 }
      ).nodes
  }

  return [node]
}

export default function renderNode (node, h, message, emojisOnly) {
  switch (node.type) {
    case 'strong':
    case 'em':
    case 'strike':
    case 'u':
      return h(node.type, node.content.map(child => renderNode(child, h, message, false)))
    case 'link':
    case 'url':
      return h(
        'a',
        {
          attrs: {
            href: node.target,
            target: '_blank',
            rel: 'noopener'
          }
        },
        node.content.map(child => renderNode(child, h, message, false))
      )
    case 'spoiler':
      return h(Spoiler, node.content.map(child => renderNode(child, h, message, false)))
    case 'blockQuote':
      return h('blockquote', node.content.map(child => renderNode(child, h, message, false)))
    case 'inlineCode':
      return h('code', { class: 'markdown__code' }, [node.content])
    case 'codeBlock':
      return h(CodeBlock, { props: { language: node.lang } }, [node.content])
    case 'discordUser':
      return h(UserMention, { props: { id: node.id.toString(), server: message.server } })
    case 'discordChannel':
      return h(ChannelMention, { props: { id: node.id.toString() } })
    case 'discordRole':
      return h(RoleMention, { props: { id: node.id.toString(), server: message.server } })
    case 'discordEveryone':
      return h(GenericMention, { props: { text: '@everyone' } })
    case 'discordHere':
      return h(GenericMention, { props: { text: '@here' } })
    case 'discordEmoji':
      return h(Emoji, {
        props: {
          url: `https://cdn.discordapp.com/emojis/${node.id}.${node.animated ? 'gif' : 'png'}?v=1`,
          name: `:${node.name}:`,
          large: emojisOnly
        }
      })
    case 'emoji':
      return h(Emoji, {
        props: {
          url: node.url,
          name: node.name,
          large: emojisOnly
        }
      })
    case 'text':
      return node.content
    case 'br':
      return h('br')
  }

  // eslint-disable-next-line no-console
  console.error(`Could not map Discord message element`, node)

  return h('span')
}
