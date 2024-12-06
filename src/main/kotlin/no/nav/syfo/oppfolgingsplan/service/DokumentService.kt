package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.repository.dao.DokumentDAO
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class DokumentService @Inject constructor(dokumentDAO: DokumentDAO) {
    private val dokumentDAO: DokumentDAO = dokumentDAO

    fun hentDokument(dokumentUuid: String?): ByteArray {
        return dokumentDAO.hent(dokumentUuid)
    }
}
