package de.fhg.aisec.ids.webconsole

import de.fhg.aisec.ids.webconsole.api.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.ws.rs.ApplicationPath

@Controller
class Controller {
    @RequestMapping("/")
    fun c(model: Map<String?, Any?>?): String {
        //return "index.html"
        return "/www/src/app/login/login.component.html"
    }
}