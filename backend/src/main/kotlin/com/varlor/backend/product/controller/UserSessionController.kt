package com.varlor.backend.product.controller

import com.varlor.backend.common.controller.BaseCrudController
import com.varlor.backend.product.model.dto.CreateUserSessionDto
import com.varlor.backend.product.model.dto.UpdateUserSessionDto
import com.varlor.backend.product.model.dto.UserSessionDto
import com.varlor.backend.product.service.UserSessionService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user-sessions")
@Tag(name = "Sessions utilisateur", description = "Gestion des sessions utilisateur et refresh tokens")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'OWNER', 'SERVICE')")
class UserSessionController(
    userSessionService: UserSessionService
) : BaseCrudController<UserSessionDto, CreateUserSessionDto, UpdateUserSessionDto, UUID>(
    service = userSessionService,
    basePath = "/api/user-sessions",
    entityName = "sessions utilisateur",
    dtoClass = UserSessionDto::class.java
)

