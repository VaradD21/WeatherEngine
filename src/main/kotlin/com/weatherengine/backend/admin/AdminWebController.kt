package com.weatherengine.backend.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminWebController {

    @GetMapping("/admin", "/admin/")
    fun adminDashboard(): String = "forward:/admin/index.html"
}
