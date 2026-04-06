package com.redn.farm.utils

import android.content.Context
import android.util.Log
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object PrinterUtils {
    private const val TAG = "PrinterUtils"
    private const val CONNECT_TIMEOUT_MS = 12_000L

    private var printerService: SunmiPrinterService? = null

    // Standard Android printing using WebView
    fun printText(context: Context, text: String) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            
            val htmlContent = """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        pre {
                            font-family: monospace;
                            white-space: pre-wrap;
                            margin: 0;
                            padding: 20px;
                        }
                    </style>
                </head>
                <body>
                    <pre>$text</pre>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Order Summary ${System.currentTimeMillis()}"
                
                val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
                
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }
        }
    }

    // Sunmi printer specific implementation
    /**
     * @param alignment Sunmi: 0 = left, 1 = center, 2 = right. Use 0 for 58mm slips (`ThermalPrintBuilders`).
     */
    suspend fun printMessage(
        context: Context,
        message: String,
        isLarge: Boolean = false,
        alignment: Int = 1,
    ): Boolean {
        return try {
            // First ensure we have a printer connection
            val service = printerService ?: connectPrinter(context) ?: return false

            service.run {
                // Initialize printer
                printerInit(null)

                setAlignment(alignment, null)

                if (isLarge) {
                    // Set larger text size (2x default size)
                    setFontSize(40f, null)
                    // Set text to bold
                    setPrinterStyle(WoyouConsts.ENABLE_BOLD, 1)
                } else {
                    // Normal text size
                    setFontSize(23f, null)
                }

                // Print the text
                printText(message + "\n", null)

                // Reset text size and style if needed
                if (isLarge) {
                    setFontSize(22f, null)
                    setPrinterStyle(WoyouConsts.ENABLE_BOLD, 0)
                }

                // Feed + cut can throw on some firmware after content already printed; still report success (BUG-PRT-01).
                try {
                    lineWrap(3, null)
                } catch (e: Exception) {
                    Log.w(TAG, "lineWrap after printText", e)
                }
                try {
                    cutPaper(null)
                } catch (e: Exception) {
                    Log.w(TAG, "cutPaper after printText", e)
                }

                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Binds Sunmi printer service. Does **not** treat [InnerPrinterCallback.onDisconnected] as bind failure:
     * a spurious disconnect before [onConnected] used to resume `null` and made [printMessage] return false
     * even when printing later succeeded (BUG-PRT-01).
     */
    private suspend fun connectPrinter(context: Context): SunmiPrinterService? =
        withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val resumed = AtomicBoolean(false)
                fun resumeOnce(value: SunmiPrinterService?) {
                    if (!resumed.compareAndSet(false, true)) return
                    try {
                        continuation.resume(value)
                    } catch (_: Exception) {
                        // Continuation already completed (e.g. timeout) — ignore late callback
                    }
                }
                try {
                    if (printerService != null) {
                        resumeOnce(printerService)
                        return@suspendCancellableCoroutine
                    }

                    InnerPrinterManager.getInstance().bindService(
                        context,
                        object : InnerPrinterCallback() {
                            override fun onConnected(service: SunmiPrinterService) {
                                printerService = service
                                resumeOnce(service)
                            }

                            override fun onDisconnected() {
                                printerService = null
                            }
                        },
                    )
                } catch (e: InnerPrinterException) {
                    e.printStackTrace()
                    resumeOnce(null)
                }

                continuation.invokeOnCancellation {
                    try {
                        InnerPrinterManager.getInstance().unBindService(
                            context,
                            object : InnerPrinterCallback() {
                                override fun onConnected(service: SunmiPrinterService) {}
                                override fun onDisconnected() {}
                            },
                        )
                        printerService = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
} 