package com.pennypincher.pro.networth.infrastructure.persistence.adapter

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.networth.infrastructure.persistence.repository.AssetJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AssetRepositoryAdapterIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var repository: AssetRepository

    @Autowired
    private lateinit var assets: AssetJpaRepository

    @Test
    fun should_not_reactivate_asset_when_an_update_read_before_the_delete_is_saved_after_it() {
        val userId = createUser().id
        val saved = repository.save(asset(userId))
        val readBeforeDelete = checkNotNull(repository.findActiveById(userId, saved.id))

        assertTrue(repository.deactivate(userId, saved.id))
        repository.save(readBeforeDelete.copy(name = "Late update"))

        assertFalse(assets.findById(saved.id).orElseThrow().active)
        assertNull(repository.findActiveById(userId, saved.id))
        assertEquals(emptyList<Asset>(), repository.findAllActive(userId, null))
    }

    @Test
    fun should_scope_soft_delete_by_user_and_report_whether_a_row_changed() {
        val userId = createUser().id
        val saved = repository.save(asset(userId))

        assertFalse(repository.deactivate(createUser().id, saved.id))
        assertTrue(repository.deactivate(userId, saved.id))
        assertFalse(repository.deactivate(userId, saved.id))
    }

    private fun asset(userId: UUID) = Asset(
        id = UUID.randomUUID(),
        userId = userId,
        name = "Carro",
        value = BigDecimal("100.00"),
        category = AssetCategory.VEHICLE,
        createdAt = NOW,
        updatedAt = NOW,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
    }
}
