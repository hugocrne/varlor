package com.varlor.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class VarlorBackendApplication

fun main(args: Array<String>) {
	runApplication<VarlorBackendApplication>(*args)
}
