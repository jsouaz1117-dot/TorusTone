package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.example.model.TouchMode
import com.example.ui.theme.BrassGold
import com.example.ui.theme.HarmonicCyan
import com.example.ui.theme.MinorRose
import com.example.ui.theme.MonoFontFamily
import kotlin.math.hypot
import kotlin.math.sqrt

private data class LatticeNode(
    val col: Int,
    val row: Int,
    val pitchClass: Int,
    val pos: Offset
)

private data class LatticeTriad(
    val rootPitchClass: Int,
    val isMinor: Boolean,
    val p1: Offset,
    val p2: Offset,
    val p3: Offset,
    val centroid: Offset,
    val pitchClasses: Set<Int>
)

/**
 * Interactive unfolded Tonnetz Triangular Lattice.
 * Horizontal = Perfect Fifths (+7), Up-Right = Major Thirds (+4), Down-Right = Minor Thirds (+3).
 * Upward triangles = Major Triads (Brass Gold); Downward triangles = Minor Triads (Minor Rose).
 * Supports both precision tapping and continuous drag glissando across nodes and triad faces.
 */
@Composable
fun TonnetzGridCanvas(
    activePitchClasses: Set<Int>,
    scale: MusicalScale,
    rootPitchClass: Int,
    touchMode: TouchMode,
    onTriadPressed: (rootPitchClass: Int, isMinor: Boolean) -> Unit,
    onSingleNotePressed: (pitchClass: Int) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val bgDeep = MaterialTheme.colorScheme.background
    val surfaceElevated = MaterialTheme.colorScheme.surfaceContainerHigh
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    var cachedNodes by remember { mutableStateOf<List<LatticeNode>>(emptyList()) }
    var cachedTriads by remember { mutableStateOf<List<LatticeTriad>>(emptyList()) }
    var lastHitKey by remember { mutableStateOf<String?>(null) }

    fun handlePointerPosition(pos: Offset) {
        if (touchMode == TouchMode.SINGLE_NOTE) {
            val nearest = cachedNodes.minByOrNull {
                hypot((it.pos.x - pos.x).toDouble(), (it.pos.y - pos.y).toDouble())
            } ?: return
            val key = "N:${nearest.pitchClass}"
            if (key != lastHitKey) {
                lastHitKey = key
                onSingleNotePressed(nearest.pitchClass)
            }
        } else {
            // Check if pointer is inside a triad face first, fallback to nearest triad centroid
            val hitTriad = cachedTriads.firstOrNull { pointInTriangle(pos, it.p1, it.p2, it.p3) }
                ?: cachedTriads.minByOrNull {
                    hypot((it.centroid.x - pos.x).toDouble(), (it.centroid.y - pos.y).toDouble())
                }
            if (hitTriad != null) {
                val key = "T:${hitTriad.rootPitchClass}:${hitTriad.isMinor}"
                if (key != lastHitKey) {
                    lastHitKey = key
                    onTriadPressed(hitTriad.rootPitchClass, hitTriad.isMinor)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tonnetz_grid_canvas")
            .pointerInput(touchMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    lastHitKey = null
                    handlePointerPosition(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        if (activeChange != null) {
                            activeChange.consume()
                            handlePointerPosition(activeChange.position)
                        } else {
                            lastHitKey = null
                            onReleased()
                            break
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cols = 6
            val rows = 6

            val cellW = (w - 56f) / (cols - 0.5f)
            val cellH = minOf(cellW * (sqrt(3f) / 2f), (h - 56f) / (rows - 1).coerceAtLeast(1))
            val actualStepX = minOf(cellW, cellH / (sqrt(3f) / 2f))
            val actualStepY = actualStepX * (sqrt(3f) / 2f)

            val totalGridW = (cols - 0.5f) * actualStepX
            val totalGridH = (rows - 1) * actualStepY
            val startX = (w - totalGridW) * 0.5f
            val startY = (h - totalGridH) * 0.5f

            // Canonical Tonnetz pitch formula:
            // Horizontal right = +7 (Perfect Fifth), Down-right = +3 (Minor Third), Up-right = +4 (Major Third)
            val exactNodes = Array(rows) { r ->
                Array(cols) { c ->
                    val offsetX = if (r % 2 != 0) actualStepX * 0.5f else 0f
                    val px = startX + c * actualStepX + offsetX
                    val py = startY + r * actualStepY
                    val pc = TonnetzMath.positiveMod12(rootPitchClass + 7 * c + 3 * (r % 2) - (r / 2))
                    LatticeNode(c, r, pc, Offset(px, py))
                }
            }

            val triads = ArrayList<LatticeTriad>()
            for (r in 0 until rows - 1) {
                for (c in 0 until cols) {
                    if (r % 2 == 0) {
                        // Downward Minor Triad: (c, r), (c+1, r), (c, r+1) rooted at (c, r)
                        if (c + 1 < cols) {
                            val a = exactNodes[r][c]
                            val b = exactNodes[r][c + 1]
                            val d = exactNodes[r + 1][c]
                            triads.add(
                                LatticeTriad(
                                    rootPitchClass = a.pitchClass,
                                    isMinor = true,
                                    p1 = a.pos,
                                    p2 = b.pos,
                                    p3 = d.pos,
                                    centroid = Offset((a.pos.x + b.pos.x + d.pos.x) / 3f, (a.pos.y + b.pos.y + d.pos.y) / 3f),
                                    pitchClasses = setOf(a.pitchClass, b.pitchClass, d.pitchClass)
                                )
                            )
                        }
                        // Upward Major Triad: (c-1, r+1), (c, r+1), (c, r) rooted at (c-1, r+1)
                        if (c - 1 >= 0) {
                            val a = exactNodes[r + 1][c - 1]
                            val b = exactNodes[r + 1][c]
                            val top = exactNodes[r][c]
                            triads.add(
                                LatticeTriad(
                                    rootPitchClass = a.pitchClass,
                                    isMinor = false,
                                    p1 = a.pos,
                                    p2 = b.pos,
                                    p3 = top.pos,
                                    centroid = Offset((a.pos.x + b.pos.x + top.pos.x) / 3f, (a.pos.y + b.pos.y + top.pos.y) / 3f),
                                    pitchClasses = setOf(a.pitchClass, b.pitchClass, top.pitchClass)
                                )
                            )
                        }
                    } else {
                        // Odd row:
                        // Downward Minor Triad: (c, r), (c+1, r), (c+1, r+1) rooted at (c, r)
                        if (c + 1 < cols) {
                            val a = exactNodes[r][c]
                            val b = exactNodes[r][c + 1]
                            val bot = exactNodes[r + 1][c + 1]
                            triads.add(
                                LatticeTriad(
                                    rootPitchClass = a.pitchClass,
                                    isMinor = true,
                                    p1 = a.pos,
                                    p2 = b.pos,
                                    p3 = bot.pos,
                                    centroid = Offset((a.pos.x + b.pos.x + bot.pos.x) / 3f, (a.pos.y + b.pos.y + bot.pos.y) / 3f),
                                    pitchClasses = setOf(a.pitchClass, b.pitchClass, bot.pitchClass)
                                )
                            )
                        }
                        // Upward Major Triad: (c, r+1), (c+1, r+1), (c, r) rooted at (c, r+1)
                        if (c + 1 < cols) {
                            val a = exactNodes[r + 1][c]
                            val b = exactNodes[r + 1][c + 1]
                            val top = exactNodes[r][c]
                            triads.add(
                                LatticeTriad(
                                    rootPitchClass = a.pitchClass,
                                    isMinor = false,
                                    p1 = a.pos,
                                    p2 = b.pos,
                                    p3 = top.pos,
                                    centroid = Offset((a.pos.x + b.pos.x + top.pos.x) / 3f, (a.pos.y + b.pos.y + top.pos.y) / 3f),
                                    pitchClasses = setOf(a.pitchClass, b.pitchClass, top.pitchClass)
                                )
                            )
                        }
                    }
                }
            }

            cachedNodes = exactNodes.flatMap { it.toList() }
            cachedTriads = triads

            // Draw triangular chord faces
            for (triad in triads) {
                val path = Path().apply {
                    moveTo(triad.p1.x, triad.p1.y)
                    lineTo(triad.p2.x, triad.p2.y)
                    lineTo(triad.p3.x, triad.p3.y)
                    close()
                }
                val isSounding = triad.pitchClasses.all { it in activePitchClasses }
                val allInScale = triad.pitchClasses.all {
                    TonnetzMath.positiveMod12(it - rootPitchClass) in scale.intervals
                }
                val triadAccent = if (triad.isMinor) MinorRose else BrassGold

                val fillColor = when {
                    isSounding -> triadAccent.copy(alpha = 0.34f)
                    allInScale -> triadAccent.copy(alpha = 0.08f)
                    else -> surfaceElevated.copy(alpha = 0.22f)
                }
                val strokeColor = when {
                    isSounding -> triadAccent.copy(alpha = 0.92f)
                    allInScale -> outlineColor.copy(alpha = 0.72f)
                    else -> outlineColor.copy(alpha = 0.34f)
                }

                drawPath(path = path, color = fillColor, style = Fill)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = if (isSounding) 3.0f else 1.2f)
                )

                // Triad micro-label at triangle center in Triad touch mode
                if (touchMode == TouchMode.TRIAD && (isSounding || allInScale)) {
                    val chordTag = TonnetzMath.noteName(triad.rootPitchClass) + if (triad.isMinor) "m" else ""
                    val measured = textMeasurer.measure(
                        text = chordTag,
                        style = TextStyle(
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isSounding) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSounding) triadAccent else triadAccent.copy(alpha = 0.58f)
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            triad.centroid.x - measured.size.width / 2f,
                            triad.centroid.y - measured.size.height / 2f
                        )
                    )
                }
            }

            // Draw glowing pitch vertices on top of the lattice
            for (node in cachedNodes) {
                val isActive = node.pitchClass in activePitchClasses
                val relPc = TonnetzMath.positiveMod12(node.pitchClass - rootPitchClass)
                val inScale = relPc in scale.intervals
                val isRoot = node.pitchClass == rootPitchClass

                val nodeRadius = when {
                    isActive -> 19f
                    isRoot -> 16f
                    inScale -> 14.5f
                    else -> 11.5f
                }

                if (isActive) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(HarmonicCyan.copy(alpha = 0.55f), Color.Transparent),
                            center = node.pos,
                            radius = nodeRadius * 2.1f
                        ),
                        radius = nodeRadius * 2.1f,
                        center = node.pos
                    )
                }

                val fill = when {
                    isActive -> HarmonicCyan
                    isRoot -> BrassGold.copy(alpha = 0.92f)
                    inScale -> surfaceElevated
                    else -> bgDeep
                }
                val stroke = when {
                    isActive -> Color.White
                    isRoot -> BrassGold
                    inScale -> HarmonicCyan.copy(alpha = 0.6f)
                    else -> outlineColor.copy(alpha = 0.5f)
                }

                drawCircle(color = fill, radius = nodeRadius, center = node.pos)
                drawCircle(
                    color = stroke,
                    radius = nodeRadius,
                    center = node.pos,
                    style = Stroke(width = if (isActive || isRoot) 2.2f else 1.3f)
                )

                val label = TonnetzMath.noteName(node.pitchClass)
                val measured = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontFamily = MonoFontFamily,
                        fontSize = if (isActive) 11.sp else 10.sp,
                        fontWeight = if (isActive || isRoot) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isActive -> bgDeep
                            isRoot -> bgDeep
                            inScale -> textPrimary
                            else -> textMuted.copy(alpha = 0.7f)
                        }
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        node.pos.x - measured.size.width / 2f,
                        node.pos.y - measured.size.height / 2f
                    )
                )
            }
        }
    }
}

private fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val d1 = sign(p, a, b)
    val d2 = sign(p, b, c)
    val d3 = sign(p, c, a)
    val hasNeg = (d1 < 0f) || (d2 < 0f) || (d3 < 0f)
    val hasPos = (d1 > 0f) || (d2 > 0f) || (d3 > 0f)
    return !(hasNeg && hasPos)
}

private fun sign(p1: Offset, p2: Offset, p3: Offset): Float {
    return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
}
