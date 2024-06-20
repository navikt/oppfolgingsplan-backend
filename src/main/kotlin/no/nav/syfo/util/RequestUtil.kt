package no.nav.syfo.util

import java.util.*

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
const val APP_CONSUMER_ID = "srvoppfolgingsplanbackend"
const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val ORGNUMMER_HEADER = "orgnummer"
const val NAV_PERSONIDENT_HEADER = "nav-personident"

fun createCallId(): String = UUID.randomUUID().toString()
