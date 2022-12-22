package de.fhg.aisec.ids.webconsole

import org.springframework.web.bind.annotation.RestController

@RestController
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiController
