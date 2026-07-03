package com.blood.hospital.service;

import com.blood.hospital.dto.HospitalRequest;
import com.blood.hospital.dto.HospitalResponse;
import com.blood.hospital.event.HospitalDeactivatedEvent;
import com.blood.hospital.event.HospitalRegisteredEvent;
import com.blood.hospital.exception.DuplicateHospitalException;
import com.blood.hospital.exception.HospitalNotFoundException;
import com.blood.hospital.model.Hospital;
import com.blood.hospital.model.HospitalStatus;
import com.blood.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    @Mock
    private HospitalRepository hospitalRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HospitalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HospitalServiceImpl(hospitalRepository, eventPublisher);
    }

    @Test
    void registerHospital_duplicateNameAndCity_throwsWithoutSaving() {
        HospitalRequest request = new HospitalRequest("St. Mary's", "1 Main St", "Lagos", "Lagos Island", "555-0100");
        when(hospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase("St. Mary's", "Lagos Island"))
                .thenReturn(Optional.of(mock(Hospital.class)));

        assertThatThrownBy(() -> service.registerHospital(request))
                .isInstanceOf(DuplicateHospitalException.class)
                .hasMessageContaining("St. Mary's")
                .hasMessageContaining("Lagos Island");

        verify(hospitalRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void registerHospital_duplicateCheckIsCaseInsensitive() {
        // Same name/city but different casing — the repository method name itself
        // (findByNameIgnoreCaseAndCityIgnoreCase) is what enforces this; verify
        // the service actually calls that exact method rather than a case-sensitive one.
        HospitalRequest request = new HospitalRequest("st. mary's", "1 Main St", "Lagos", "lagos island", "555-0100");
        when(hospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase("st. mary's", "lagos island"))
                .thenReturn(Optional.of(mock(Hospital.class)));

        assertThatThrownBy(() -> service.registerHospital(request)).isInstanceOf(DuplicateHospitalException.class);
    }

    @Test
    void registerHospital_noDuplicate_savesAndPublishesEvent() {
        HospitalRequest request = new HospitalRequest("New Hospital", "2 Side St", "Oyo", "Ibadan", "555-0200");
        when(hospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase("New Hospital", "Ibadan"))
                .thenReturn(Optional.empty());
        Hospital saved = Hospital.builder()
                .id(1L).name("New Hospital").address("2 Side St").state("Oyo").city("Ibadan")
                .contact("555-0200").status(HospitalStatus.ACTIVE).build();
        when(hospitalRepository.save(any(Hospital.class))).thenReturn(saved);

        HospitalResponse response = service.registerHospital(request);

        assertThat(response.name()).isEqualTo("New Hospital");
        assertThat(response.status()).isEqualTo(HospitalStatus.ACTIVE);

        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(HospitalStatus.ACTIVE);

        verify(eventPublisher).publishEvent(any(HospitalRegisteredEvent.class));
    }

    @Test
    void deactivateHospital_unconditionallySucceeds_noPendingTransferCheck() {
        // Verified this session: there is genuinely no check for pending
        // transfers/inventory before deactivating — this test documents that
        // real (if perhaps surprising) behavior so a future change to add
        // such a check is a deliberate decision, not an accidental regression.
        Hospital hospital = Hospital.builder()
                .id(5L).name("Test Hospital").address("addr").state("state").city("city")
                .contact("555").status(HospitalStatus.ACTIVE).build();
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(hospital));
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(inv -> inv.getArgument(0));

        HospitalResponse response = service.deactivateHospital(5L);

        assertThat(response.status()).isEqualTo(HospitalStatus.INACTIVE);
        verify(eventPublisher).publishEvent(any(HospitalDeactivatedEvent.class));
    }

    @Test
    void deactivateHospital_notFound_throws() {
        when(hospitalRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateHospital(404L))
                .isInstanceOf(HospitalNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void hospitalExists_activeHospital_returnsTrue() {
        Hospital hospital = Hospital.builder().id(1L).status(HospitalStatus.ACTIVE).build();
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        assertThat(service.hospitalExists(1L)).isTrue();
    }

    @Test
    void hospitalExists_inactiveHospital_returnsFalse() {
        Hospital hospital = Hospital.builder().id(1L).status(HospitalStatus.INACTIVE).build();
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        assertThat(service.hospitalExists(1L)).isFalse();
    }

    @Test
    void hospitalExists_unknownId_returnsFalse() {
        when(hospitalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.hospitalExists(999L)).isFalse();
    }
}
