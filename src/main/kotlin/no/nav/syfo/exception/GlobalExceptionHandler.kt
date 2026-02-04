package no.nav.syfo.exception

import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.exceptions.JwtTokenInvalidClaimException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.brukertilgang.DependencyUnavailableException
import no.nav.syfo.logger
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = logger()

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception, request: HttpServletRequest): ResponseEntity<Any> {
        val responseStatus = AnnotatedElementUtils.findMergedAnnotation(ex.javaClass, ResponseStatus::class.java)
        val status: HttpStatus = when {
            ex is AbstractApiError -> {
                logError(ex)
                ex.httpStatus
            }

            ex is ResponseStatusException -> {
                val resolved = resolveStatus(ex.statusCode)
                logResponseStatus(resolved, ex.message)
                resolved
            }

            responseStatus != null -> {
                logResponseStatus(responseStatus.code, ex.message)
                responseStatus.code
            }

            ex is JwtTokenInvalidClaimException || ex is JwtTokenUnauthorizedException -> HttpStatus.UNAUTHORIZED
            ex is HttpMediaTypeNotAcceptableException -> HttpStatus.NOT_ACCEPTABLE
            else -> {
                log.error("Internal server error - ${ex.message} - ${request.method}: ${request.requestURI}", ex)
                HttpStatus.INTERNAL_SERVER_ERROR
            }
        }
        return ResponseEntity(ApiError(status.reasonPhrase), status)
    }

    private fun logError(ex: AbstractApiError) {
        when (ex.loglevel) {
            LogLevel.WARN -> {
                if (ex is DependencyUnavailableException) {
                    log.warn(ex.message)
                } else {
                    log.warn(ex.message, ex)
                }
            }
            LogLevel.ERROR -> log.error(ex.message, ex)
            LogLevel.OFF -> {
            }
        }
    }

    private fun logResponseStatus(status: HttpStatus, message: String?) {
        if (status.is5xxServerError) {
            log.warn("Request failed with status ${status.value()} ${status.reasonPhrase}: ${message ?: "no message"}")
        } else {
            log.debug("Request failed with status ${status.value()} ${status.reasonPhrase}: ${message ?: "no message"}")
        }
    }

    private fun resolveStatus(status: HttpStatusCode): HttpStatus {
        return HttpStatus.resolve(status.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
    }
}

private data class ApiError(val reason: String)

abstract class AbstractApiError(
    message: String,
    val httpStatus: HttpStatus,
    val reason: String,
    val loglevel: LogLevel,
    grunn: Throwable? = null
) : RuntimeException(message, grunn)

enum class LogLevel {
    WARN, ERROR, OFF
}
