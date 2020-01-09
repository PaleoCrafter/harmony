package com.seventeenthshard.harmony.search.simpleast.core.node

/**
 * @param RC RenderContext
 * @param T Type of style to apply
 */
open class StyleNode<RC, T>(val styles: List<T>) : Node<RC>() {

  override fun render(builder: StringBuilder, renderContext: RC) {
    val startIndex = builder.length

    // First render all child nodes, as these are the nodes we want to apply the styles to.
    getChildren()?.forEach { it.render(builder, renderContext) }
  }

  override fun toString() = "${javaClass.simpleName} >\n" +
      getChildren()?.joinToString("\n->", prefix = ">>", postfix = "\n>|") {
        it.toString()
      }
}
