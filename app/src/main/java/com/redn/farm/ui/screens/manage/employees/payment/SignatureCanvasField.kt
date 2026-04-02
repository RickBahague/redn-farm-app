package com.redn.farm.ui.screens.manage.employees.payment

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import java.io.ByteArrayOutputStream
import java.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path

@Composable
fun SignatureCanvasField(
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    strokeWidth: androidx.compose.ui.unit.Dp = 3.dp,
    backgroundColor: Color = Color(0xFFF5F5F5),
    onSignatureBase64Change: (String) -> Unit
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Export after the user finishes a stroke.
    fun exportCurrentInkToBase64(
        size: IntSize,
        allStrokes: List<List<Offset>>,
        strokePx: Float
    ): String {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val androidCanvas = AndroidCanvas(bitmap)

        // Transparent background; only ink is drawn.
        val paint = Paint().apply {
            this.isAntiAlias = true
            this.color = android.graphics.Color.BLACK
            this.style = Paint.Style.STROKE
            this.strokeWidth = strokePx
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }

        for (stroke in allStrokes) {
            if (stroke.isEmpty()) continue
            val path = AndroidPath().apply {
                moveTo(stroke.first().x, stroke.first().y)
                for (p in stroke.drop(1)) {
                    lineTo(p.x, p.y)
                }
            }
            androidCanvas.drawPath(path, paint)
        }

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.getEncoder().encodeToString(out.toByteArray())
    }

    fun clear() {
        strokes = emptyList()
        currentStroke = emptyList()
        onSignatureBase64Change("")
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .onSizeChanged { canvasSize = it }
                .pointerInput(strokes, currentStroke) {
                    awaitEachGesture {
                        val downEvent = awaitPointerEvent()
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val activePointerId = downChange.id
                        val initialPoint = downChange.position
                        var stroke = mutableListOf(initialPoint)
                        currentStroke = stroke.toList()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activePointerId } ?: break
                            if (!change.pressed) break

                            stroke.add(change.position)
                            currentStroke = stroke.toList()
                        }

                        if (stroke.size > 1) {
                            val newStrokes = strokes + listOf(stroke.toList())
                            strokes = newStrokes
                            currentStroke = emptyList()

                            val size = canvasSize
                            val ink = newStrokes
                            val base64 = exportCurrentInkToBase64(size, ink, strokeWidthPx)
                            onSignatureBase64Change(base64)
                        }
                    }
                }
        ) {
            // Render strokes live using Compose primitives.
            val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes
            for (stroke in allStrokes) {
                if (stroke.isEmpty()) continue
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    for (p in stroke.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        OutlinedButton(
            onClick = ::clear,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text("Clear")
        }
    }
}

