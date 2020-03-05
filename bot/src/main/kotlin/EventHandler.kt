package com.seventeenthshard.harmony.bot

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class EventHandler(init: EventHandler.() -> Unit) {
    private val handlers = mutableMapOf<Class<*>, Handler<*>>()
    val logger: Logger = LogManager.getLogger("EventHandler")

    init {
        this.init()
    }

    fun <T> addHandler(
        clazz: Class<T>,
        handler: (id: String, event: T) -> Unit
    ) {
        handlers[clazz] = Handler(handler)
    }

    inline fun <reified T : Any> listen(noinline handler: (id: String, event: T) -> Unit) {
        addHandler(T::class.java, handler)
    }

    fun handle(id: String, event: Any) {
        (handlers[event.javaClass] as? Handler<Any>)?.let {
            it.run(id, event)
        }
    }

    private data class Handler<T>(val run: (id: String, event: T) -> Unit)
}
