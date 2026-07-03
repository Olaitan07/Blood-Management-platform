package com.blood.inventory.transfer;

import com.blood.inventory.model.BloodGroup;
import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.repository.BloodInventoryRepository;
import com.blood.inventory.repository.InventoryAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression test for a real bug fixed this session: finalizeTransfer used to
 * always credit the destination with the full *approved* quantity regardless
 * of what was actually received, silently absorbing any shortfall. The fixed
 * behavior: the source always loses the full approved amount (that's what was
 * physically shipped), but the destination is credited only what's confirmed
 * received — this class is package-private, so the test lives in the same
 * package to access it.
 */
@ExtendWith(MockitoExtension.class)
class TransferInventoryPortImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC);
    private static final Long SOURCE_INVENTORY_ID = 1L;
    private static final Long SOURCE_HOSPITAL_ID = 10L;
    private static final Long DEST_HOSPITAL_ID = 20L;
    private static final LocalDate EXPIRY = LocalDate.of(2026, 9, 1);

    @Mock
    private BloodInventoryRepository inventoryRepository;
    @Mock
    private InventoryAuditLogRepository auditLogRepository;

    private TransferInventoryPortImpl port;
    private BloodInventory source;

    @BeforeEach
    void setUp() {
        port = new TransferInventoryPortImpl(inventoryRepository, auditLogRepository, FIXED_CLOCK);

        source = BloodInventory.builder()
                .id(SOURCE_INVENTORY_ID)
                .hospitalId(SOURCE_HOSPITAL_ID)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .unitsAvailable(10)
                .unitsReserved(4)
                .expiryDate(EXPIRY)
                .lastUpdated(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();

        when(inventoryRepository.findById(SOURCE_INVENTORY_ID)).thenReturn(Optional.of(source));
        // save() echoes back whatever was passed in (real JPA semantics), but
        // also simulates auto-generated-ID assignment for a brand-new entity
        // (id == null) — a freshly built() destination row has no id yet,
        // same as it wouldn't until a real INSERT actually ran.
        when(inventoryRepository.save(any(BloodInventory.class))).thenAnswer(inv -> {
            BloodInventory entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(999L);
            }
            return entity;
        });
    }

    @Test
    void finalizeTransfer_fullReceipt_creditsDestinationWithFullAmount() {
        when(inventoryRepository.findAvailableByHospital(eq(DEST_HOSPITAL_ID), any())).thenReturn(List.of());

        Long resultId = port.finalizeTransfer(SOURCE_INVENTORY_ID, 4, 4, DEST_HOSPITAL_ID, "officer@test.com");

        ArgumentCaptor<BloodInventory> captor = ArgumentCaptor.forClass(BloodInventory.class);
        verify(inventoryRepository, times(2)).save(captor.capture());
        BloodInventory savedSource = captor.getAllValues().get(0);
        BloodInventory savedDest = captor.getAllValues().get(1);

        assertThat(savedSource.getUnitsAvailable()).isEqualTo(6); // 10 - 4 approved
        assertThat(savedSource.getUnitsReserved()).isEqualTo(0); // 4 - 4 approved

        assertThat(savedDest.getUnitsAvailable()).isEqualTo(4); // full amount, since received == approved
        assertThat(savedDest.getHospitalId()).isEqualTo(DEST_HOSPITAL_ID);
        assertThat(savedDest.getExpiryDate()).isEqualTo(EXPIRY);
        assertThat(resultId).isNotNull();
    }

    @Test
    void finalizeTransfer_partialReceipt_sourceLosesFullApprovedAmount_destGetsOnlyReceived() {
        when(inventoryRepository.findAvailableByHospital(eq(DEST_HOSPITAL_ID), any())).thenReturn(List.of());

        // THE CRITICAL CASE: approved 4, only 3 actually arrive.
        port.finalizeTransfer(SOURCE_INVENTORY_ID, 4, 3, DEST_HOSPITAL_ID, "officer@test.com");

        ArgumentCaptor<BloodInventory> captor = ArgumentCaptor.forClass(BloodInventory.class);
        verify(inventoryRepository, times(2)).save(captor.capture());
        BloodInventory savedSource = captor.getAllValues().get(0);
        BloodInventory savedDest = captor.getAllValues().get(1);

        // Source ships the full approved amount regardless of what arrives intact.
        assertThat(savedSource.getUnitsAvailable()).isEqualTo(6); // 10 - 4, NOT 10 - 3
        assertThat(savedSource.getUnitsReserved()).isEqualTo(0); // 4 - 4

        // Destination is credited only what was actually received — the bug
        // this test guards against credited the full 4 here.
        assertThat(savedDest.getUnitsAvailable()).isEqualTo(3);
    }

    @Test
    void finalizeTransfer_partialReceipt_creditsExistingDestinationRowByReceivedAmountOnly() {
        BloodInventory existingDest = BloodInventory.builder()
                .id(99L)
                .hospitalId(DEST_HOSPITAL_ID)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .unitsAvailable(2)
                .unitsReserved(0)
                .expiryDate(EXPIRY)
                .lastUpdated(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();
        when(inventoryRepository.findAvailableByHospital(eq(DEST_HOSPITAL_ID), any())).thenReturn(List.of(existingDest));

        port.finalizeTransfer(SOURCE_INVENTORY_ID, 4, 3, DEST_HOSPITAL_ID, "officer@test.com");

        ArgumentCaptor<BloodInventory> captor = ArgumentCaptor.forClass(BloodInventory.class);
        verify(inventoryRepository, times(2)).save(captor.capture());
        BloodInventory savedDest = captor.getAllValues().get(1);

        assertThat(savedDest.getId()).isEqualTo(99L);
        assertThat(savedDest.getUnitsAvailable()).isEqualTo(5); // 2 existing + 3 received, not +4
    }

    @Test
    void finalizeTransfer_totalLossInTransit_sourceStillDebitedButNoDestinationCreditOccurs() {
        Long resultId = port.finalizeTransfer(SOURCE_INVENTORY_ID, 4, 0, DEST_HOSPITAL_ID, "officer@test.com");

        // Source still loses the full approved amount — it was genuinely shipped.
        ArgumentCaptor<BloodInventory> captor = ArgumentCaptor.forClass(BloodInventory.class);
        verify(inventoryRepository, times(1)).save(captor.capture()); // source only, no dest save at all
        BloodInventory savedSource = captor.getValue();
        assertThat(savedSource.getUnitsAvailable()).isEqualTo(6);
        assertThat(savedSource.getUnitsReserved()).isEqualTo(0);

        // No destination lookup/credit happens for a total loss.
        verify(inventoryRepository, never()).findAvailableByHospital(eq(DEST_HOSPITAL_ID), any());
        assertThat(resultId).isNull();
    }

    @Test
    void finalizeTransfer_reservedFloorNeverGoesNegative_whenApprovedExceedsReserved() {
        source.setUnitsReserved(2); // less than the 4 approved units being finalized
        when(inventoryRepository.findAvailableByHospital(eq(DEST_HOSPITAL_ID), any())).thenReturn(List.of());

        port.finalizeTransfer(SOURCE_INVENTORY_ID, 4, 4, DEST_HOSPITAL_ID, "officer@test.com");

        ArgumentCaptor<BloodInventory> captor = ArgumentCaptor.forClass(BloodInventory.class);
        verify(inventoryRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getUnitsReserved()).isEqualTo(0); // floored at 0, not -2
    }
}
