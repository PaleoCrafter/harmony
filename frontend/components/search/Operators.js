import { Extension, Plugin } from 'tiptap'
import { Decoration, DecorationSet } from 'prosemirror-view'

export default class ParenthesisMatching extends Extension {
  get name () {
    return 'operators'
  }

  get plugins () {
    return [
      new Plugin({
        props: {
          decorations: (state) => {
            const selection = state.selection.$cursor
            if (!selection) {
              return DecorationSet.empty
            }

            const allOperators = []
            state.doc.content.descendants(
              (node, pos) => {
                if (node.isText) {
                  const regex = /(?<=^|[\s(])(OR|AND|NOT|&&|\|\||!)(?=[\s)]|$)/g
                  let match
                  while ((match = regex.exec(node.text)) !== null) {
                    const index = pos + match.index
                    allOperators.push({ operator: match[1], index })
                  }
                }
              },
              0
            )

            return DecorationSet.create(
              state.doc,
              allOperators.map(({ index, operator }) => Decoration.inline(index, index + operator.length, {
                nodeName: 'span',
                class: 'search-box__operator'
              }))
            )
          }
        }
      })
    ]
  }
}
