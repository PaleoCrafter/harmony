import Spoiler from '@/components/message/Spoiler.vue'
import UserMention from '@/components/message/UserMention.vue'
import ChannelMention from '@/components/message/ChannelMention.vue'
import GenericMention from '@/components/message/GenericMention.vue'
import RoleMention from '@/components/message/RoleMention.vue'

export default function renderNode (node, h, message) {
  switch (node.type) {
    case 'strong':
    case 'em':
    case 'strike':
    case 'u':
      return h(node.type, node.content.map(child => renderNode(child, h, message)))
    case 'url':
      return h('a', { attrs: { href: node.target, target: '_blank' } }, node.content.map(child => renderNode(child, h)))
    case 'spoiler':
      return h(Spoiler, node.content.map(child => renderNode(child, h, message)))
    case 'blockQuote':
      return h('blockquote', node.content.map(child => renderNode(child, h, message)))
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
    case 'text':
      return node.content
    case 'br':
      return h('br')
  }

  console.log(node)

  return h('span')
}
