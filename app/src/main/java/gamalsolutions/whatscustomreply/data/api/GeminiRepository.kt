package gamalsolutions.whatscustomreply.data.api

import android.util.Log
import gamalsolutions.whatscustomreply.data.security.EncryptedPrefsManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GeminiRepository(
    private val apiService: GeminiApiService,
    private val encryptedPrefs: EncryptedPrefsManager
) {
    suspend fun generateReply(
        prompt: String,
        systemPrompt: String,
        model: String = "gemini-2.5-flash"
    ): Result<String> {
        val userSavedKey = encryptedPrefs.getGeminiApiKey()
        val apiKey = if (userSavedKey.isNotBlank()) {
            userSavedKey
        } else {
            try {
                val buildConfigKey = gamalsolutions.whatscustomreply.BuildConfig.GEMINI_API_KEY
                if (buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.isNotBlank()) {
                    buildConfigKey
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }

        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API key is not configured. Please enter your API key in Gemini Settings."))
        }

        val modelPath = when (model) {
            "gemini-2.5-flash" -> "gemini-2.5-flash"
            "gemini-3.5-flash" -> "gemini-3.5-flash"
            else -> model
        }

        // Construct Request JSON
        val systemInstructionBlock = if (systemPrompt.isNotBlank()) {
            """
            "systemInstruction": {
              "parts": [
                {
                  "text": ${Json.encodeToString(systemPrompt)}
                }
              ]
            },
            """.trimIndent()
        } else ""

        val jsonRequestString = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": ${Json.encodeToString(prompt)}
                }
              ]
            }
          ],
          $systemInstructionBlock
          "generationConfig": {
            "temperature": 0.5
          }
        }
        """.trimIndent()

        val requestBody = jsonRequestString.toRequestBody("application/json; charset=utf-8".toMediaType())

        return try {
            val responseBody = apiService.generateContent(modelPath, apiKey, requestBody)
            val responseString = responseBody.string()

            val jsonElement = Json.parseToJsonElement(responseString).jsonObject
            val candidates = jsonElement["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val firstPart = parts?.firstOrNull()?.jsonObject
            val text = firstPart?.get("text")?.jsonPrimitive?.content

            if (text != null) {
                Result.success(text.trim())
            } else {
                Result.failure(Exception("No reply output candidate from Gemini API."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Please verify your internet connection. Detail: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("API Error: ${e.message}"))
        }
    }
}
