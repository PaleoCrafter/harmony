import { Extension, Plugin } from 'tiptap'
import { Decoration, DecorationSet } from 'prosemirror-view'
import { InputRule } from 'prosemirror-inputrules'
import { TextSelection } from 'prosemirror-state'
import { keymap } from 'prosemirror-keymap'
import { removeMatchedCharacters } from '@/components/search/utils'

export function findMatchingParenthesis (parens, backward) {
  if (backward) {
    parens.reverse()
  }

  let pairCount = 1
  for (const { type, index } of parens) {
    if ((type === ')' && backward) || (type === '(' && !backward)) {
      pairCount++
      continue
    }

    if ((type === '(' && backward) || (type === ')' && !backward)) {
      pairCount--
    }

    if (pairCount === 0) {
      return index
    }
  }

  return null
}

export function findParentheses (doc) {
  const parens = []
  doc.content.descendants(
    (node, pos) => {
      if (node.isText) {
        const regex = /[()]/g
        let match
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

export default class ParenthesisMatching extends Extension {
  get name () {
    return 'parentheses'
  }

  inputRules () {
    return [
      new InputRule(/([()])$/, function (state, match, start, end) {
        const tr = state.tr
        const type = match[1]

        if (type === '(') {
          if (end > start) {
            tr.insertText('(', start, start)
              .insertText(')', end + 1, end + 1)
              .setSelection(TextSelection.create(tr.doc, end + 2, end + 2))
          } else {
            tr.insertText('()').setSelection(TextSelection.create(tr.doc, start + 1, start + 1))
          }
        } else if (type === ')') {
          tr.insertText(')', start, start + 1)
            .setSelection(TextSelection.create(tr.doc, start + 1, start + 1))
        }

        return tr
      })
    ]
  }

  get plugins () {
    return [
      keymap({
        Backspace: removeMatchedCharacters('(', ')')
      }),
      new Plugin({
        props: {
          decorations: (state) => {
            const selection = state.selection.$cursor
            if (!selection) {
              return DecorationSet.empty
            }

            const allParens = findParentheses(state.doc)
            const unmatchedParens = allParens.filter(({ type, index }, i) =>
              findMatchingParenthesis(
                type === ')' ? allParens.slice(0, i) : allParens.slice(i + 1),
                type === ')'
              ) === null
            )
            let decorations = DecorationSet.create(
              state.doc,
              unmatchedParens.map(({ index }) => Decoration.inline(index, index + 1, {
                nodeName: 'span',
                class: 'search-box__parens search-box__parens--unmatched'
              }))
            )

            const before = selection.nodeBefore
            const after = selection.nodeAfter

            if (!before && !after) {
              return decorations
            }

            const beforeHighlight = before && (before.textContent.endsWith('(') || before.textContent.endsWith(')'))
            const afterHighlight = after && (after.textContent.startsWith('(') || after.textContent.startsWith(')'))
            const ownHighlightStart = beforeHighlight ? selection.pos - 1 : selection.pos
            const forwardSearchStart = beforeHighlight ? selection.pos : selection.pos + 1

            if (!beforeHighlight && !afterHighlight) {
              return decorations
            }

            const searchBackward = beforeHighlight ? before.textContent.endsWith(')') : after.textContent.startsWith(')')
            const parens = []
            const from = searchBackward ? 0 : forwardSearchStart
            const to = searchBackward ? ownHighlightStart : state.doc.content.size
            state.doc.content.nodesBetween(
              from,
              to,
              (node, pos) => {
                if (node.isText) {
                  const regex = /[()]/g
                  let match
                  while ((match = regex.exec(node.text)) !== null) {
                    const index = pos + match.index
                    if (index < from || index >= to) {
                      continue
                    }
                    parens.push({ type: match[0], index })
                  }
                }
              },
              0
            )

            const matching = findMatchingParenthesis(parens, searchBackward)
            if (matching !== null) {
              decorations = decorations.add(state.doc, [
                Decoration.inline(matching, matching + 1, {
                  nodeName: 'span',
                  class: 'search-box__parens'
                }),
                Decoration.inline(ownHighlightStart, ownHighlightStart + 1, {
                  nodeName: 'span',
                  class: 'search-box__parens'
                })
              ])
            }

            return decorations
          }
        }
      })
    ]
  }
}
