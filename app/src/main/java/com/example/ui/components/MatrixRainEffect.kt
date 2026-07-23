package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val MATRIX_CHARS = "0123456789ABCDEFGHIKLMNOPNQRSTUVXYZλμπθΩ$#@%&*!?"

private data class MatrixStream(
    val xIndex: Int,
    var headY: Float,
    var speed: Float,
    val length: Int,
    val chars: MutableList<Char>
)

@Composable
fun MatrixRainEffect(
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 14
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }

    val androidPaint = remember(fontSizePx) {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textSize = fontSizePx
            isFakeBoldText = true
        }
    }

    var streams by remember { mutableStateOf<List<MatrixStream>>(emptyList()) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // Animation frame tick
    LaunchedEffect(canvasWidth, canvasHeight) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@LaunchedEffect

        val columnCount = (canvasWidth / (fontSizePx * 1.3f)).toInt().coerceAtLeast(8)

        streams = List(columnCount) { colIdx ->
            MatrixStream(
                xIndex = colIdx,
                headY = Random.nextFloat() * canvasHeight * -1f,
                speed = Random.nextFloat() * 12f + 8f,
                length = Random.nextInt(10, 25),
                chars = MutableList(30) { MATRIX_CHARS[Random.nextInt(MATRIX_CHARS.length)] }
            )
        }

        while (true) {
            delay(40) // 25 FPS matrix stream tick
            streams.forEach { stream ->
                stream.headY += stream.speed
                if (Random.nextFloat() < 0.2f) {
                    val randomPos = Random.nextInt(stream.chars.size)
                    stream.chars[randomPos] = MATRIX_CHARS[Random.nextInt(MATRIX_CHARS.length)]
                }
                if (stream.headY - (stream.length * fontSizePx * 1.5f) > canvasHeight) {
                    stream.headY = Random.nextFloat() * -150f
                    stream.speed = Random.nextFloat() * 12f + 8f
                }
            }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        if (size.width != canvasWidth || size.height != canvasHeight) {
            canvasWidth = size.width
            canvasHeight = size.height
        }

        // Deep Pitch Black Cyber Canvas
        drawRect(color = Color(0xFF030A06))

        val charHeight = fontSizePx * 1.5f
        val colStep = if (streams.isNotEmpty()) size.width / streams.size else fontSizePx * 1.3f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            streams.forEach { stream ->
                val xPos = stream.xIndex * colStep

                for (i in 0 until stream.length) {
                    val yPos = stream.headY - (i * charHeight)
                    if (yPos in 0f..(size.height + charHeight)) {
                        val char = stream.chars[i % stream.chars.size]

                        val charColor = when (i) {
                            0 -> Color(0xFFE2FFEC) // Bright White-Green Head
                            1 -> Color(0xFF00FF66) // Neon Green
                            in 2..5 -> Color(0xFF00CC44) // Bright Emerald
                            in 6..12 -> Color(0xFF008822) // Dark Green
                            else -> Color(0xFF004411).copy(
                                alpha = (1f - (i.toFloat() / stream.length)).coerceIn(0.1f, 0.6f)
                            )
                        }

                        androidPaint.color = charColor.toArgb()
                        nativeCanvas.drawText(char.toString(), xPos, yPos, androidPaint)
                    }
                }
            }
        }
    }
}
