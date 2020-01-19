/**
 * More open version of https://github.com/brussell98/discord-markdown
 *
 * MIT License
 *
 * Copyright (c) 2017 Brussell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import markdown from 'simple-markdown'

const rules = {
  blockQuote: Object.assign({}, markdown.defaultRules.blockQuote, {
    match (source, state, prevSource) {
      return !/^$|\n *$/.test(prevSource) || state.inQuote ? null : /^( *>>> ([\s\S]*))|^( *> [^\n]*(\n *> [^\n]*)*\n?)/.exec(source)
    },
    parse (capture, parse, state) {
      const all = capture[0]
      const isBlock = Boolean(/^ *>>> ?/.exec(all))
      const removeSyntaxRegex = isBlock ? /^ *>>> ?/ : /^ *> ?/gm
      const content = all.replace(removeSyntaxRegex, '')

      state.inQuote = true
      if (!isBlock) {
        state.inline = true
      }

      const parsed = parse(content, state)

      state.inQuote = state.inQuote || false
      state.inline = state.inline || false

      return {
        content: parsed,
        type: 'blockQuote'
      }
    }
  }),
  codeBlock: Object.assign({}, markdown.defaultRules.codeBlock, {
    match: markdown.inlineRegex(/^```(([a-z0-9-]+?)\n+)?\n*([^]+?)\n*```/i),
    parse (capture, parse, state) {
      return {
        lang: (capture[2] || '').trim(),
        content: capture[3] || '',
        inQuote: state.inQuote || false
      }
    }
  }),
  newline: markdown.defaultRules.newline,
  escape: markdown.defaultRules.escape,
  autolink: Object.assign({}, markdown.defaultRules.autolink, {
    parse: (capture) => {
      return {
        content: [{
          type: 'text',
          content: capture[1]
        }],
        target: capture[1]
      }
    }
  }),
  url: Object.assign({}, markdown.defaultRules.url, {
    parse: (capture) => {
      return {
        content: [{
          type: 'text',
          content: capture[1]
        }],
        target: capture[1]
      }
    }
  }),
  em: markdown.defaultRules.em,
  strong: markdown.defaultRules.strong,
  u: markdown.defaultRules.u,
  strike: Object.assign({}, markdown.defaultRules.del, {
    match: markdown.inlineRegex(/^~~([\s\S]+?)~~(?!_)/)
  }),
  inlineCode: markdown.defaultRules.inlineCode,
  text: Object.assign({}, markdown.defaultRules.text, {
    match: source => /^[\s\S]+?(?=[^0-9A-Za-z\s\u00C0-\uFFFF-]|\n\n|\n|\w+:\S|$)/.exec(source)
  }),
  emoticon: {
    order: markdown.defaultRules.text.order,
    match: source => /^(¯\\_\(ツ\)_\/¯)/.exec(source),
    parse (capture) {
      return {
        type: 'text',
        content: capture[1]
      }
    }
  },
  br: Object.assign({}, markdown.defaultRules.br, {
    match: markdown.anyScopeRegex(/^\n/)
  }),
  spoiler: {
    order: 0,
    match: source => /^\|\|([\s\S]+?)\|\|/.exec(source),
    parse (capture, parse, state) {
      return {
        content: parse(capture[1], state)
      }
    }
  },
  searchHighlight: {
    order: markdown.defaultRules.u.order - 1,
    match: markdown.inlineRegex(/^__HARMONY_SEARCH_\d+__((?:\\[\s\S]|[^\\])+?)__HARMONY_SEARCH_\d+__/),
    quality: capture => capture[0].length,
    parse (capture, parse, state) {
      return {
        content: parse(capture[1], state)
      }
    }
  }
}

const discordRules = {
  discordUser: {
    order: markdown.defaultRules.strong.order,
    match: source => /^<@!?([0-9]*)>/.exec(source),
    parse (capture) {
      return {
        id: capture[1]
      }
    }
  },
  discordChannel: {
    order: markdown.defaultRules.strong.order,
    match: source => /^<#?([0-9]*)>/.exec(source),
    parse (capture) {
      return {
        id: capture[1]
      }
    }
  },
  discordRole: {
    order: markdown.defaultRules.strong.order,
    match: source => /^<@&([0-9]*)>/.exec(source),
    parse (capture) {
      return {
        id: capture[1]
      }
    }
  },
  discordEmoji: {
    order: markdown.defaultRules.strong.order,
    match: source => /^<(a?):(\w+):(\d+)>/.exec(source),
    parse (capture) {
      return {
        animated: capture[1] === 'a',
        name: capture[2],
        id: capture[3]
      }
    }
  },
  discordEveryone: {
    order: markdown.defaultRules.strong.order,
    match: source => /^@everyone/.exec(source),
    parse () {
      return {}
    }
  },
  discordHere: {
    order: markdown.defaultRules.strong.order,
    match: source => /^@here/.exec(source),
    parse () {
      return {}
    }
  }
}
Object.assign(rules, discordRules)

const embedRules = Object.assign({}, rules, {
  link: markdown.defaultRules.link
})

const parser = markdown.parserFor(rules)
const embedParser = markdown.parserFor(embedRules)

export const parse = source => parser(source, { inline: true })
export const parseEmbed = source => embedParser(source, { inline: true })
