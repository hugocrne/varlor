package com.varlor.backend.product.controller

import com.varlor.backend.common.controller.BaseCrudController
import com.varlor.backend.product.model.dto.CreateUserDto
import com.varlor.backend.product.model.dto.UpdateUserDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.service.UserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'OWNER', 'SERVICE')")
class UserController(
    userService: UserService
) : BaseCrudController<UserDto, CreateUserDto, UpdateUserDto, UUID>(
    service = userService,
    basePath = "/api/users",
    entityName = "utilisateurs",
    dtoClass = UserDto::class.java
) {
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    override fun create(@Valid @RequestBody dto: CreateUserDto) = super.create(dto)
    
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    override fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateUserDto
    ) = super.update(id, dto)
}

