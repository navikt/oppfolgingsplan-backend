package no.nav.syfo.util

import no.nav.syfo.util.PropertyUtil.ENVIRONMENT_NAME

object ToggleUtil {
    enum class ENVIRONMENT_MODE {
        dev,
        p,
        q0,
        q1,
    }

    fun kjorerLokalt(): Boolean {
        return System.getProperty(ENVIRONMENT_NAME, ENVIRONMENT_MODE.p.name)
            .equals(ENVIRONMENT_MODE.dev.name, ignoreCase = true)
    }
}