package de.michaelzinn.playerservices.net

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import de.michaelzinn.playerservices.util.Ok
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import java.net.URL

class PlayerServiceClient {

    companion object {
        val playerServiceMediaType = "application/prs.playerservices-mc.v0+json".toMediaType()
    }

    val client = OkHttpClient()

    sealed class RegistrationError(val message: String) {
        class Unsuccessful(message: String) : RegistrationError(message)
        class WrongResponseCode(message: String) : RegistrationError(message)
        class Timeout(message: String) : RegistrationError(message)
    }

    // data class RequestError(val message: String)

    /**
     * Attempt to register a service with a user.
     */
    fun register(requestData: PlayerServiceRegistrationRequestBody): Result<Unit, RegistrationError> {
        val requestBody = Json.encodeToString(requestData).toRequestBody(playerServiceMediaType)

        val request = Request.Builder()
            .url("${requestData.serviceUrl}/registration")
            .put(requestBody)
            .build()

        try {
            return client.newCall(request).execute().use { response ->
                when {
                    !response.isSuccessful -> Err(
                        RegistrationError.Unsuccessful(
                            "Registration was not successful.\nCode: ${response.code}\nResponse:\n${response}"
                        )
                    )

                    response.code != HttpURLConnection.HTTP_CREATED -> Err(
                        RegistrationError.WrongResponseCode(
                            """
                        Protocol requires response code 201 (CREATED). Was ${response.code}.
                        Response:
                        ${response}
                        
                        Body:
                        ${response.body?.string()}
                        
                        Message:
                        ${response.message}
                        
                        Headers:
                        ${response.headers}
                        """.trimIndent()
                        )
                    )

                    else -> Ok()
                }
            }
        } catch (timeout: java.net.SocketTimeoutException) {
            return Err(RegistrationError.Timeout("Timeout: ${timeout.localizedMessage}"))
        }
    }

    fun privateRequest(): Result<String, String> = Err("Private requests are not implemented yet!")

    fun sharingRequest(url: String, requestBody: PlayerServiceRequestBody) = sharingRequest(URL(url), requestBody)
    fun sharingRequest(url: URL, requestBody: PlayerServiceRequestBody): Result<String, String> {
        val requestBodyJson = Json.encodeToString(requestBody).toRequestBody(playerServiceMediaType)

        val request = Request.Builder()
            .url("$url/command")
            .post(requestBodyJson)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body
                when {
                    response.isSuccessful && body != null -> Ok(body.string())
                    response.isSuccessful && body == null -> Err("Response contained no body!")
                    else -> Err("Unexpected code $response")
                }
            }
        } catch (timeout: java.net.SocketTimeoutException) {
            Err("Timeout: ${timeout.localizedMessage}")
        }
    }

}