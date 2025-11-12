package com.varlor.backend.product.controller

import com.varlor.backend.common.controller.BaseCrudController
import com.varlor.backend.product.model.dto.ClientDto
import com.varlor.backend.product.model.dto.CreateClientDto
import com.varlor.backend.product.model.dto.UpdateClientDto
import com.varlor.backend.product.service.ClientService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gestion des clients")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'OWNER', 'SERVICE')")
class ClientController(
    clientService: ClientService
) : BaseCrudController<ClientDto, CreateClientDto, UpdateClientDto, UUID>(
    service = clientService,
    basePath = "/api/clients",
    entityName = "clients",
    dtoClass = ClientDto::class.java
) {
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    override fun create(@Valid @RequestBody dto: CreateClientDto) = super.create(dto)
    
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    override fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateClientDto
    ) = super.update(id, dto)
}

