package com.varlor.backend.product.controller

import com.varlor.backend.common.controller.BaseCrudController
import com.varlor.backend.product.model.dto.ClientDto
import com.varlor.backend.product.model.dto.CreateClientDto
import com.varlor.backend.product.model.dto.UpdateClientDto
import com.varlor.backend.product.service.ClientService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gestion des clients")
@SecurityRequirement(name = "bearerAuth")
class ClientController(
    clientService: ClientService
) : BaseCrudController<ClientDto, CreateClientDto, UpdateClientDto, UUID>(
    service = clientService,
    basePath = "/api/clients",
    entityName = "clients",
    dtoClass = ClientDto::class.java
) {
    override fun getId(entity: ClientDto): UUID = entity.id
}

