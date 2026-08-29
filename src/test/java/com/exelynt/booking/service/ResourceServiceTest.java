package com.exelynt.booking.service;

import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.model.Resource;
import com.exelynt.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResourceService, covering the two bugs the review flagged:
 * updateResource silently nulling out omitted fields, and deleteResource
 * relying on a raw deleteById instead of checking existence first.
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    private ResourceService resourceService;

    private Resource existing;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(resourceRepository);
        existing = new Resource(1L, "Conference Room", "3rd floor", true);
    }

    @Test
    void updateResource_appliesOnlySuppliedFields() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));

        ResourceRequest request = new ResourceRequest();
        request.setAvailable(false); // title/description intentionally omitted

        Resource result = resourceService.updateResource(1L, request);

        assertThat(result.getAvailable()).isFalse();
        assertThat(result.getTitle()).isEqualTo("Conference Room"); // untouched, not wiped to null
        assertThat(result.getDescription()).isEqualTo("3rd floor"); // untouched
    }

    @Test
    void updateResource_overwritesFieldsThatAreSupplied() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));

        ResourceRequest request = new ResourceRequest();
        request.setTitle("Renamed Room");
        request.setDescription("2nd floor");

        Resource result = resourceService.updateResource(1L, request);

        assertThat(result.getTitle()).isEqualTo("Renamed Room");
        assertThat(result.getDescription()).isEqualTo("2nd floor");
    }

    @Test
    void updateResource_throwsNotFound_whenResourceMissing() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.updateResource(999L, new ResourceRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteResource_deletesWhenFound() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(existing));

        resourceService.deleteResource(1L);

        verify(resourceRepository).delete(existing);
    }

    @Test
    void deleteResource_throwsCleanNotFound_insteadOfRawDeleteByIdFailure() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.deleteResource(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(resourceRepository, never()).deleteById(any());
        verify(resourceRepository, never()).delete(any(Resource.class));
    }
}
