package com.piania.app.data


import android.util.Base64
import org.json.JSONObject


object JwtUtils {

    private fun decodePayload(jwt: String?): JSONObject? {
        if (jwt.isNullOrBlank()) return null
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null

            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            )
            JSONObject(payload)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodifica el claim "role" del JWT (sin validar firma).
     * En el backend nuevo el access token incluye: claim "role" = USER / TEACHER / ADMIN...
     */
    fun extractRole(jwt: String?): String? {
        val json = decodePayload(jwt) ?: return null
        return json.optString("role").takeIf { it.isNotBlank() && it != "null" }
    }

    /**
     * Email/username del usuario autenticado.
     * Normalmente viene en el claim estándar "sub". Si el backend usa otro claim (p.ej. "email"),
     * hacemos fallback.
     */
    fun getEmail(jwt: String?): String? {
        val json = decodePayload(jwt) ?: return null
        val sub = json.optString("sub").takeIf { it.isNotBlank() && it != "null" }
        if (!sub.isNullOrBlank()) return sub
        return json.optString("email").takeIf { it.isNotBlank() && it != "null" }
    }

    fun isTeacher(jwt: String?): Boolean = extractRole(jwt) == "TEACHER"
}
