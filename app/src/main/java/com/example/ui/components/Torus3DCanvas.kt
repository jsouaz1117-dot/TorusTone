package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.model.MusicalScale
import com.example.model.TonnetzMath
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class ProjectedNode(
    val u: Int,
    val v: Int,
    val pitchClass: Int,
    val screenPos: Offset,
    val depth: Float,
    val scaleFactor: Float
)

/**
 * Interactive 3D projected Harmonic Torus (Tonnetz) rendered on Compose Canvas.
 * Major ring steps by Perfect Fifths (12 steps), minor ring steps by Major Thirds (6 steps).
 * Drag anywhere to spin in 3D space; tap or press any pitch node to resonate.
 */
@Composable
fun Torus3DCanvas(
    activePitchClasses: Set<Int>,
    scale: MusicalScale,
    rootPitchClass: Int,
    isMinorAccent: Boolean,
    onNodePressed: (pitchClass: Int) -> Unit,
    onNodeReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uCount = 12
    val vCount = 6

    var yaw by remember { mutableFloatStateOf(0.45f) }
    var pitch by remember { mutableFloatStateOf(0.62f) }
    var isDragging by remember { mutableStateOf(false) }

    // Gentle idle precession so the torus feels alive like a precision acoustic sculpture
    LaunchedEffect(isDragging) {
        while (!isDragging) {
            delay(16L)
            yaw += 0.0038f
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "torus_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val textMeasurer = rememberTextMeasurer()
    val outlineSubtle = MaterialTheme.colorScheme.outlineVariant
    val bgDeep = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    var latestProjected by remember { mutableStateOf<List<ProjectedNode>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("torus_3d_canvas")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        yaw += dragAmount.x * 0.0085f
                        pitch = (pitch + dragAmount.y * 0.0085f).coerceIn(-1.25f, 1.25f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { tapOffset ->
                        // Prefer front-facing nodes when hit-testing
                        val hit = latestProjected
                            .filter { it.depth > -0.6f }
                            .minByOrNull { node ->
                                hypot(
                                    (node.screenPos.x - tapOffset.x).toDouble(),
                                    (node.screenPos.y - tapOffset.y).toDouble()
                                )
                            }
                        if (hit != null) {
                            val dist = hypot(
                                (hit.screenPos.x - tapOffset.x).toDouble(),
                                (hit.screenPos.y - tapOffset.y).toDouble()
                            )
                            if (dist < 86.0) {
                                onNodePressed(hit.pitchClass)
                                tryAwaitRelease()
                                onNodeReleased()
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val minDim = minOf(w, h)

            // Ambient radial studio glow behind the torus
            val activeGlowColor = when {
                activePitchClasses.isEmpty() -> HarmonicCyan.copy(alpha = 0.08f)
                isMinorAccent -> MinorRose.copy(alpha = 0.16f + pulse * 0.06f)
                else -> BrassGold.copy(alpha = 0.16f + pulse * 0.06f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeGlowColor, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = minDim * 0.54f
                ),
                radius = minDim * 0.54f,
                center = Offset(cx, cy)
            )

            val majorR = 1.0f
            val minorR = 0.43f
            val cameraDist = 3.55f
            val focal = minDim * 0.94f

            val cosYaw = cos(yaw)
            val sinYaw = sin(yaw)
            val cosPitch = cos(pitch)
            val sinPitch = sin(pitch)

            val grid = Array(uCount) { u ->
                Array(vCount) { v ->
                    val theta = (u.toFloat() / uCount) * (2.0 * PI).toFloat()
                    val phi = (v.toFloat() / vCount) * (2.0 * PI).toFloat()

                    val ring = majorR + minorR * cos(phi)
                    val x0 = ring * cos(theta)
                    val y0 = ring * sin(theta)
                    val z0 = minorR * sin(phi)

                    // Rotate yaw around Y, then pitch around X
                    val x1 = x0 * cosYaw + z0 * sinYaw
                    val z1 = -x0 * sinYaw + z0 * cosYaw
                    val y2 = y0 * cosPitch - z1 * sinPitch
                    val z2 = y0 * sinPitch + z1 * cosPitch

                    val perspective = focal / (cameraDist - z2)
                    val sx = cx + x1 * perspective
                    val sy = cy + y2 * perspective
                    val scaleFactor = (perspective / (focal / cameraDist)).coerceIn(0.55f, 1.65f)

                    ProjectedNode(
                        u = u,
                        v = v,
                        pitchClass = TonnetzMath.pitchClassAt(u, v),
                        screenPos = Offset(sx, sy),
                        depth = z2,
                        scaleFactor = scaleFactor
                    )
                }
            }

            // Collect and depth-sort edges along Perfect Fifths (u), Major Thirds (v), and Minor Thirds (diag)
            data class TorusEdge(
                val a: ProjectedNode,
                val b: ProjectedNode,
                val avgDepth: Float,
                val isFifthEdge: Boolean
            )

            val edges = ArrayList<TorusEdge>(uCount * vCount * 3)
            for (u in 0 until uCount) {
                for (v in 0 until vCount) {
                    val cur = grid[u][v]
                    val nextU = grid[(u + 1) % uCount][v]
                    val nextV = grid[u][(v + 1) % vCount]
                    val diag = grid[(u + 1) % uCount][(v - 1 + vCount) % vCount]
                    edges.add(TorusEdge(cur, nextU, (cur.depth + nextU.depth) * 0.5f, true))
                    edges.add(TorusEdge(cur, nextV, (cur.depth + nextV.depth) * 0.5f, false))
                    edges.add(TorusEdge(cur, diag, (cur.depth + diag.depth) * 0.5f, false))
                }
            }
            edges.sortBy { it.avgDepth }

            val activeAccent = if (isMinorAccent) MinorRose else BrassGold

            for (edge in edges) {
                val depthAlpha = ((edge.avgDepth + 1.55f) / 3.1f).coerceIn(0.08f, 1f)
                val bothActive = edge.a.pitchClass in activePitchClasses &&
                    edge.b.pitchClass in activePitchClasses

                val strokeColor = when {
                    bothActive -> activeAccent.copy(alpha = (0.45f + depthAlpha * 0.55f).coerceIn(0f, 1f))
                    edge.isFifthEdge -> HarmonicCyan.copy(alpha = depthAlpha * 0.34f)
                    else -> outlineSubtle.copy(alpha = depthAlpha * 0.42f)
                }
                val strokeW = when {
                    bothActive -> 3.4f * ((edge.a.scaleFactor + edge.b.scaleFactor) * 0.5f)
                    edge.isFifthEdge -> 1.5f
                    else -> 1.0f
                }

                drawLine(
                    color = strokeColor,
                    start = edge.a.screenPos,
                    end = edge.b.screenPos,
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Sort nodes back-to-front for crisp 3D painter's rendering
            val flatNodes = grid.flatMap { it.toList() }.sortedBy { it.depth }
            latestProjected = flatNodes

            for (node in flatNodes) {
                val depthNorm = ((node.depth + 1.55f) / 3.1f).coerceIn(0f, 1f)
                val isActive = node.pitchClass in activePitchClasses
                val relPc = TonnetzMath.positiveMod12(node.pitchClass - rootPitchClass)
                val inScale = relPc in scale.intervals
                val isRoot = node.pitchClass == rootPitchClass

                val baseRadius = when {
                    isActive -> 15.5f * node.scaleFactor
                    isRoot -> 12.5f * node.scaleFactor
                    inScale -> 10.5f * node.scaleFactor
                    else -> 6.5f * node.scaleFactor
                }

                if (isActive && depthNorm > 0.25f) {
                    val haloRadius = baseRadius * (1.75f + pulse * 0.45f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = 0.55f * depthNorm),
                                Color.Transparent
                            ),
                            center = node.screenPos,
                            radius = haloRadius
                        ),
                        radius = haloRadius,
                        center = node.screenPos
                    )
                }

                val fillColor = when {
                    isActive -> activeAccent
                    isRoot -> HarmonicCyan.copy(alpha = (0.35f + depthNorm * 0.65f).coerceIn(0f, 1f))
                    inScale -> Color(0xFF1F2A3C).copy(alpha = (0.35f + depthNorm * 0.65f).coerceIn(0f, 1f))
                    else -> bgDeep.copy(alpha = (0.25f + depthNorm * 0.55f).coerceIn(0f, 1f))
                }
                val borderColor = when {
                    isActive -> Color.White.copy(alpha = 0.9f)
                    isRoot -> HarmonicCyan
                    inScale -> HarmonicCyan.copy(alpha = depthNorm * 0.62f)
                    else -> outlineSubtle.copy(alpha = depthNorm * 0.45f)
                }

                drawCircle(
                    color = fillColor,
                    radius = baseRadius,
                    center = node.screenPos
                )
                drawCircle(
                    color = borderColor,
                    radius = baseRadius,
                    center = node.screenPos,
                    style = Stroke(width = if (isActive) 2.2f else 1.2f)
                )

                // Render crisp pitch labels on front-facing or active nodes
                if ((depthNorm > 0.56f && (inScale || isRoot)) || (isActive && depthNorm > 0.35f)) {
                    val label = TonnetzMath.noteName(node.pitchClass)
                    val measured = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontFamily = MonoFontFamily,
                            fontSize = (9.5f * node.scaleFactor.coerceIn(0.85f, 1.2f)).sp,
                            fontWeight = if (isActive || isRoot) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isActive -> bgDeep
                                isRoot -> bgDeep
                                else -> textPrimary.copy(alpha = depthNorm)
                            }
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            node.screenPos.x - measured.size.width / 2f,
                            node.screenPos.y - measured.size.height / 2f
                        )
                    )
                } else if (depthNorm > 0.72f && !inScale) {
                    val dotColor = textMuted.copy(alpha = 0.35f)
                    drawCircle(color = dotColor, radius = 2.2f, center = node.screenPos)
                }
            }
        }
    }
}
