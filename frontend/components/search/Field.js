import { Node } from 'tiptap'
import { InputRule } from 'prosemirror-inputrules'
import { keymap } from 'prosemirror-keymap'
import { chainCommands, joinBackward, selectNodeBackward } from 'prosemirror-commands'
import { NodeSelection, Plugin } from 'prosemirror-state'
import { Fragment, Slice } from 'prosemirror-model'
import { findMatchingParenthesis, findParentheses } from '@/components/search/ParenthesisMatching'
import { findClosest } from '@/components/search/utils'

const deleteCommand = chainCommands(joinBackward, selectNodeBackward)

function undoFieldInputRule (state, dispatch, view) {
  const plugins = state.plugins
  for (let i = 0; i < plugins.length; i++) {
    const plugin = plugins[i]
    const cursor = state.selection.$cursor
    if (plugin.spec.isInputRules && plugin.getState(state) && cursor && cursor.nodeBefore.type.name === 'field') {
      if (dispatch) {
        const selection = NodeSelection.create(state.doc, cursor.pos - 1)
        dispatch(state.tr.setSelection(selection).deleteSelection().scrollIntoView())
      }
      deleteCommand(state, dispatch, view)
      return true
    }
  }
  return false
}

const VALID_FIELDS = ['has', 'from', 'mentions', 'in']

function nodeInputRule (regexp, type, getAttrs) {
  return new InputRule(regexp, function (state, match, start, end) {
    const attrs = getAttrs instanceof Function ? getAttrs(match) : getAttrs
    const tr = state.tr

    if (match[0]) {
      tr.replaceWith(start, end, type.create(attrs))
    }

    return tr
  })
}

function nodePasteRule (regexp, type, getAttrs) {
  function handler (fragment) {
    const nodes = []
    fragment.forEach(function (child) {
      if (child.isText) {
        const text = child.text
        let pos = 0
        let match

        do {
          match = regexp.exec(text)

          if (match) {
            const start = match.index
            const end = start + match[0].length
            const attrs = getAttrs instanceof Function ? getAttrs(match) : getAttrs

            if (start > 0) {
              nodes.push(child.cut(pos, start))
            }

            nodes.push(type.create(attrs))
            pos = end
          }
        } while (match)

        if (pos < text.length) {
          nodes.push(child.cut(pos))
        }
      } else {
        nodes.push(child.copy(handler(child.content)))
      }
    })
    return Fragment.fromArray(nodes)
  }

  return new Plugin({
    props: {
      transformPasted: function transformPasted (slice) {
        return new Slice(handler(slice.content), slice.openStart, slice.openEnd)
      }
    }
  })
}

export function findField ({ doc, selection }) {
  const cursor = selection.$cursor
  if (!cursor) {
    return null
  }

  let field = null
  let fieldPos = null
  doc.content.descendants(
    (node, pos) => {
      if (pos > cursor.pos) {
        return false
      }

      if (node.type.name === 'field') {
        field = node
        fieldPos = pos
      }
    },
    0
  )

  if (field === null) {
    return null
  }

  const parentheses = findParentheses(doc).filter(p => p.index >= fieldPos)
  const firstParen = parentheses[0]

  let maxQueryEnd = findClosest(doc, fieldPos + 1, 1, /\s/, true) - 1

  // If the field has a grouped value, make sure the cursor is inside that group
  if (firstParen && firstParen.type === '(' && firstParen.index === fieldPos + 1) {
    parentheses.shift()
    const closing = findMatchingParenthesis(parentheses, false)
    if (closing !== null && cursor.pos > closing) {
      return null
    }

    if (closing !== null) {
      maxQueryEnd = closing
    }
  } else if (firstParen && firstParen.type === ')' && cursor.pos > firstParen.index) {
    return null
  }

  const queryStart = findClosest(doc, cursor.pos, -1, /[\s()]/, true) + 1
  const queryEnd = findClosest(doc, cursor.pos, 1, /[\s()]/, true) - 1

  if (queryEnd > maxQueryEnd) {
    return null
  }

  const query = doc.content.textBetween(queryStart, queryEnd)
  return { reference: field, pos: fieldPos, query, queryStart, queryEnd, cutOff: false }
}

export default class Field extends Node {
  get name () {
    return 'field'
  }

  get schema () {
    return {
      attrs: {
        field: {}
      },
      group: 'inline',
      inline: true,
      selectable: false,
      toDOM: node => [
        'span',
        {
          class: `search-box__field${!VALID_FIELDS.includes(node.attrs.field) ? ' search-box__field--invalid' : ''}`
        },
        `${node.attrs.field}:`
      ]
    }
  }

  inputRules ({ type }) {
    return [
      nodeInputRule(/([A-Za-z]+):/, type, match => ({ field: match[1] }))
    ]
  }

  pasteRules ({ type }) {
    return [
      nodePasteRule(/([A-Za-z]+):/g, type, match => ({ field: match[1] }))
    ]
  }

  get plugins () {
    const { onQuery, onCommit, reset } = this.options

    return [
      keymap({
        Backspace: undoFieldInputRule
      }),
      new Plugin({
        view () {
          return {
            update: ({ state }) => {
              const field = findField(state)
              if (field === null) {
                reset()
                return
              }

              if (field.cutOff) {
                onCommit(field.reference.attrs.field, field.query)
                return
              }

              onQuery(field.reference.attrs.field, field.query)
            }
          }
        }
      })
    ]
  }
}
