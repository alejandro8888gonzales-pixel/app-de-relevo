package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    fun isApiKeyAvailable(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "IA Desconectada: Configure su API Key en el panel de secretos de AI Studio para activar las funciones de Inteligencia Artificial."
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${getApiKey()}"

            // Build request JSON using JSONObject
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext "Error API (${code}): $errorBody"
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No se recibió texto.")
                        }
                    }
                }
                "No se obtuvo respuesta válida de la IA."
            }
        } catch (e: Throwable) {
            "Error completo al consultar la IA: ${e.localizedMessage ?: e.message ?: e.toString()}"
        }
    }

    suspend fun generateChatResponse(history: List<ChatMessage>, systemInstruction: String? = null, bitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "IA Desconectada: Configure su API Key en el panel de secretos de AI Studio para activar las funciones de Inteligencia Artificial."
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${getApiKey()}"

            val root = JSONObject()
            val contentsArray = JSONArray()

            // Keep only the last 30 messages to avoid token overflow
            val truncatedHistory = if (history.size > 34) history.takeLast(30) else history

            truncatedHistory.forEachIndexed { index, msg ->
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.isUser) "user" else "model")
                val partsArray = JSONArray()
                
                val partObj = JSONObject()
                partObj.put("text", msg.text)
                partsArray.put(partObj)

                // Append image data to the last user message if present
                if (msg.isUser && index == truncatedHistory.lastIndex && bitmap != null) {
                    try {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                        val byteArray = stream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                        val partImageObj = JSONObject()
                        val inlineDataObj = JSONObject()
                        inlineDataObj.put("mimeType", "image/jpeg")
                        inlineDataObj.put("data", base64String)
                        partImageObj.put("inlineData", inlineDataObj)
                        partsArray.put(partImageObj)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            root.put("contents", contentsArray)

            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext "Error API (${code}): $errorBody"
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No se recibió texto.")
                        }
                    }
                }
                "No se obtuvo respuesta válida de la IA."
            }
        } catch (e: Throwable) {
            "Error al consultar la IA: ${e.localizedMessage ?: e.message ?: e.toString()}"
        }
    }

    suspend fun analyzeCertificate(bitmap: Bitmap): OcrResult = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext OcrResult(
                success = false,
                error = "API Key no configurada",
                titulo = "Falta Llave de API",
                institucion = "Desconocida",
                fecha = "2026-06-05",
                horas = 0.0,
                notas = "Ingrese su GEMINI_API_KEY en los secretos para un escaneo OCR automático con Inteligencia Artificial."
            )
        }

        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
            val byteArray = stream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val systemPrompt = """
                Eres un escáner OCR inteligente especializado en diplomas, constancias escolares y certificados para estudiantes hondureños. 
                Estudia meticulosamente la imagen adjunta. Extrae la información y devuélvela exclusivamente en formato JSON con los siguientes campos:
                - "titulo": El nombre o tema del taller, diploma, conferencia, voluntariado o curso.
                - "institucion": Quién emitió el documento (ejemplo: Cruz Roja, Techo, Instituto, Colegio, CEUTEC, UNITEC, UNAH, etc.).
                - "fecha": Fecha del documento en formato YYYY-MM-DD. Si no se puede precisar, deduce una aproximada o escribe "".
                - "horas": Las horas asignadas (formato número ej: 10.0 o 40.0). Busca leyendas que digan 'horas', 'hrs', 'validez por x horas'. Si no las hay, responde con 0.0.
                - "notas": Un resumen breve de 1 línea sobre lo que dice el documento.

                Responde únicamente con el bloque JSON crudo. No incluyas bloques ```json o texto explicativo adicional.
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${getApiKey()}"

            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Part 1: System prompt Text
            val partTextObj = JSONObject()
            partTextObj.put("text", systemPrompt)
            partsArray.put(partTextObj)

            // Part 2: InlineData base64 image
            val partImageObj = JSONObject()
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", "image/jpeg")
            inlineDataObj.put("data", base64String)
            partImageObj.put("inlineData", inlineDataObj)
            partsArray.put(partImageObj)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // Generation config with JSON response type
            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            root.put("generationConfig", generationConfig)

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext OcrResult(
                        success = false,
                        error = "API Error ${response.code}",
                        titulo = "Error de Lectura",
                        institucion = "Reintente",
                        fecha = "2026-06-05",
                        horas = 0.0,
                        notas = "El servidor Gemini devolvió error (${response.code})."
                    )
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                var jsonText = ""
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            jsonText = parts.getJSONObject(0).optString("text", "")
                        }
                    }
                }

                if (jsonText.isEmpty()) {
                    return@withContext OcrResult(
                        success = false,
                        error = "Respuesta vacía",
                        titulo = "Error",
                        institucion = "Desconocida",
                        fecha = "2026-06-05",
                        horas = 0.0,
                        notas = "La IA no pudo procesar la imagen."
                    )
                }

                // Clean response structure from extra Markdown fences if any
                val cleanJson = jsonText.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val obj = JSONObject(cleanJson)
                OcrResult(
                    success = true,
                    titulo = obj.optString("titulo", "Certificado sin nombre"),
                    institucion = obj.optString("institucion", "Institución Desconocida"),
                    fecha = obj.optString("fecha", "2026-06-05"),
                    horas = obj.optDouble("horas", 0.0),
                    notas = obj.optString("notas", "Extraído autómata")
                )
            }
        } catch (e: Throwable) {
            OcrResult(
                success = false,
                error = e.localizedMessage ?: e.message ?: e.toString(),
                titulo = "Error de Lectura",
                institucion = "Reintente",
                fecha = "2026-06-05",
                horas = 0.0,
                notas = "No se pudo realizar el OCR por un error de conexión o formato: ${e.localizedMessage}"
            )
        }
    }
}

data class OcrResult(
    val success: Boolean,
    val error: String? = null,
    val titulo: String,
    val institucion: String,
    val fecha: String,
    val horas: Double,
    val notas: String
)
