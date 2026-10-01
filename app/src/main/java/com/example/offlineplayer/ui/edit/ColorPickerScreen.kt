package com.example.offlineplayer.ui.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor

/**
 * Отчуждаемый компонент выбора цвета: не знает ничего про [com.example.offlineplayer.media.SavedMedia]
 * или экран редактирования — принимает текущий цвет параметром, отдаёт выбранный колбэком [onDone].
 */
@Composable
fun ColorPickerScreen(
    initialColor: Color,
    onDone: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor = hsvToColor(hue, saturation, brightness)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Текущий цвет: квадрат со скруглёнными углами.
            CurrentColorSwatch(currentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Brightness:")
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = 0f..1f
                )
            }
        }

        HueSaturationBox(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onPick = { newHue, newSaturation ->
                hue = newHue
                saturation = newSaturation
            }
        )

        Button(
            onClick = { onDone(currentColor) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun CurrentColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, RoundedCornerShape(12.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
    )
}

@Composable
private fun HueSaturationBox(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onPick: (hue: Float, saturation: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        var widthPx by remember { mutableFloatStateOf(1f) }
        var heightPx by remember { mutableFloatStateOf(1f) }

        fun pick(x: Float, y: Float) {
            val clampedX = x.coerceIn(0f, widthPx)
            val clampedY = y.coerceIn(0f, heightPx)
            onPick((clampedX / widthPx) * 360f, 1f - (clampedY / heightPx))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        pick(change.position.x, change.position.y)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> pick(offset.x, offset.y) }
                }
        ) {
            widthPx = size.width
            heightPx = size.height

            // Горизонтальный спектр оттенков (hue 0..360).
            val hueColors = (0..360 step 60).map { degrees ->
                Color(AndroidColor.HSVToColor(floatArrayOf(degrees.toFloat(), 1f, brightness)))
            }
            drawRect(brush = Brush.horizontalGradient(hueColors))

            // Вертикальное выцветание в белый — имитирует падение насыщенности к низу.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0f), Color.White)
                )
            )

            val reticleX = (hue / 360f) * size.width
            val reticleY = (1f - saturation) * size.height
            val reticleColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))
            drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(reticleX, reticleY))
            drawCircle(color = reticleColor, radius = 11.dp.toPx(), center = Offset(reticleX, reticleY))
        }
    }
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))

@Preview(showBackground = true)
@Composable
private fun ColorPickerScreenPreview() {
    ColorPickerScreen(initialColor = Color(0xFF268BD2), onDone = {})
}
