export default function (slice, elasticQuery) {
  const from = 0
  const to = slice.content.size
  let text = ''
  let separated = true
  slice.content.nodesBetween(from, to, (node, pos) => {
    if (node.isText) {
      text += node.text.slice(Math.max(from, pos) - pos, to - pos)
      separated = false
    } else if (node.type.name === 'field') {
      text += `${node.attrs.field}:`
    } else if (node.type.name === 'fieldvalue') {
      text += `${elasticQuery ? node.attrs.id : node.attrs.label}:`
    } else if (!separated && node.isBlock) {
      text += '\n\n'
      separated = true
    }
  }, 0)

  return text
}
