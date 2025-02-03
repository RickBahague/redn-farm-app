package com.redn.farm.utils

import android.content.Context
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PrinterUtils {
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
    suspend fun printMessage(context: Context, message: String, isLarge: Boolean = false): Boolean {
        return try {
            // First ensure we have a printer connection
            val service = printerService ?: connectPrinter(context) ?: return false

            service.run {
                // Initialize printer
                printerInit(null)

                // Set alignment to center
                setAlignment(1, null)

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

                // Add line spacing
                lineWrap(3, null)

                // Cut paper
                cutPaper(null)

                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun connectPrinter(context: Context): SunmiPrinterService? =
        suspendCancellableCoroutine { continuation ->
            try {
                // Check if we already have a connection
                if (printerService != null) {
                    continuation.resume(printerService)
                    return@suspendCancellableCoroutine
                }

                // Bind to the printer service
                InnerPrinterManager.getInstance().bindService(context,
                    object : InnerPrinterCallback() {
                        override fun onConnected(service: SunmiPrinterService) {
                            printerService = service
                            continuation.resume(service)
                        }

                        override fun onDisconnected() {
                            printerService = null
                            continuation.resume(null)
                        }
                    })
            } catch (e: InnerPrinterException) {
                e.printStackTrace()
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                try {
                    InnerPrinterManager.getInstance().unBindService(context,
                        object : InnerPrinterCallback() {
                            override fun onConnected(service: SunmiPrinterService) {}
                            override fun onDisconnected() {}
                        })
                    printerService = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
} 