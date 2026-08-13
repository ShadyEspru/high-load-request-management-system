package com.hlrms.mobile.data.remote

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val authApiProvider: () -> AuthApi
) : Authenticator {

    private val lock =
        Any()

    override fun authenticate(
        route: Route?,
        response: Response
    ): Request? {

        if (responseCount(response) >= 2) {
            return null
        }

        return synchronized(lock) {

            runBlocking {

                val currentAccessToken =
                    sessionManager.accessToken.first()
                        ?: return@runBlocking null

                val requestAccessToken =
                    response.request
                        .header("Authorization")
                        ?.removePrefix("Bearer ")
                        ?.trim()

                /*
                 * ربما طلب آخر سبقنا وقام بعمل refresh.
                 * عندها لا نرسل refresh ثانيًا.
                 */
                if (
                    requestAccessToken != null &&
                    requestAccessToken != currentAccessToken
                ) {

                    return@runBlocking response.request
                        .newBuilder()
                        .header(
                            "Authorization",
                            "Bearer $currentAccessToken"
                        )
                        .build()
                }

                val refreshToken =
                    sessionManager.refreshToken.first()
                        ?: return@runBlocking null

                try {

                    val authResponse =
                        authApiProvider()
                            .refresh(
                                RefreshTokenRequest(
                                    refreshToken =
                                        refreshToken
                                )
                            )

                    sessionManager.saveSession(
                        accessToken =
                            authResponse.accessToken,

                        refreshToken =
                            authResponse.refreshToken
                    )

                    response.request
                        .newBuilder()
                        .header(
                            "Authorization",
                            "Bearer ${authResponse.accessToken}"
                        )
                        .build()

                } catch (_: Exception) {

                    sessionManager.clearSession()

                    null
                }
            }
        }
    }

    private fun responseCount(
        response: Response
    ): Int {

        var count = 1

        var priorResponse =
            response.priorResponse

        while (priorResponse != null) {

            count++

            priorResponse =
                priorResponse.priorResponse
        }

        return count
    }
}