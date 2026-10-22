package com.pennypincher.pro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PennyPincherProApplication

fun main(args: Array<String>) {
    runApplication<PennyPincherProApplication>(*args)
}
