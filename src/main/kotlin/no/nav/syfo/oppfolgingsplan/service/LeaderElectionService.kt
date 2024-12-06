package no.nav.syfo.oppfolgingsplan.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.domain.LeaderPod
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class LeaderElectionService(
    metrikk: Metrikk,
    @Qualifier("scheduler") restTemplateScheduler: RestTemplate,
    @Value("\${elector.path}") private val electorpath: String
) {
    private val metrikk: Metrikk = metrikk
    private val restTemplateScheduler: RestTemplate = restTemplateScheduler

    val isLeader: Boolean
        get() {
            if (isLocal) return false
            metrikk.tellHendelse("isLeader_kalt")
            val objectMapper =
                ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val url = "http://$electorpath"

            val response: String =
                restTemplateScheduler.getForObject<String>(url, String::class.java)

            try {
                val leader: LeaderPod = objectMapper.readValue(response, LeaderPod::class.java)
                return isHostLeader(leader)
            } catch (e: IOException) {
                log.error(
                    "Couldn't map response from electorPath to LeaderPod object",
                    e
                )
                metrikk.tellHendelse("isLeader_feilet")
                throw RuntimeException("Couldn't map response from electorpath to LeaderPod object", e)
            } catch (e: Exception) {
                log.error("Something went wrong when trying to check leader", e)
                metrikk.tellHendelse("isLeader_feilet")
                throw RuntimeException("Got exception when trying to find leader", e)
            }
        }

    @Throws(Exception::class)
    private fun isHostLeader(leader: LeaderPod): Boolean {
        val hostName: String = InetAddress.getLocalHost().getHostName()
        val leaderName: String = leader.getName()

        return hostName == leaderName
    }

    private val isLocal: Boolean
        get() = "true" == System.getProperty(LOCAL_MOCK)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LeaderElectionService::class.java)
    }
}
