package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository

class DeleteCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(id: CategoryId) {
        repository.delete(id)
    }
}
