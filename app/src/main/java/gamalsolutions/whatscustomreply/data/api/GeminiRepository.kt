package gamalsolutions.whatscustomreply.data.api

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

class GeminiRepository(
    private val httpClient: OkHttpClient
) {
    private val TAG = "GeminiRepository"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateReply(
        apiKey: String,
        systemInstruction: String,
        message: String
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key is empty. Please configure it in Gemini Settings."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = message)))
            ),
            systemInstruction = if (systemInstruction.isNotBlank()) {
                GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
            } else null
        )

        val jsonString = json.encodeToString(GeminiRequest.serializer(), requestBody)
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val responseString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP Error $code: $responseString".take(250)))
                }

                // Parse response
                val jsonResponse = json.parseToJsonElement(responseString)
                val candidates = jsonResponse.jsonObject["candidates"]?.jsonArray
                val firstCandidate = candidates?.getOrNull(0)?.jsonObject
                val content = firstCandidate?.get("content")?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                val firstPart = parts?.getOrNull(0)?.jsonObject
                val text = firstPart?.get("text")?.jsonPrimitive?.content

                if (text != null) {
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Extract failed: Could not find reply text in Gemini response."))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network exception calling Gemini API", e)
            Result.failure(Exception("Connection failed! Verify your internet connection. Error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "General exception calling Gemini API", e)
            Result.failure(Exception("Gemini API Error: ${e.message}"))
        }
    }
}
