package no.nav.syfo.brukertilgang

data class RSTilgang(
    val harTilgang: Boolean,
    val ikkeTilgangGrunn: String? = null
)
