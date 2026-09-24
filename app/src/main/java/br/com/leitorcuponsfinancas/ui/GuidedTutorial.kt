package br.com.leitorcuponsfinancas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.composed
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class GuidedTutorialStep(
    val targetKey: String,
    val title: String,
    val text: String,
)

@Stable
internal class TutorialTargetRegistry {
    val bounds = mutableStateMapOf<String, Rect>()

    fun update(key: String, rect: Rect) {
        bounds[key] = rect
    }
}

internal val LocalTutorialTargetRegistry = staticCompositionLocalOf<TutorialTargetRegistry?> {
    null
}

internal fun Modifier.tutorialTarget(key: String): Modifier = composed {
    val registry = LocalTutorialTargetRegistry.current

    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coordinates ->
            registry.update(key, coordinates.boundsInRoot())
        }
    }
}

@Composable
internal fun GuidedTutorialOverlay(
    steps: List<GuidedTutorialStep>,
    stepIndex: Int,
    registry: TutorialTargetRegistry,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty() || stepIndex !in steps.indices) return

    val step = steps[stepIndex]
    val target = registry.bounds[step.targetKey]
    val density = LocalDensity.current
    val paddingPx = with(density) { 8.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        Canvas(Modifier.fillMaxSize()) {
            val overlay = Color.Black.copy(alpha = 0.68f)

            if (target == null) {
                drawRect(overlay)
            } else {
                val left = max(0f, target.left - paddingPx)
                val top = max(0f, target.top - paddingPx)
                val right = min(size.width, target.right + paddingPx)
                val bottom = min(size.height, target.bottom + paddingPx)

                drawRect(
                    color = overlay,
                    topLeft = Offset.Zero,
                    size = Size(size.width, top),
                )
                drawRect(
                    color = overlay,
                    topLeft = Offset(0f, bottom),
                    size = Size(size.width, max(0f, size.height - bottom)),
                )
                drawRect(
                    color = overlay,
                    topLeft = Offset(0f, top),
                    size = Size(left, max(0f, bottom - top)),
                )
                drawRect(
                    color = overlay,
                    topLeft = Offset(right, top),
                    size = Size(max(0f, size.width - right), max(0f, bottom - top)),
                )
            }
        }

        target?.let { rect ->
            val left = max(0f, rect.left - paddingPx)
            val top = max(0f, rect.top - paddingPx)
            val right = min(screenWidthPx, rect.right + paddingPx)
            val bottom = min(screenHeightPx, rect.bottom + paddingPx)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            left.roundToInt(),
                            top.roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { (right - left).toDp() },
                        height = with(density) { (bottom - top).toDp() },
                    )
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(18.dp),
                    ),
            )
        }

        val bubbleAlignment = when {
            target == null -> Alignment.Center
            target.center.y > screenHeightPx * 0.58f -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }

        AnimatedContent(
            targetState = stepIndex,
            transitionSpec = {
                (
                    fadeIn(tween(180)) +
                        slideInVertically(tween(220)) { height -> height / 5 }
                    ).togetherWith(
                    fadeOut(tween(130)) +
                        slideOutVertically(tween(170)) { height -> -height / 7 },
                )
            },
            modifier = Modifier
                .align(bubbleAlignment)
                .padding(
                    horizontal = 18.dp,
                    vertical = if (bubbleAlignment == Alignment.Center) 0.dp else 82.dp,
                ),
            label = "guidedTutorialBubble",
        ) { index ->
            val current = steps[index]

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = current.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${index + 1} de ${steps.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (index > 0) {
                            TextButton(onClick = onBack) {
                                Text("Voltar")
                            }
                        } else {
                            TextButton(onClick = onSkip) {
                                Text("Pular")
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (index > 0) {
                                TextButton(onClick = onSkip) {
                                    Text("Pular")
                                }
                            }

                            Button(onClick = onNext) {
                                Text(
                                    if (index == steps.lastIndex) {
                                        "Começar"
                                    } else {
                                        "Próximo"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
