package com.respiroc.gregfullstack

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@SpringBootApplication
@EnableScheduling
class GregFullstackApplication {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(GregFullstackApplication::class.java, *args)
        }
    }
}
