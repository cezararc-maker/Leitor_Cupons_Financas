package br.com.leitorcuponsfinancas.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface QrImageReadResult {
    data class Success(val text: String) : QrImageReadResult
    data class Error(val message: String) : QrImageReadResult
}

object QrImageReader {

    suspend fun read(
        context: Context,
        uri: Uri,
    ): QrImageReadResult = suspendCancellableCoroutine { continuation ->
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (error: Exception) {
            continuation.resume(
                QrImageReadResult.Error(
                    "Não foi possível abrir a imagem selecionada: ${error.message ?: error::class.java.simpleName}",
                ),
            )
            return@suspendCancellableCoroutine
        }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        continuation.invokeOnCancellation {
            scanner.close()
        }

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (!continuation.isActive) return@addOnSuccessListener

                val qrText = barcodes
                    .firstNotNullOfOrNull { barcode ->
                        barcode.rawValue?.trim()?.takeIf { it.isNotBlank() }
                    }

                continuation.resume(
                    if (qrText != null) {
                        QrImageReadResult.Success(qrText)
                    } else {
                        QrImageReadResult.Error(
                            "Nenhum QR Code foi encontrado na imagem selecionada.",
                        )
                    },
                )
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resume(
                        QrImageReadResult.Error(
                            "Não foi possível ler o QR Code da imagem: ${error.message ?: error::class.java.simpleName}",
                        ),
                    )
                }
            }
            .addOnCompleteListener {
                scanner.close()
            }
    }
}
