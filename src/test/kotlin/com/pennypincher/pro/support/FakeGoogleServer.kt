package com.pennypincher.pro.support

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder

/**
 * A local stand-in for Google's token and JWKS endpoints, built on the JDK HTTP server, so the real
 * HTTP adapter can be tested without reaching the internet.
 */
class FakeGoogleServer(private val tokens: GoogleIdTokens) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress(LOOPBACK, 0), 0)

    @Volatile var tokenStatus: Int = OK

    @Volatile var tokenBody: String = ""

    @Volatile var lastTokenForm: Map<String, String> = emptyMap()

    @Volatile var tokenCalls: Int = 0

    @Volatile var certsStatus: Int = OK

    val tokenUri: String get() = "${baseUrl()}/token"
    val jwkSetUri: String get() = "${baseUrl()}/certs"

    init {
        server.createContext("/token", ::handleToken)
        server.createContext("/certs") { exchange -> respond(exchange, certsStatus, tokens.jwkSetJson) }
        server.start()
    }

    fun respondWithIdToken(idToken: String) {
        tokenStatus = OK
        tokenBody = """{"access_token":"ya29.fake","id_token":"$idToken","expires_in":3599,"token_type":"Bearer"}"""
    }

    fun respondWithError(status: Int, error: String = "invalid_grant") {
        tokenStatus = status
        tokenBody = """{"error":"$error","error_description":"Bad Request"}"""
    }

    override fun close() = server.stop(0)

    private fun handleToken(exchange: HttpExchange) {
        tokenCalls++
        lastTokenForm = parseForm(exchange.requestBody.readAllBytes().decodeToString())
        respond(exchange, tokenStatus, tokenBody)
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun parseForm(form: String): Map<String, String> = form.split("&").filter(String::isNotEmpty).associate {
        val (key, value) = it.split("=", limit = 2)
        decode(key) to decode(value)
    }

    private fun decode(value: String) = URLDecoder.decode(value, Charsets.UTF_8)

    private fun baseUrl() = "http://$LOOPBACK:${server.address.port}"

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val OK = 200
    }
}
