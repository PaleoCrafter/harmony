package com.seventeenthshard.harmony.bot

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class EventHandler(name: String, init: EventHandler.() -> Unit) {
    private val handlers = mutableMapOf<Class<*>, Handler<*>>()
    val logger: Logger = LogManager.getLogger(name)

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
            logger.info("Handled event of type ${event.javaClass} for $id successfully")
        }
    }

    private data class Handler<T>(val run: (id: String, event: T) -> Unit)
}
