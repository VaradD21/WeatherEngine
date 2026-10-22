package com.weatherengine.backend.location

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets

class DatabaseIndexesTest {

    @Test
    fun `schema sql contains idempotent user_id indexes for all user owned tables`() {
        val resource = ClassPathResource("schema.sql")
        val content = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

        assertTrue(
            content.contains("CREATE INDEX IF NOT EXISTS idx_locations_user_id ON locations(user_id);"),
            "schema.sql must contain idx_locations_user_id"
        )
        assertTrue(
            content.contains("CREATE INDEX IF NOT EXISTS idx_alert_log_user_id ON alert_log(user_id);"),
            "schema.sql must contain idx_alert_log_user_id"
        )
        assertTrue(
            content.contains("CREATE INDEX IF NOT EXISTS idx_alert_subscriptions_user_id ON alert_subscriptions(user_id);"),
            "schema.sql must contain idx_alert_subscriptions_user_id"
        )
        assertTrue(
            content.contains("ON DELETE CASCADE"),
            "User-owned tables must have ON DELETE CASCADE"
        )
    }
}
