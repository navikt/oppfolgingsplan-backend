package no.nav.syfo

//import no.nav.syfo.LocalApplication
import no.nav.syfo.util.TokenValidationTestUtil
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import javax.inject.Inject

/**
 * Hensikten her er å samle koden som mock svar fra istilgangskontroll.
 * Subklasser arver tilgangskontrollResponse, som de kan sette opp til å returnere 200 OK, 403 Forbidden eller
 * 500-feil.
 */
// alternative to @RunWith(SpringRunner::class) using Gradle
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
abstract class AbstractRessursTilgangTest {

    @Value("\${azure.openid.config.token.endpoint}")
    lateinit var azureTokenEndpoint: String

    @Value("\${istilgangskontroll.url}")
    lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var restTemplate: RestTemplate
    lateinit var mockRestServiceServer: MockRestServiceServer

    @Inject
    @Qualifier("restTemplateMedProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    @Inject
    lateinit var tokenValidationTestUtil: TokenValidationTestUtil

    @Before
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
    }

    @After
    open fun tearDown() {
        mockRestServiceServer.verify()
        mockRestServiceWithProxyServer.verify()
        tokenValidationTestUtil.logout()
    }
}
