package com.weatherengine.backend.location

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationRepository : JpaRepository<Location, UUID> {
    fun findByUserId(userId: UUID): List<Location>
}
