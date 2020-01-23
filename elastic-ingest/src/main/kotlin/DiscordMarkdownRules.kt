package com.seventeenthshard.harmony.search

import com.seventeenthshard.harmony.search.simpleast.core.node.Node
import com.seventeenthshard.harmony.search.simpleast.core.node.TextNode
import com.seventeenthshard.harmony.search.simpleast.core.parser.ParseSpec
import com.seventeenthshard.harmony.search.simpleast.core.parser.Parser
import com.seventeenthshard.harmony.search.simpleast.core.parser.Rule
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object DiscordMarkdownRules {
    private val PATTERN_BOLD = Pattern.compile("^\\*\\*([\\s\\S]+?)\\*\\*(?!\\*)")
    private val PATTERN_UNDERLINE = Pattern.compile("^__([\\s\\S]+?)__(?!_)")
    private val PATTERN_STRIKETHRU = Pattern.compile("^~~(?=\\S)([\\s\\S]*?\\S)~~")
    private val PATTERN_NEWLINE = Pattern.compile("""^(?:\n *)*\n""")
    private val PATTERN_TEXT = Pattern.compile("^[\\s\\S]+?(?=[^0-9A-Za-z\\s\\u00c0-\\uffff]|\\n| {2,}\\n|\\w+:\\S|$)")
    private val PATTERN_ESCAPE = Pattern.compile("^\\\\([^0-9A-Za-z\\s])")
    private val PATTERN_ITALICS = Pattern.compile(
        // only match _s surrounding words.
        "^\\b_" + "((?:__|\\\\[\\s\\S]|[^\\\\_])+?)_" + "\\b" +
            "|" +
            // Or match *s that are followed by a non-space:
            "^\\*(?=\\S)(" +
            // Match any of:
            //  - `**`: so that bolds inside italics don't close the
            // italics
            //  - whitespace
            //  - non-whitespace, non-* characters
            "(?:\\*\\*|\\s+(?:[^*\\s]|\\*\\*)|[^\\s*])+?" +
            // followed by a non-space, non-* then *
            ")\\*(?!\\*)"
    )
    private val PATTERN_AUTOLINK = Pattern.compile("^<([^: >]+:/[^ >]+)>")
    private val PATTERN_URL = Pattern.compile("^(https?://[^\\s<]+[^<.,:;\"')\\]\\s])")
    private val PATTERN_SPOILER = Pattern.compile("^\\|\\|([\\s\\S]+?)\\|\\|")
    private val PATTERN_USER_MENTION = Pattern.compile("^<@!?([0-9]*)>")
    private val PATTERN_CHANNEL_MENTION = Pattern.compile("^<#?([0-9]*)>")
    private val PATTERN_ROLE_MENTION = Pattern.compile("^<@&([0-9]*)>")
    private val PATTERN_EMOJI = Pattern.compile("^<(a?):(\\w+):(\\d+)>")
    private val PATTERN_INLINE_CODE = Pattern.compile("^(`+)([\\s\\S]*?[^`])\\1(?!`)")
    private val PATTERN_BLOCKQUOTE = Pattern.compile("^( *>>> ([\\s\\S]*))|^( *> [^\\n]*(\\n *> [^\\n]*)*\\n?)")
    private val PATTERN_CODE_BLOCK = Pattern.compile("^```(([a-z0-9-]+?)\\n+)?\\n*(.+?)\\n*```", Pattern.CASE_INSENSITIVE)

    private fun <R> createTextRule() =
        object : Rule<R, Node<R>, State>(PATTERN_TEXT) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                val node = TextNode<R>(matcher.group())
                return ParseSpec.createTerminal(node, state)
            }
        }

    private fun <R> createNewlineRule() =
        object : Rule.BlockRule<R, Node<R>, State>(PATTERN_NEWLINE) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                val node = TextNode<R>("\n")
                return ParseSpec.createTerminal(node, state)
            }
        }

    private fun <R> createEscapeRule() =
        object : Rule<R, Node<R>, State>(PATTERN_ESCAPE) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                return ParseSpec.createTerminal(TextNode(matcher.group(1)), state)
            }
        }

    private fun <R> createItalicsRule() =
        object : Rule<R, Node<R>, State>(PATTERN_ITALICS) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                val startIndex: Int
                val endIndex: Int
                val asteriskMatch = matcher.group(2)
                if (asteriskMatch != null && asteriskMatch.isNotEmpty()) {
                    startIndex = matcher.start(2)
                    endIndex = matcher.end(2)
                } else {
                    startIndex = matcher.start(1)
                    endIndex = matcher.end(1)
                }

                return ParseSpec.createNonterminal(Node(), state, startIndex, endIndex)
            }
        }

    private fun createUserMentionRule() =
        object : Rule<Unit, Node<Unit>, State>(PATTERN_USER_MENTION) {
            override fun parse(matcher: Matcher, parser: Parser<Unit, in Node<Unit>, State>, state: State): ParseSpec<Unit, Node<Unit>, State> {
                return ParseSpec.createTerminal(
                    TextNode("@${matcher.group(1)}"),
                    state.also { it.mentionedUsers += matcher.group(1) }
                )
            }
        }

    private fun createLinkRule(pattern: Pattern) =
        object : Rule<Unit, Node<Unit>, State>(pattern) {
            override fun parse(matcher: Matcher, parser: Parser<Unit, in Node<Unit>, State>, state: State): ParseSpec<Unit, Node<Unit>, State> {
                return ParseSpec.createTerminal(TextNode(matcher.group(1)), state.also { it.hasLinks = true })
            }
        }

    private fun createBlockQuoteRule() =

        object : Rule<Unit, Node<Unit>, State>(PATTERN_BLOCKQUOTE) {
            val BLOCK_START_PATTERN = Pattern.compile("^ *>>> ?")
            val LINE_START_PATTERN = Pattern.compile("^ *> ?", Pattern.MULTILINE)

            override fun match(inspectionSource: CharSequence, lastCapture: String?, state: State): Matcher? {
                val matcher = PATTERN_BLOCKQUOTE.matcher(inspectionSource)
                return if (!lastCapture?.trim().isNullOrEmpty() && state.quote || !matcher.find()) null else matcher
            }

            override fun parse(matcher: Matcher, parser: Parser<Unit, in Node<Unit>, State>, state: State): ParseSpec<Unit, Node<Unit>, State> {
                val content = matcher.group(0)
                val isBlock = BLOCK_START_PATTERN.matcher(content).find()
                val removeSyntaxRegex = (if (isBlock) BLOCK_START_PATTERN else LINE_START_PATTERN).toRegex()
                state.quote = true
                val text = content.replace(removeSyntaxRegex, "")
                val internalNodes = parser.parse(text, state) as MutableList<Node<Unit>>
                state.quote = false

                return ParseSpec.createTerminal(Node<Unit>().also { it.addChildren(internalNodes) }, state)
            }
        }

    private val PARSER by threadLocalLazy {
        Parser<Unit, Node<Unit>, State>().also { parser ->
            parser.addRules(
                listOf(
                    createBlockQuoteRule(),
                    createTerminalRule(PATTERN_CODE_BLOCK) { it.group(3) },
                    createNewlineRule(),
                    createEscapeRule(),
                    createLinkRule(PATTERN_AUTOLINK),
                    createLinkRule(PATTERN_URL),
                    createUserMentionRule(),
                    createTerminalRule(PATTERN_CHANNEL_MENTION) { "#${it.group(1)}" },
                    createTerminalRule(PATTERN_ROLE_MENTION) { "@${it.group(1)}" },
                    createTerminalRule(PATTERN_EMOJI) { ":${it.group(2)}:" },
                    createNonTerminalRule(PATTERN_BOLD),
                    createNonTerminalRule(PATTERN_UNDERLINE),
                    createItalicsRule(),
                    createNonTerminalRule(PATTERN_STRIKETHRU),
                    createNonTerminalRule(PATTERN_SPOILER),
                    createTerminalRule(PATTERN_INLINE_CODE) { it.group(2) },
                    createTextRule()
                )
            )
        }
    }

    fun parse(content: String) =
        State().let { PARSER.parse(content, it) to it }

    private inline fun <R> createNonTerminalRule(pattern: Pattern, crossinline nodeFactory: () -> Node<R> = { Node() }) =
        object : Rule<R, Node<R>, State>(pattern) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                return ParseSpec.createNonterminal(nodeFactory(), state, matcher.start(1), matcher.end(1))
            }
        }

    private inline fun <R> createTerminalRule(pattern: Pattern, crossinline nodeFactory: (matcher: Matcher) -> String = { it.group(1) }) =
        object : Rule<R, Node<R>, State>(pattern) {
            override fun parse(matcher: Matcher, parser: Parser<R, in Node<R>, State>, state: State): ParseSpec<R, Node<R>, State> {
                return ParseSpec.createTerminal(TextNode(nodeFactory(matcher)), state)
            }
        }

    data class State(var quote: Boolean = false, val mentionedUsers: MutableSet<String> = mutableSetOf(), var hasLinks: Boolean = false)
}

class ThreadLocalLazy<T>(val provider: () -> T) : ReadOnlyProperty<Any?, T> {
    private val threadLocal = object : ThreadLocal<T>() {
        override fun initialValue(): T = provider()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        threadLocal.get()
}

fun <T> threadLocalLazy(provider: () -> T) = ThreadLocalLazy(provider)
