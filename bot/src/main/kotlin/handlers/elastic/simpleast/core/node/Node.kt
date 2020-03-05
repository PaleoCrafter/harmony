package com.seventeenthshard.harmony.bot.handlers.elastic.simpleast.core.node

/**
 * Represents a single node in an Abstract Syntax Tree. It can (but does not need to) have children.
 *
 * @param R The render context, can be any object that holds what's required for rendering. See [render].
 */
open class Node<R> {

  private var children: MutableCollection<Node<R>>? = null

  fun getChildren(): Collection<Node<R>>? = children

  fun hasChildren(): Boolean = children?.isNotEmpty() == true

  fun addChild(child: Node<R>) {
    children = (children ?: ArrayList()).apply {
      add(child)
    }
  }

  fun addChildren(children: Iterable<Node<R>>) {
    this.children = (this.children ?: ArrayList()).apply {
      addAll(children)
    }
  }

  open fun render(builder: StringBuilder, renderContext: R) {
    // First render all child nodes, as these are the nodes we want to apply the styles to.
    getChildren()?.forEach { it.render(builder, renderContext) }
  }

  override fun toString(): String = "${javaClass.simpleName} >\n" +
      getChildren()?.joinToString("\n->", prefix = ">>", postfix = "\n>|") {
        it.toString()
      }
}
