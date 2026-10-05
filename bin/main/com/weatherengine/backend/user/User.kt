package com.weatherengine.backend.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
)
