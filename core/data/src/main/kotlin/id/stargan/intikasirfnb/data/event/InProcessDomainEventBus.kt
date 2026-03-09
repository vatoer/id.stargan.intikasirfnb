package id.stargan.intikasirfnb.data.event

import id.stargan.intikasirfnb.domain.shared.DomainEvent
import id.stargan.intikasirfnb.domain.shared.DomainEventBus

/**
 * Simple in-process event bus backed by a map of handlers.
 * Thread-safe via synchronized handler list.
 */
class InProcessDomainEventBus : DomainEventBus {

    private val handlers = mutableMapOf<Class<*>, MutableList<suspend (Any) -> Unit>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : DomainEvent> subscribe(eventType: Class<T>, handler: suspend (T) -> Unit) {
        synchronized(handlers) {
            handlers.getOrPut(eventType) { mutableListOf() }
                .add(handler as suspend (Any) -> Unit)
        }
    }

    override suspend fun publish(event: DomainEvent) {
        val eventHandlers = synchronized(handlers) {
            handlers[event::class.java]?.toList() ?: emptyList()
        }
        eventHandlers.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                // Log but don't propagate — event handlers should not break the publisher
                android.util.Log.e("DomainEventBus", "Handler failed for ${event::class.simpleName}", e)
            }
        }
    }
}
