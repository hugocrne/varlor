package com.varlor.backend.product.controller

import com.varlor.backend.common.controller.BaseCrudController
import com.varlor.backend.product.model.dto.CreateUserPreferenceDto
import com.varlor.backend.product.model.dto.UpdateUserPreferenceDto
import com.varlor.backend.product.model.dto.UserPreferenceDto
import com.varlor.backend.product.service.UserPreferenceService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user-preferences")
@Tag(name = "Préférences utilisateur", description = "Gestion des préférences utilisateur")
@SecurityRequirement(name = "bearerAuth")
class UserPreferenceController(
    userPreferenceService: UserPreferenceService
) : BaseCrudController<UserPreferenceDto, CreateUserPreferenceDto, UpdateUserPreferenceDto, UUID>(
    service = userPreferenceService,
    basePath = "/api/user-preferences",
    entityName = "préférences utilisateur",
    dtoClass = UserPreferenceDto::class.java
) {
    override fun getId(entity: UserPreferenceDto): UUID = entity.id
}

