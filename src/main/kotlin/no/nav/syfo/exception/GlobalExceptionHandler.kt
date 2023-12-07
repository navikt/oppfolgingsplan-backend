package no.nav.syfo.exception

import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.exceptions.JwtTokenInvalidClaimException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = logger()

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception, request: HttpServletRequest): ResponseEntity<Any> {
        val status = when (ex) {
            is AbstractApiError -> {
                logError(ex)
                ex.httpStatus
            }

            is JwtTokenInvalidClaimException, is JwtTokenUnauthorizedException -> HttpStatus.UNAUTHORIZED
            is HttpMediaTypeNotAcceptableException -> HttpStatus.NOT_ACCEPTABLE
            else -> {
                log.error("Internal server error - ${ex.message} - ${request.method}: ${request.requestURI}", ex)
                HttpStatus.INTERNAL_SERVER_ERROR
            }
        }
        return ResponseEntity(ApiError(status.reasonPhrase), status)
    }

    private fun logError(ex: AbstractApiError) {
        when (ex.loglevel) {
            LogLevel.WARN -> log.warn(ex.message, ex)
            LogLevel.ERROR -> log.error(ex.message, ex)
            LogLevel.OFF -> {
            }
        }
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
