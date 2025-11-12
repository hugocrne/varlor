package com.varlor.backend.common.extensions

import java.net.URI
import org.springframework.http.ResponseEntity

fun <T> T.toOkResponse(): ResponseEntity<T> = ResponseEntity.ok(this)

fun <T> List<T>.toOkResponse(): ResponseEntity<List<T>> = ResponseEntity.ok(this)

fun <T> T.toCreatedResponse(location: String): ResponseEntity<T> =
    ResponseEntity.created(URI.create(location)).body(this)

fun toNoContentResponse(): ResponseEntity<Void> = ResponseEntity.noContent().build()

