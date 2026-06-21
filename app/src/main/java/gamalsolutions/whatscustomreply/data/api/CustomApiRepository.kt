package gamalsolutions.whatscustomreply.data.api

import android.util.Log
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CustomApiRepository(
    private val httpClient: OkHttpClient
) {
    private val TAG = "CustomApiRepository"

    suspend fun generateReply(
        apiUrl: String,
        apiMethod: String,
        apiHeaders: String,
        apiBodyTemplate: String,
        apiResponsePath: String,
        sender: String,
        message: String
    ): Result<String> {
        if (apiUrl.isBlank()) {
            return Result.failure(Exception("API Endpoint URL is empty. Please configure it in API Settings."))
        }

        // 1. Resolve URL with optional placeholders
        var finalUrl = apiUrl
            .replace("{sender}", java.net.URLEncoder.encode(sender, "UTF-8"))
            .replace("{message}", java.net.URLEncoder.encode(message, "UTF-8"))

        val isPost = apiMethod.trim().uppercase() == "POST"

        if (!isPost) {
            // Append as query parameters if placeholder was not explicitly used in GET URL
            if (!apiUrl.contains("{sender}") && !apiUrl.contains("{message}")) {
                val separator = if (finalUrl.contains("?")) "&" else "?"
                finalUrl += "${separator}sender=${java.net.URLEncoder.encode(sender, "UTF-8")}&message=${java.net.URLEncoder.encode(message, "UTF-8")}"
            }
        }

        // 2. Build Headers
        val headersBuilder = Headers.Builder()
        apiHeaders.lines().forEach { line ->
            if (line.contains(":")) {
                val parts = line.split(":", limit = 2)
                val key = parts[PartLayoutSpec.KEY_INDEX]?.trim() ?: ""
                val value = parts.getOrNull(1)?.trim() ?: ""
                if (key.isNotEmpty()) {
                    headersBuilder.add(key, value)
                }
            }
        }
        val requestHeaders = headersBuilder.build()

        // 3. Build Request Body if POST
        var requestBody: RequestBody? = null
        if (isPost) {
            val resolvedBody = apiBodyTemplate
                .replace("{sender}", sender)
                .replace("{message}", message)
            
            // Guess media type (default JSON, fallback if user customized)
            val contentType = requestHeaders["Content-Type"] ?: "application/json; charset=utf-8"
            requestBody = resolvedBody.toRequestBody(contentType.toMediaType())
        }

        // 4. Create HTTP request
        val request = Request.Builder().apply {
            url(finalUrl)
            headers(requestHeaders)
            if (isPost) {
                post(requestBody!!)
            } else {
                get()
            }
        }.build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val responseString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP Error $code: $responseString".take(200)))
                }

                // 2. Extract Response Text
                var extractedText: String? = null
                if (apiResponsePath.isNotBlank()) {
                    extractedText = getJsonKeyValue(responseString, apiResponsePath)
                }

                // Default dynamic guess loops
                if (extractedText == null) {
                    try {
                        val element = Json.parseToJsonElement(responseString)
                        if (element is JsonObject) {
                            val guesses = listOf("reply", "response", "text", "message", "output", "content")
                            for (guess in guesses) {
                                val matched = element[guess]
                                if (matched is JsonPrimitive) {
                                    extractedText = matched.content
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Response is not structured JSON
                    }
                }

                // Final fallback is plain content response itself
                if (extractedText == null) {
                    extractedText = responseString
                }

                if (extractedText.isNotBlank()) {
                    Result.success(extractedText.trim())
                } else {
                    Result.failure(Exception("Extract failed: Returned response text is empty."))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network exception invoking custom API", e)
            Result.failure(Exception("Connection failed! Verify your internet connection and API Endpoint URL. Error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "General exception invoking custom API", e)
            Result.failure(Exception("API Error: ${e.message}"))
        }
    }

    private fun getJsonKeyValue(jsonStr: String, pathSpec: String): String? {
        return try {
            val element = Json.parseToJsonElement(jsonStr)
            var current: JsonElement = element
            val parts = pathSpec.split(".")
            for (part in parts) {
                if (current is JsonObject) {
                    current = current[part] ?: return null
                } else if (current is JsonArray) {
                    val idx = part.toIntOrNull() ?: return null
                    if (idx in 0 until current.size) {
                        current = current[idx]
                    } else return null
                } else return null
            }
            if (current is JsonPrimitive) {
                current.content
            } else {
                current.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private object PartLayoutSpec {
        const val KEY_INDEX = 0
    }
}
