package com.weatherengine.backend.location

import com.weatherengine.backend.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "locations")
class Location(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    var label: String? = null, // "Home", "Work", "London trip"

    @Column(nullable = false)
    var latitude: BigDecimal,

    @Column(nullable = false)
    var longitude: BigDecimal,

    @Column(name = "is_primary")
    var isPrimary: Boolean = false,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
)
