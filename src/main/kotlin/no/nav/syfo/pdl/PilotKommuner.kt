package no.nav.syfo.pdl

/**
 * Pilotkommuner (kommunenummer) som har tilgang.
 */
object PilotKommuner {
    private val pilotKommunenr: Set<String> = linkedSetOf(
        // Vestland – Region Sunnhordland
        "4614", // Stord
        "4613", // Bømlo
        "4615", // Fitjar
        "4617", // Kvinnherad
        "4612", // Sveio
        "4616", // Tysnes

        // Vestland – Region Nordfjord
        "4650", // Gloppen
        "4651", // Stryn
        "4648", // Bremanger
        "4649", // Stad

        // Vestland – Region Midthordland
        "4625", // Austevoll
        "4624", // Bjørnafjorden
        "4623", // Samnanger

        // Vestland – Region Nordhordland
        "4630", // Osterøy
        "4631", // Alver
        "4634", // Masfjorden
        "4629", // Modalen
        "4635", // Gulen
        "4632", // Austrheim
        "4636", // Solund
        "4633", // Fedje

        // Vestland – Region Voss/Hardanger
        "4618", // Ullensvang
        "4619", // Eidfjord
        "4620", // Ulvik
        "4621", // Voss
        "4628", // Vaksdal
        "4622", // Kvam

        // Vestland – Region Sunnfjord
        "4647", // Sunnfjord
        "4645", // Askvoll
        "4602", // Kinn
        "4646", // Fjaler
        "4637", // Hyllestad
        "4638", // Høyanger

        // Vestland – Region Bergen
        "4601", // Bergen

        // Vestland – Region Vest
        "4627", // Askøy
        "4626", // Øygarden

        // Vestland – Region Sogn
        "4641", // Aurland
        "4642", // Lærdal
        "4643", // Årdal
        "4644", // Luster
        "4640", // Sogndal
        "4639", // Vik
    )

    fun erPilot(kommunenr: String?): Boolean = kommunenr != null && kommunenr in pilotKommunenr
}
