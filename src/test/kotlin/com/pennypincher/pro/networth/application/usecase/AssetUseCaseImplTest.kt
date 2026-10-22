package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.AssetCommand
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AssetUseCaseImplTest {
    private val repository = mockk<AssetRepository>()
    private val useCase = AssetUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_sort_by_category_then_name_ignoring_case_when_listing() {
        val unsorted = listOf(
            asset("zeta", AssetCategory.OTHER),
            asset("carro", AssetCategory.VEHICLE),
            asset("Apto", AssetCategory.REAL_ESTATE),
            asset("Bici", AssetCategory.VEHICLE),
        )
        every { repository.findAllActive(userId, null) } returns unsorted

        val names = useCase.list(userId, null).map { it.name }

        assertEquals(listOf("Apto", "Bici", "carro", "zeta"), names)
    }

    @Test
    fun should_pass_category_to_repository_when_filtering() {
        val vehicle = asset("Carro", AssetCategory.VEHICLE)
        every { repository.findAllActive(userId, AssetCategory.VEHICLE) } returns listOf(vehicle)

        assertEquals(listOf(vehicle), useCase.list(userId, AssetCategory.VEHICLE))
    }

    @Test
    fun should_return_total_with_two_decimals_when_summing_values() {
        every { repository.sumActive(userId) } returns BigDecimal.ZERO

        assertEquals("0.00", useCase.total(userId).toPlainString())
    }

    @Test
    fun should_save_trimmed_asset_with_scaled_value_when_creating() {
        val saved = slot<Asset>()
        every { repository.save(capture(saved)) } answers { firstArg() }

        val created = useCase.create(userId, AssetCommand("  Apto Madera ", BigDecimal("350000000"), REAL_ESTATE))

        assertEquals(saved.captured, created)
        assertEquals(userId, created.userId)
        assertEquals("Apto Madera", created.name)
        assertEquals("350000000.00", created.value.toPlainString())
        assertEquals(REAL_ESTATE, created.category)
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_reject_command_without_touching_repository_when_name_or_value_is_invalid() {
        val invalid = listOf(
            AssetCommand(" ", BigDecimal.ONE, REAL_ESTATE) to listOf("name"),
            AssetCommand("x".repeat(Asset.MAX_NAME_LENGTH + 1), BigDecimal.ONE, REAL_ESTATE) to listOf("name"),
            AssetCommand("Carro", BigDecimal("-1"), REAL_ESTATE) to listOf("value"),
            AssetCommand("Carro", BigDecimal("1.005"), REAL_ESTATE) to listOf("value"),
            AssetCommand("", BigDecimal("-1"), REAL_ESTATE) to listOf("name", "value"),
        )

        invalid.forEach { (command, fields) ->
            val exception = assertThrows<ValidationException> { useCase.create(userId, command) }
            assertEquals(fields, exception.violations.map { it.field })
            assertThrows<ValidationException> { useCase.update(userId, UUID.randomUUID(), command) }
        }
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { repository.findActiveById(any(), any()) }
    }

    @Test
    fun should_accept_zero_value_and_name_of_maximum_length_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        assertDoesNotThrow {
            useCase.create(userId, AssetCommand("x".repeat(Asset.MAX_NAME_LENGTH), BigDecimal.ZERO, REAL_ESTATE))
        }
    }

    @Test
    fun should_keep_identity_and_creation_time_when_updating() {
        val current = asset("Carro", AssetCategory.VEHICLE)
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(
            userId,
            current.id,
            AssetCommand(" Moto ", BigDecimal("12.5"), AssetCategory.OTHER),
        )

        assertEquals(current.id, updated.id)
        assertEquals(current.createdAt, updated.createdAt)
        assertEquals("Moto", updated.name)
        assertEquals("12.50", updated.value.toPlainString())
        assertEquals(AssetCategory.OTHER, updated.category)
        assertEquals(NOW, updated.updatedAt)
        assertNotEquals(current.updatedAt, updated.updatedAt)
        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun should_throw_not_found_when_updating_asset_that_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        val exception = assertThrows<ResourceNotFoundException> {
            useCase.update(userId, id, AssetCommand("Carro", BigDecimal.ONE, REAL_ESTATE))
        }

        assertEquals("Asset not found", exception.message)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun should_return_own_active_asset_when_getting_by_id() {
        val asset = asset("Carro", AssetCategory.VEHICLE)
        every { repository.findActiveById(userId, asset.id) } returns asset

        assertEquals(asset, useCase.get(userId, asset.id))
    }

    @Test
    fun should_throw_not_found_when_getting_asset_that_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }
    }

    @Test
    fun should_deactivate_asset_when_deleting() {
        val id = UUID.randomUUID()
        every { repository.deactivate(userId, id) } returns true

        useCase.delete(userId, id)

        verify(exactly = 1) { repository.deactivate(userId, id) }
    }

    @Test
    fun should_throw_not_found_when_deleting_asset_that_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.deactivate(userId, id) } returns false

        assertThrows<ResourceNotFoundException> { useCase.delete(userId, id) }
    }

    private fun asset(name: String, category: AssetCategory) = Asset(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        value = BigDecimal("100.00"),
        category = category,
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
        val REAL_ESTATE = AssetCategory.REAL_ESTATE
    }
}
