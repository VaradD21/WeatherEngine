package com.weatherengine.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WeatherEngineApplication

fun main(args: Array<String>) {
    runApplication<WeatherEngineApplication>(*args)
}
