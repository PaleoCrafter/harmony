package com.seventeenthshard.harmony.search.simpleast.core.node

/**
 * Node representing simple text.
 */
open class TextNode<R> (val content: String) : Node<R>() {
  override fun render(builder: StringBuilder, renderContext: R) {
    builder.append(content)
  }

  override fun toString() = "${javaClass.simpleName}[${getChildren()?.size}]: $content"
}
