package br.com.leitorcuponsfinancas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface ReceiptOcrReadResult {
    data class Success(val text: String) : ReceiptOcrReadResult
    data class Error(val message: String) : ReceiptOcrReadResult
}

object ReceiptOcrReader {
    suspend fun read(context: Context, uri: Uri): ReceiptOcrReadResult = try {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime == "application/pdf") readPdf(context, uri) else recognize(InputImage.fromFilePath(context, uri))
    } catch (error: Exception) {
        ReceiptOcrReadResult.Error("Não foi possível ler o arquivo: ${error.message ?: error::class.java.simpleName}")
    }

    suspend fun read(bitmap: Bitmap): ReceiptOcrReadResult =
        recognize(InputImage.fromBitmap(bitmap, 0))

    private suspend fun readPdf(context: Context, uri: Uri): ReceiptOcrReadResult {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return ReceiptOcrReadResult.Error("Não foi possível abrir o PDF.")
        descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount == 0) return ReceiptOcrReadResult.Error("O PDF não possui páginas.")
                val texts = mutableListOf<String>()
                for (index in 0 until renderer.pageCount.coerceAtMost(10)) {
                    renderer.openPage(index).use { page ->
                        val scale = 2
                        val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        when (val result = recognize(InputImage.fromBitmap(bitmap, 0))) {
                            is ReceiptOcrReadResult.Success -> texts += result.text
                            is ReceiptOcrReadResult.Error -> return result
                        }
                        bitmap.recycle()
                    }
                }
                return ReceiptOcrReadResult.Success(texts.joinToString("\n"))
            }
        }
    }

    private suspend fun recognize(image: InputImage): ReceiptOcrReadResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (continuation.isActive) {
                        val text = result.text.trim()
                        continuation.resume(
                            if (text.isBlank()) ReceiptOcrReadResult.Error("Nenhum texto legível foi identificado.")
                            else ReceiptOcrReadResult.Success(text)
                        )
                    }
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    if (continuation.isActive) continuation.resume(
                        ReceiptOcrReadResult.Error("Falha no reconhecimento do texto: ${error.message ?: "erro desconhecido"}")
                    )
                }
        }
}
