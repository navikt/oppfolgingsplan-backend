package no.nav.syfo.oppfolgingsplan.service

import jakarta.ws.rs.ForbiddenException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import javax.inject.Inject

@Service
class PdfService(
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val oppfolgingsplanService: OppfolgingsplanService,
    dokumentDAO: DokumentDAO,
    godkjentplanDAO: GodkjentplanDAO,
    metrikk: Metrikk
) {
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO

    private val dokumentDAO: DokumentDAO = dokumentDAO

    private val godkjentplanDAO: GodkjentplanDAO = godkjentplanDAO

    private val metrikk: Metrikk = metrikk

    private val pngDpiResolution = 150.0f

    private val pngImageType: ImageType = ImageType.RGB

    fun hentPdf(oppfolgingsplanId: Long, innloggetFnr: String?): ByteArray {
        if (!oppfolgingsplanService.harBrukerTilgangTilDialog(oppfolgingsplanId, innloggetFnr)) {
            throw ForbiddenException("Ikke tilgang")
        }
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        metrikk.tellAntallDagerSiden(oppfolgingsplan.opprettet, "antallDagerFraOpprettetTilPdf")
        val godkjentPlanOptional: Optional<GodkjentPlan> =
            godkjentplanDAO.godkjentPlanByOppfolgingsplanId(oppfolgingsplanId)
        if (godkjentPlanOptional.isPresent()) {
            val dokumentUuid: String = godkjentPlanOptional.get().dokumentUuid
            return dokumentDAO.hent(dokumentUuid)
        } else {
            metrikk.tellHendelse("hent_pdf_missing_godkjentplan")
            log.error("Did not find PDF due to missing GodkjentPlan for plan {}", oppfolgingsplanId)
            throw RuntimeException("Did not find PDF due to missing GodkjentPlan for plan")
        }
    }

    fun hentPdfTilAltinn(oppfolgingsplan: Oppfolgingsplan): ByteArray {
        metrikk.tellAntallDagerSiden(oppfolgingsplan.opprettet, "antallDagerFraOpprettetTilPdf")

        val godkjentPlan: GodkjentPlan = oppfolgingsplan.godkjentPlan.orElseThrow {
            throwOppfoelgingsplanUtenGodkjenPlan(
                oppfolgingsplan
            )
        }

        return dokumentDAO.hent(godkjentPlan.dokumentUuid)
    }

    fun pdf2image(pdfBytes: ByteArray, side: Int): ByteArray {
        try {
            PDDocument.load(ByteArrayInputStream(pdfBytes)).use { document ->
                val pdfRenderer: PDFRenderer = PDFRenderer(document)
                val image: BufferedImage = pdfRenderer.renderImageWithDPI(side - 1, pngDpiResolution, pngImageType)
                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIOUtil.writeImage(image, "png", byteArrayOutputStream)
                return byteArrayOutputStream.toByteArray()
            }
        } catch (e: IOException) {
            log.error("Fikk feil ved konvertering fra PDF til PNG")
            throw RuntimeException("Klarte ikke å konvertere fra PDF til PNG")
        }
    }

    fun hentAntallSiderIDokument(pdf: ByteArray): Int {
        val `is`: InputStream = ByteArrayInputStream(pdf)
        try {
            val document: PDDocument = PDDocument.load(`is`)
            val antallSider: Int = document.getNumberOfPages()
            document.close()
            return antallSider
        } catch (e: IOException) {
            log.error("Catched IOException when get number of pages of document", e)
            return 1
        }
    }

    private fun throwOppfoelgingsplanUtenGodkjenPlan(oppfolgingsplan: Oppfolgingsplan): RuntimeException {
        log.error("Oppfoelgingsplan med id {} har ikke godkjentPlan", oppfolgingsplan.id)
        return RuntimeException("Oppfoelgingsplan har ikke godkjentPlan")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PdfService::class.java)
    }
}
