package com.pennypincher.pro.support

import com.jayway.jsonpath.JsonPath
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/** Integration-test base for the authenticated REST API: creates users and signs requests as them. */
abstract class ApiIntegrationTest : IntegrationTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var accessTokenIssuer: AccessTokenIssuer

    /** Stores a new user with a unique Google subject and e-mail. */
    protected fun createUser(): User {
        val suffix = UUID.randomUUID()
        val profile = IdentityProfile("api-$suffix@example.com", "Api User", null)
        return userRepository.save(User.register("api-sub-$suffix", profile, AuthFixtures.NOW))
    }

    /** Adds a valid access token for [user]. */
    protected fun MockHttpServletRequestDsl.bearer(user: User) {
        header(HttpHeaders.AUTHORIZATION, "Bearer ${accessTokenIssuer.issue(user).value}")
    }

    /** Sends [body] as JSON. */
    protected fun MockHttpServletRequestDsl.json(body: String) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    protected fun postJson(user: User, path: String, body: String): ResultActionsDsl = mockMvc.post(path) {
        bearer(user)
        json(body)
    }

    protected fun putJson(user: User, path: String, body: String): ResultActionsDsl = mockMvc.put(path) {
        bearer(user)
        json(body)
    }

    protected fun deleteAs(user: User, path: String): ResultActionsDsl = mockMvc.delete(path) { bearer(user) }

    /** The `data.id` of a successful response. */
    protected fun idOf(result: ResultActionsDsl): UUID =
        UUID.fromString(JsonPath.read(result.andReturn().response.contentAsString, "$.data.id"))
}
