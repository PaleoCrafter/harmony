import { Extension } from 'tiptap'
import { InputRule } from 'prosemirror-inputrules'
import { TextSelection } from 'prosemirror-state'
import { keymap } from 'prosemirror-keymap'
import { removeMatchedCharacters } from '@/components/search/utils'

function findQuotes (doc) {
  const parens = []
  doc.content.descendants(
    (node, pos) => {
      console.log(node)
      if (node.isText) {
        const regex = /"/g
        let match
        console.log(node.text)
        while ((match = regex.exec(node.text)) !== null) {
          const index = pos + match.index
          parens.push({ type: match[0], index })
        }
      }
    },
    0
  )

  return parens
}

export default class Quotes extends Extension {
  get name () {
    return 'quotes'
  }

  inputRules () {
    return [
      new InputRule(/(""?)$/, function (state, match, start, end) {
        const tr = state.tr
        const type = match[1]

        if (type === '"') {
          if (end > start) {
            tr.insertText('"', start, start)
              .insertText('"', end + 1, end + 1)
              .setSelection(TextSelection.create(tr.doc, end + 2, end + 2))
          } else {
            const existingQuotes = findQuotes(state.doc)
            tr.insertText(existingQuotes.length % 2 === 1 ? '"' : '""').setSelection(TextSelection.create(tr.doc, start + 1, start + 1))
          }
        } else if (type === '""') {
          tr.insertText('"', end, end + 1)
            .setSelection(TextSelection.create(tr.doc, end + 1, end + 1))
        }

        return tr
      })
    ]
  }

  get plugins () {
    return [
      keymap({
        Backspace: removeMatchedCharacters('"')
      })
    ]
  }
}
