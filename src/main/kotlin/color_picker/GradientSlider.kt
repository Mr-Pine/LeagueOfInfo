package color_picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.util.lerp
import design.darken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

// Internal to be referred to in tests
internal val ThumbRadius = 12.dp
private val ThumbRippleRadius = 24.dp
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

// Internal to be referred to in tests
internal val TrackHeight = 8.dp
private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp // TODO: clarify min width
private val DefaultSliderConstraints =
    Modifier.widthIn(min = SliderMinWidth)
        .heightIn(max = SliderHeight)

@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    currentColor: Color,
    brush: Brush,
    backgroundColor: Color = currentColor.darken(),
    outlineColor: Color = Color.Gray
) {
    require(steps >= 0) { "steps should be >= 0" }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val tickFractions = remember(steps) {
        if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }
    }

    BoxWithConstraints(
        modifier
            .requiredSizeIn(minWidth = ThumbRadius * 4, minHeight = ThumbRadius * 4, maxHeight = ThumbRadius * 4)
            .sliderSemantics(value, tickFractions, enabled, onValueChange, valueRange, steps)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val minPx = with(LocalDensity.current) { ThumbRadius.toPx() * 2 }
        val maxPx = constraints.maxWidth - minPx

        fun scaleToUserValue(offset: Float) =
            scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

        val scope = rememberCoroutineScope()
        val rawOffset = remember { mutableStateOf(scaleToOffset(value)) }
        var imaginaryOffset by remember { mutableStateOf(scaleToOffset(value)) }
        val draggableState = remember(minPx, maxPx, valueRange) {
            SliderDraggableState {
                imaginaryOffset += it
                rawOffset.value = imaginaryOffset.coerceIn(minPx, maxPx)
                onValueChangeState.value.invoke(scaleToUserValue(rawOffset.value))
            }
        }
        SideEffect {
            val newOffset = scaleToOffset(value)
            // floating point error due to rescaling
            val error = (valueRange.endInclusive - valueRange.start) / 1000
            if (abs(newOffset - rawOffset.value) > error) rawOffset.value = newOffset
        }

        val gestureEndAction = rememberUpdatedState<(Float) -> Unit> { velocity: Float ->
            val current = rawOffset.value
            // target is a closest anchor to the `current`, if exists
            val target = tickFractions
                .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
                ?.run { lerp(minPx, maxPx, this) }
                ?: current
            if (current != target) {
                scope.launch {
                    animateToTarget(draggableState, current, target, velocity)
                    onValueChangeFinished?.invoke()
                }
            } else if (!draggableState.isDragging) {
                // check ifDragging in case the change is still in progress (touch -> drag case)
                onValueChangeFinished?.invoke()
            }
        }

        val press = Modifier.sliderPressModifier(
            draggableState, interactionSource, maxPx, isRtl, rawOffset, gestureEndAction, enabled
        )

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { velocity ->
                gestureEndAction.value.invoke(velocity); imaginaryOffset = imaginaryOffset.coerceIn(minPx, maxPx)
            },
            startDragImmediately = draggableState.isDragging,
            state = draggableState
        )

        val scrollableState = rememberScrollableState { delta ->

            scope.launch {
                /*draggableState.drag(MutatePriority.UserInput) {
                    dragBy(2 * delta)
                }*/
                rawOffset.value += delta * 2
                rawOffset.value = rawOffset.value.coerceIn(minPx, maxPx)
                onValueChangeState.value.invoke(scaleToUserValue(rawOffset.value))
            }

            delta
        }

        val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)

        Box(/*modifier = Modifier.scrollable(scrollableState, Orientation.Vertical)*/) {
            GradientSliderImpl(
                enabled,
                fraction,
                tickFractions,
                currentColor,
                maxPx,
                interactionSource,
                modifier = press.then(drag),
                brush,
                outlineColor
            )
        }
    }
}

@Composable
private fun GradientSliderImpl(
    enabled: Boolean,
    positionFraction: Float,
    tickFractions: List<Float>,
    currentColor: Color,
    width: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    brush: Brush,
    outlineColor: Color
) {
    val widthDp = with(LocalDensity.current) {
        width.toDp()
    }
    Box(modifier.then(DefaultSliderConstraints)) {
        val thumbSize = ThumbRadius * 2
        val offset = (widthDp - thumbSize) * positionFraction + ThumbRadius
        val center = Modifier.align(Alignment.CenterStart)

        val trackStrokeWidth: Float
        val thumbPx: Float
        with(LocalDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            thumbPx = ThumbRadius.toPx()
        }
        GradientTrack(
            center.fillMaxSize(),
            currentColor,
            thumbPx,
            trackStrokeWidth,
            brush
        )
        Box(center.padding(start = offset)) {
            val interactions = remember { mutableStateListOf<Interaction>() }

            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> interactions.add(interaction)
                        is PressInteraction.Release -> interactions.remove(interaction.press)
                        is PressInteraction.Cancel -> interactions.remove(interaction.press)
                        is DragInteraction.Start -> interactions.add(interaction)
                        is DragInteraction.Stop -> interactions.remove(interaction.start)
                        is DragInteraction.Cancel -> interactions.remove(interaction.start)
                    }
                }
            }

            val hasInteraction = interactions.isNotEmpty()
            val elevation = if (hasInteraction) {
                ThumbPressedElevation
            } else {
                ThumbDefaultElevation
            }
            Spacer(
                Modifier
                    .size(thumbSize, thumbSize)
                    .focusable(interactionSource = interactionSource)
                    .indication(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
                    )
                    .shadow(if (enabled) elevation else 0.dp, CircleShape, clip = false)
                    .background(currentColor, CircleShape)
                    .border(1.dp, outlineColor, CircleShape)
            )
        }
    }
}


private fun Modifier.sliderSemantics(
    value: Float,
    tickFractions: List<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    return semantics(mergeDescendants = true) {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                val newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                val resolvedValue = if (steps > 0) {
                    tickFractions
                        .map { lerp(valueRange.start, valueRange.endInclusive, it) }
                        .minByOrNull { (it - newValue) } ?: newValue
                } else {
                    newValue
                }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue as Float)
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}

private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

private class SliderDraggableState(
    val onDelta: (Float) -> Unit
) : DraggableState {

    var isDragging by mutableStateOf(false)
        private set

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

private suspend fun animateToTarget(
    draggableState: DraggableState,
    current: Float,
    target: Float,
    velocity: Float
) {
    draggableState.drag {
        var latestValue = current
        Animatable(initialValue = current).animateTo(target, SliderToTickAnimation, velocity) {
            dragBy(this.value - latestValue)
            latestValue = this.value
        }
    }
}

private val SliderToTickAnimation = TweenSpec<Float>(durationMillis = 100)

private fun Modifier.sliderPressModifier(
    draggableState: DraggableState,
    interactionSource: MutableInteractionSource,
    maxPx: Float,
    isRtl: Boolean,
    rawOffset: State<Float>,
    gestureEndAction: State<(Float) -> Unit>,
    enabled: Boolean
): Modifier =
    if (enabled) {
        pointerInput(draggableState, interactionSource, maxPx, isRtl) {
            detectTapGestures(
                onPress = { pos ->
                    draggableState.drag(MutatePriority.UserInput) {
                        val to = if (isRtl) maxPx - pos.x else pos.x
                        dragBy(to - rawOffset.value)
                    }
                    val interaction = PressInteraction.Press(pos)
                    interactionSource.emit(interaction)
                    val finishInteraction =
                        try {
                            val success = tryAwaitRelease()
                            gestureEndAction.value.invoke(0f)
                            if (success) {
                                PressInteraction.Release(interaction)
                            } else {
                                PressInteraction.Cancel(interaction)
                            }
                        } catch (c: CancellationException) {
                            PressInteraction.Cancel(interaction)
                        }
                    interactionSource.emit(finishInteraction)
                }
            )
        }
    } else {
        this
    }

@Composable
private fun GradientTrack(
    modifier: Modifier,
    currentColor: Color,
    thumbPx: Float,
    trackStrokeWidth: Float,
    brush: Brush
) {
    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbPx * 2, center.y)
        val sliderRight = Offset(size.width - thumbPx * 2, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight



        drawLine(
            brush,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round,
        )
    }
}