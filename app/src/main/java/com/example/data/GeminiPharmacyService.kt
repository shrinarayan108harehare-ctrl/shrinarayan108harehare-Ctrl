package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class AiImageResolution(val label: String, val size: ImageSize) {
    RES_1K("1K", ImageSize.SIZE_1K),
    RES_2K("2K", ImageSize.SIZE_2K),
    RES_4K("4K", ImageSize.SIZE_4K)
}

data class AiChatTurn(
    val isUser: Boolean,
    val text: String,
    val usedHighThinking: Boolean = false,
    val groundingNote: String = ""
)

class GeminiPharmacyService {

    companion object {
        private const val TAG = "GeminiPharmacyService"
        private const val TIMEOUT_MS = 60_000L
    }

    private val assistantSystemInstruction = content {
        text(
            """
            You are the official Pharmacy & Wellness Information Assistant for PRAYAG DEEN & SONS Medical.
            Help customers understand medicines, generic alternatives, general usage precautions, storage instructions, and nearby healthcare clinics/pharmacies.
            Always support both English and Hindi clearly depending on the user's language.
            CRITICAL SAFETY RULE: Never diagnose diseases or prescribe Schedule H/H1 prescription-only medicines.
            Always remind customers that prescription-only medicines require a valid doctor's prescription verified by a licensed Prayag Deen & Sons pharmacist/manager before dispatch.
            """.trimIndent()
        )
    }

    private val searchAndMapsChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-hydrogen",
            tools = listOf(Tool.googleSearch(), Tool.googleMaps()),
            systemInstruction = assistantSystemInstruction
        )
    }

    private val deepThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = assistantSystemInstruction
        )
    }

    private val fastFlashLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )
    }

    private val prescriptionVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-hydrogen",
            systemInstruction = content {
                text(
                    "You assist licensed pharmacy managers by reading uploaded prescription photos. " +
                        "List visible medicine names, strengths, and quantities clearly. " +
                        "Mark unclear handwriting as [UNCLEAR - VERIFY MANUALLY]. " +
                        "Always state that final verification by the manager pharmacist is mandatory."
                )
            }
        )
    }

    private val biThinkingAnalystModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    fun streamPharmacistChat(
        historyTurns: List<AiChatTurn>,
        userPrompt: String,
        useHighThinking: Boolean
    ): Flow<String> = flow {
        val model = if (useHighThinking) deepThinkingModel else searchAndMapsChatModel
        val history: List<Content> = historyTurns.map { turn ->
            content(if (turn.isUser) "user" else "model") { text(turn.text) }
        }
        val chat = model.startChat(history = history)
        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }

    suspend fun extractPrescriptionDraft(bitmap: Bitmap): Result<String> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                prescriptionVisionModel.generateContent(
                    content {
                        image(bitmap)
                        text("Extract all medicine names, dosages, and quantities from this prescription image for pharmacist review.")
                    }
                )
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("No readable prescription text extracted."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Prescription extraction timed out", e)
            Result.failure(Exception("Prescription analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prescription extraction failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateOrSuggestReply(
        messageText: String,
        targetLang: String
    ): Result<String> {
        return try {
            val prompt = "Translate or craft a polite, concise pharmacy manager reply in $targetLang for this customer message:\n\"$messageText\""
            val response = withTimeout(35_000L) {
                fastFlashLiteModel.generateContent(prompt)
            }
            val text = response.text
            if (text.isNullOrBlank()) Result.failure(IllegalStateException("Empty reply suggestion."))
            else Result.success(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fast reply generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeBusinessIntelligence(summaryPrompt: String): Result<String> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                biThinkingAnalystModel.generateContent(
                    "You are a senior retail pharmacy business intelligence analyst for Prayag Deen & Sons. " +
                        "Analyze these exact metrics and provide actionable margin, area demand, and delivery efficiency recommendations:\n$summaryPrompt"
                )
            }
            val text = response.text
            if (text.isNullOrBlank()) Result.failure(IllegalStateException("BI analyst returned empty insight."))
            else Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Deep BI analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "BI analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun generatePromoBanner(
        prompt: String,
        resolution: AiImageResolution
    ): Result<Bitmap> {
        return try {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(resolution.size)
                    .build()
            }
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3-pro-image-preview",
                generationConfig = config
            )
            val response = withTimeout(TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            val bmp = imagePart?.image
            if (bmp != null) {
                Result.success(bmp)
            } else {
                Result.failure(IllegalStateException("No image returned by Gemini image model."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Banner generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }
}
