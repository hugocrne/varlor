package com.varlor.backend.product.repository

import com.varlor.backend.common.repository.SoftDeleteRepository
import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import com.varlor.backend.product.model.entity.Client
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
interface ClientRepository : SoftDeleteRepository<Client, UUID>, SoftDeleteRepositoryMethods<Client, UUID>

