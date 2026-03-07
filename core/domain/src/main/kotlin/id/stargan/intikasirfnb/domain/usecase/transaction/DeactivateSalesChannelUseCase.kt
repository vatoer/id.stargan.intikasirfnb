package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository

class DeactivateSalesChannelUseCase(private val repository: SalesChannelRepository) {
    suspend operator fun invoke(channelId: SalesChannelId): Result<Unit> = runCatching {
        val channel = repository.getById(channelId) ?: error("Channel not found")
        repository.save(channel.copy(isActive = false))
    }
}
