package com.varlor.backend.product.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.varlor.backend.common.model.SoftDeletableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "clients")
class Client(
    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: ClientType = ClientType.INDIVIDUAL,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: ClientStatus = ClientStatus.PENDING
) : SoftDeletableEntity<UUID>() {
    @JsonIgnore
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var users: MutableSet<User> = LinkedHashSet()
}

