import { chainCommands, joinBackward, selectNodeBackward } from 'prosemirror-commands'
import { TextSelection } from 'prosemirror-state'

const deleteCommand = chainCommands(joinBackward, selectNodeBackward)

export function findClosest (doc, startPos, direction, search, returnLast) {
  const content = doc.content
  const prefix = direction < 0 ? '^' : ''
  const suffix = direction > 0 ? '$' : ''
  const regexSource = search instanceof RegExp ? search.source : search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`${prefix}${regexSource}${suffix}`)
  let pos
  for (pos = startPos; pos >= 0 && pos <= content.size; pos += direction) {
    const text = content.textBetween(Math.min(pos, startPos), Math.max(pos, startPos))
    if (regex.exec(text)) {
      return pos
    }

    let anyNodes = false
    content.nodesBetween(Math.min(pos, startPos), Math.max(pos, startPos), (node) => {
      anyNodes = anyNodes || (node.isLeaf && !node.isText)
    })

    if (anyNodes) {
      return returnLast ? pos : null
    }
  }

  return returnLast ? pos : null
}

export function removeMatchedCharacters (startCharacter, endCharacter) {
  if (endCharacter === undefined) {
    endCharacter = startCharacter
  }

  return (state, dispatch, view) => {
    const plugins = state.plugins
    const cursor = state.selection.$cursor

    if (!cursor) {
      return
    }

    if (cursor.nodeBefore?.text?.trim()?.endsWith(startCharacter) && cursor.nodeAfter?.text?.trim()?.startsWith(endCharacter)) {
      if (dispatch) {
        const selection = TextSelection.create(
          state.doc,
          findClosest(state.doc, cursor.pos, -1, startCharacter),
          findClosest(state.doc, cursor.pos, 1, endCharacter)
        )
        dispatch(state.tr.setSelection(selection).deleteSelection().scrollIntoView())
      }
      deleteCommand(state, dispatch, view)
      return true
    }

    for (let i = 0; i < plugins.length; i++) {
      const plugin = plugins[i]
      if (plugin.spec.isInputRules && plugin.getState(state) && cursor.nodeBefore.text?.endsWith(')')) {
        if (dispatch) {
          const selection = TextSelection.create(state.doc, cursor.pos - 1, cursor.pos)
          dispatch(state.tr.setSelection(selection).deleteSelection().scrollIntoView())
        }
        deleteCommand(state, dispatch, view)
        return true
      }
    }

    return false
  }
}
