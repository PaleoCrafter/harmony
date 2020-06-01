package com.seventeenthshard.harmony.bot

import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.BiFunction

class EventEmitter(val dispatcher: EventDispatcher, val handlers: List<EventHandler>) {
    val logger = LogManager.getLogger("event-emitter")

    inline fun <reified DiscordEvent : Event, reified EmittedEvent : Any> map(
        noinline mapper: (event: DiscordEvent) -> Pair<Snowflake, Mono<EmittedEvent>>
    ) {
        listen<DiscordEvent, EmittedEvent> {
            val (id, emitted) = mapper(it)
            Flux.zip(
                Mono.just(id),
                emitted,
                BiFunction { a: Snowflake, b: EmittedEvent -> a to b }
            )
        }
    }

    inline fun <reified DiscordEvent : Event, reified EmittedEvent : Any> listen(
        noinline mapper: (event: DiscordEvent) -> Flux<Pair<Snowflake, EmittedEvent>>
    ) {
        dispatcher.on(DiscordEvent::class.java)
            .flatMap { event ->
                mapper(event).onErrorResume {
                    logger.error("Failed to handle event $event", it)
                    Mono.empty()
                }
            }
            .map { emit(it.first, it.second) }
            .onErrorContinue { t, e ->
                logger.error("Failed to emit event, event: $e", t)
            }
            .subscribe()
    }

    fun emit(id: Snowflake, event: Any) {
        logger.info("Emitting event $event for $id")
        handlers.forEach { handler -> handler.handle(id.asString(), event) }
    }
}
