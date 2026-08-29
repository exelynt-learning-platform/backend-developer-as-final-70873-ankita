package com.exelynt.booking.service;

import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.model.Resource;
import com.exelynt.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    public Resource createResource(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long id, ResourceRequest request) {
        Resource resource = getResourceById(id);

        // Partial update: only overwrite fields that were actually supplied,
        // so an update that omits e.g. description doesn't erase it.
        if (request.getTitle() != null) {
            resource.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {
        Resource resource = getResourceById(id);
        resourceRepository.delete(resource);
    }
}
