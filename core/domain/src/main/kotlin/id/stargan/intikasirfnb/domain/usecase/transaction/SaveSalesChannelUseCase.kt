package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository

class SaveSalesChannelUseCase(private val repository: SalesChannelRepository) {
    suspend operator fun invoke(channel: SalesChannel): Result<SalesChannel> = runCatching {
        require(channel.name.isNotBlank()) { "Channel name must not be blank" }
        require(channel.code.isNotBlank()) { "Channel code must not be blank" }
        repository.save(channel)
        channel
    }
}
