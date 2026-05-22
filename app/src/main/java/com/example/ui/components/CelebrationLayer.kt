package com.example.ui.components

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CelebrationParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val alpha: Float,
    val rotationSpeed: Float,
    val swaySpeed: Float,
    val swayWidth: Float,
    val isFlower: Boolean
) {
    var rotation = Random.nextFloat() * 360f
    var swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2f
}

@Composable
fun CelebrationLayer(
    active: Boolean,
    primaryColor: Color,
    accentColor: Color
) {
    if (!active) return

    val particles = remember(active) {
        List(80) {
            val isFlower = Random.nextInt(10) < 5 // 50% flowers, 50% confetti
            val colors = listOf(
                primaryColor,
                accentColor,
                Color(0xFFFFD700), // Gold
                Color(0xFFFF4081), // Pink Rose
                Color(0xFFEA80FC), // Light Violet
                Color(0xFF00E676), // Lime Green
                Color(0xFF00B0FF)  // Deep Sky Blue
            )
            val chosenColor = colors.random()
            CelebrationParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1.5f, // spread above the screen
                size = Random.nextFloat() * 12f + 8f,
                speed = Random.nextFloat() * 3.5f + 1.8f,
                r = chosenColor.red,
                g = chosenColor.green,
                b = chosenColor.blue,
                alpha = Random.nextFloat() * 0.4f + 0.6f,
                rotationSpeed = Random.nextFloat() * 4f - 2f,
                swaySpeed = Random.nextFloat() * 0.04f + 0.01f,
                swayWidth = Random.nextFloat() * 35f + 10f,
                isFlower = isFlower
            )
        }
    }

    var repaintTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(active) {
        while (active) {
            withFrameMillis {
                particles.forEach { p ->
                    // Advance position
                    p.y += p.speed * 0.0018f
                    p.rotation += p.rotationSpeed
                    p.swayPhase += p.swaySpeed

                    // Reset when falling way below screen height
                    if (p.y > 1.15f) {
                        p.y = -0.15f
                        p.x = Random.nextFloat()
                    }
                }
                repaintTrigger++
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Redraw triggered by repaintTrigger change
        val trigger = repaintTrigger
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = p.y * height
            val swayX = sin(p.swayPhase) * p.swayWidth
            val currentX = p.x * width + swayX

            val color = Color(p.r, p.g, p.b, p.alpha)

            withTransform({
                translate(currentX, currentY)
                rotate(p.rotation)
            }) {
                if (p.isFlower) {
                    // Draw a 5-petal sakura / cute neon flower
                    val petalRadius = p.size / 2f
                    val centerRadius = p.size / 4f

                    // Draw petals arranged radially
                    for (i in 0 until 5) {
                        val angle = i * (2f * Math.PI) / 5f
                        val px = (petalRadius * cos(angle)).toFloat()
                        val py = (petalRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = color,
                            radius = petalRadius * 0.85f,
                            center = Offset(px, py)
                        )
                    }
                    // Central golden-bright pistil core
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = centerRadius
                    )
                } else {
                    // Draw dynamic rectangle confetti
                    drawRect(
                        color = color,
                        size = Size(p.size * 1.6f, p.size * 0.6f)
                    )
                }
            }
        }
    }
}
