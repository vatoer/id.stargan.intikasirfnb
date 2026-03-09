package id.stargan.intikasirfnb.domain.shared

/**
 * Base interface for all domain events.
 *
 * Domain events represent something that happened in the domain.
 * They are published after a use case completes successfully and
 * consumed by handlers in other bounded contexts.
 */
interface DomainEvent {
    val eventId: String get() = UlidGenerator.generate()
    val occurredAtMillis: Long get() = System.currentTimeMillis()
}

/**
 * In-process domain event bus.
 *
 * Simple publish/subscribe mechanism for decoupling bounded contexts.
 * Handlers run in the caller's coroutine scope — keep them lightweight.
 * For heavy work, handlers should launch their own coroutine.
 */
interface DomainEventBus {
    suspend fun publish(event: DomainEvent)
    fun <T : DomainEvent> subscribe(eventType: Class<T>, handler: suspend (T) -> Unit)
}

/** Subscribe using reified type parameter */
inline fun <reified T : DomainEvent> DomainEventBus.on(noinline handler: suspend (T) -> Unit) {
    subscribe(T::class.java, handler)
}
