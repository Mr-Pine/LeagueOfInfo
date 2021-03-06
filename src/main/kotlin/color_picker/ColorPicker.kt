package color_picker

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import design.darken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.BlendMode
import java.util.*
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

@Composable
fun ColorPickerWidget(
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    currentContrast: Float,
    modifier: Modifier = Modifier,
    setContrast: (Float) -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        var lightnessSliderPosition by remember { mutableStateOf(0.5f) }


        val currentHSB = FloatArray(3)
        java.awt.Color.RGBtoHSB(
            (currentColor.red * 255).toInt(),
            (currentColor.green * 255).toInt(),
            (currentColor.blue * 255).toInt(),
            currentHSB
        )
        val fullLightness = java.awt.Color.HSBtoRGB(currentHSB[0], currentHSB[1], 1f)

        val lightnessGradientBrush = Brush.linearGradient(listOf(Color.Black, Color(fullLightness)))

        val outlineColor by remember(
            currentColor,
            currentContrast
        ) {
            val darkerColor = currentColor.darken(currentContrast)
            val contrastHSB = FloatArray(3)
            java.awt.Color.RGBtoHSB(
                (darkerColor.red * 255).toInt(),
                (darkerColor.green * 255).toInt(),
                (darkerColor.blue * 255).toInt(),
                contrastHSB
            )
            contrastHSB[2] -= 0.2f * (contrastHSB[2] - 0.4f).sign
            contrastHSB[2] = contrastHSB[2].coerceIn(0f..1f)
            mutableStateOf(
                Color(java.awt.Color.HSBtoRGB(contrastHSB[0], contrastHSB[1], contrastHSB[2]))
            ) }

        ColorPicker(Modifier, lightnessSliderPosition, outlineColor, onColorChange)

        GradientSlider(
            value = lightnessSliderPosition,
            valueRange = 0.1f..1f,
            currentColor = currentColor,
            brush = lightnessGradientBrush,
            onValueChange = { lightness ->
                CoroutineScope(Dispatchers.Default).launch {
                    lightnessSliderPosition = lightness

                    val adjustedLightness = java.awt.Color.HSBtoRGB(currentHSB[0], currentHSB[1], lightness)

                    onColorChange(Color(adjustedLightness))
                }
            },
            backgroundColor = currentColor.darken(currentContrast),
            outlineColor = outlineColor
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )

        val contrastGradientBrush = Brush.linearGradient(listOf(currentColor.darken(0f), currentColor.darken(1f)))



        GradientSlider(
            value = currentContrast,
            valueRange = 0f..1f,
            currentColor = currentColor.darken(currentContrast),
            brush = contrastGradientBrush,
            onValueChange = setContrast,
            backgroundColor = currentColor.darken(currentContrast),
            outlineColor = outlineColor
        )
    }
}

@Composable
fun ColorPicker(sizeModifier: Modifier, lightness: Float, outlineColor: Color = Color.Transparent, onColorChange: (Color) -> Unit) {
    require(lightness in 0f..1f)
    BoxWithConstraints(
        sizeModifier.padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val diameter = constraints.maxWidth
        var position by remember { mutableStateOf(Offset.Zero) }
        val colorWheel = remember(diameter, lightness) { ColorWheel(diameter, lightness) }

        var hasInput by remember { mutableStateOf(false) }
        val inputModifier = Modifier.pointerInput(colorWheel) {
            fun updateColorWheel(newPosition: Offset) {
                var updatedPosition = newPosition
                // Work out if the new position is inside the circle we are drawing, and has a
                // valid color associated to it. If not, keep the current position
                val centeredPosition = newPosition - Offset(diameter / 2f, diameter / 2f)
                if (centeredPosition.getDistance() > diameter / 2) {
                    val angle = asin(centeredPosition.x / centeredPosition.getDistance())
                    updatedPosition = Offset(
                        sin(angle) * (diameter / 2 - 2),
                        cos(angle) * (diameter / 2 - 2) * centeredPosition.y.sign
                    ) + Offset(diameter / 2f, diameter / 2f)
                }
                val newColor = colorWheel.colorForPosition(updatedPosition)
                if (newColor.isSpecified) {
                    position = updatedPosition
                    onColorChange(newColor)
                }
            }

            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown()
                    hasInput = true
                    updateColorWheel(down.position)
                    drag(down.id) { change ->
                        change.consumePositionChange()
                        updateColorWheel(change.position)
                    }
                    hasInput = false
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            Image(modifier = inputModifier
                .border(2.dp, outlineColor, CircleShape), contentDescription = null, bitmap = colorWheel.image)
            val color = colorWheel.colorForPosition(position)
            if (color.isSpecified) {
                Magnifier(visible = hasInput, position = position, color = color)
            }
        }
    }
}

/**
 * Magnifier displayed on top of [position] with the currently selected [color].
 */
@Composable
private fun Magnifier(visible: Boolean, position: Offset, color: Color) {
    val offset = with(LocalDensity.current) {
        Modifier.offset(
            position.x.toDp() - MagnifierWidth / 2,
            // Align with the center of the selection circle
            position.y.toDp() - (MagnifierHeight - (SelectionCircleDiameter / 2))
        )
    }
    Popup {
        MagnifierTransition(
            visible
        ) { labelWidth: Dp, selectionDiameter: Dp,
            alpha: Float ->
            Column(
                offset.size(width = MagnifierWidth, height = MagnifierHeight)
                    .alpha(alpha)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MagnifierLabel(Modifier.size(labelWidth, MagnifierLabelHeight), color)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().height(SelectionCircleDiameter),
                    contentAlignment = Alignment.Center
                ) {
                    MagnifierSelectionCircle(Modifier.size(selectionDiameter), color)
                }
            }
        }
    }
}

private val MagnifierWidth = 110.dp
private val MagnifierHeight = 90.dp
private val MagnifierLabelHeight = 50.dp
private val SelectionCircleDiameter = 30.dp

@Composable
private fun MagnifierTransition(
    visible: Boolean,
    content: @Composable (labelWidth: Dp, selectionDiameter: Dp, alpha: Float) -> Unit
) {
    val transition = updateTransition(visible)
    val labelWidth by transition.animateDp(transitionSpec = { tween() }) {
        if (it) MagnifierWidth else 0.dp
    }
    val magnifierDiameter by transition.animateDp(transitionSpec = { tween() }) {
        if (it) SelectionCircleDiameter else 0.dp
    }
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (true isTransitioningTo false) {
                tween(delayMillis = 100, durationMillis = 200)
            } else {
                tween()
            }
        }
    ) {
        if (it) 1f else 0f
    }
    content(labelWidth, magnifierDiameter, alpha)
}

/**
 * Label representing the currently selected [color], with [Text] representing the hex code and a
 * square at the start showing the [color].
 */
@Composable
private fun MagnifierLabel(modifier: Modifier, color: Color) {
    Surface(shape = MagnifierPopupShape, elevation = 4.dp) {
        Row(modifier) {
            Box(Modifier.weight(0.25f).fillMaxHeight().background(color))
            // Add `#` and drop alpha characters
            val text = "#" + Integer.toHexString(color.toArgb()).uppercase(Locale.ROOT).drop(2)
            val textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            Text(
                text = text,
                modifier = Modifier.weight(0.75f).padding(top = 10.dp, bottom = 20.dp),
                style = textStyle,
                maxLines = 1
            )
        }
    }
}

/**
 * Selection circle drawn over the currently selected pixel of the color wheel.
 */
@Composable
private fun MagnifierSelectionCircle(modifier: Modifier, color: Color) {
    Surface(
        modifier,
        shape = CircleShape,
        elevation = 4.dp,
        color = color,
        border = BorderStroke(2.dp, SolidColor(Color.Black.copy(alpha = 0.75f))),
        content = {}
    )
}

/**
 * A [GenericShape] that draws a box with a triangle at the bottom center to indicate a popup.
 */
private val MagnifierPopupShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height

    val arrowY = height * 0.8f
    val arrowXOffset = width * 0.4f

    addRoundRect(RoundRect(0f, 0f, width, arrowY, cornerRadius = CornerRadius(20f, 20f)))

    moveTo(arrowXOffset, arrowY)
    lineTo(width / 2f, height)
    lineTo(width - arrowXOffset, arrowY)
    close()
}

/**
 * A color wheel with an [ImageBitmap] that draws a circular color wheel of the specified diameter.
 */
private class ColorWheel(diameter: Int, lightness: Float) {
    private val radius = diameter / 2f

    private val sweepGradient = SweepGradientShader(
        colors = listOf(
            Color.Red,
            Color.Magenta,
            Color.Blue,
            Color.Cyan,
            Color.Green,
            Color.Yellow,
            Color.Red
        ),
        colorStops = null,
        center = Offset(radius, radius)
    )

    val saturationGradient = RadialGradientShader(
        center = Offset(radius, radius),
        radius = radius,
        colors = listOf(Color.White, Color.Transparent)
    )

    val lightnessShader = Shader.makeColor(Color(lightness, lightness, lightness).toArgb())

    val combinedShader = Shader.makeBlend(BlendMode.LIGHTEN, sweepGradient, saturationGradient)
    val lightnessAdjustedShader = Shader.makeBlend(BlendMode.MULTIPLY, combinedShader, lightnessShader)

    val image = ImageBitmap(diameter, diameter).also { imageBitmap ->
        val canvas = Canvas(imageBitmap)
        val center = Offset(radius, radius)
        val paint = Paint().apply { shader = lightnessAdjustedShader }
        canvas.drawCircle(center, radius, paint)
    }
}

/**
 * @return the matching color for [position] inside [ColorWheel], or `null` if there is no color
 * or the color is partially transparent.
 */
private fun ColorWheel.colorForPosition(position: Offset): Color {
    val x = position.x.toInt().coerceAtLeast(0)
    val y = position.y.toInt().coerceAtLeast(0)
    with(image.toPixelMap()) {
        if (x >= width || y >= height) return Color.Unspecified
        return this[x, y].takeIf { it.alpha == 1f } ?: Color.Unspecified
    }
}