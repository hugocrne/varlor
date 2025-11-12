package com.varlor.backend.common.service

import java.io.Serializable

interface CrudService<DTO, CREATE_DTO, UPDATE_DTO, ID : Serializable> {
    fun findAll(): List<DTO>
    fun findById(id: ID): DTO
    fun create(dto: CREATE_DTO): DTO
    fun update(id: ID, dto: UPDATE_DTO): DTO
    fun delete(id: ID)
}

