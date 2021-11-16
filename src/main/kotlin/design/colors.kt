package design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.sqrt

val blue = Color(0xFF003870)
val blue_light = Color(0xFF047ac9)

fun Color.darken(contrast: Float = 0.5f): Color {

    var r = if(contrast > 0) 0 else 255
    var g = if(contrast > 0) 0 else 255
    var b = if(contrast > 0) 0 else 255

    r = ((r - this.red * 255) * contrast + this.red * 255).toInt()
    g = ((g - this.green * 255) * contrast + this.green * 255).toInt()
    b = ((b - this.blue * 255) * contrast + this.blue * 255).toInt()

    return Color(r, g, b)
}

fun Color.withLightness(lightness: Float): Color {
    val hsbArray = FloatArray(3)
    java.awt.Color.RGBtoHSB(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        hsbArray
    )
    val newRGB = java.awt.Color.HSBtoRGB(hsbArray[0], hsbArray[1], lightness)
    return Color(java.awt.Color(newRGB).red, java.awt.Color(newRGB).green, java.awt.Color(newRGB).blue)
}

fun Color.getLegibleTextColor(): Color {

    val minAlphaBlack = minAlpha(this, Color.Black, 4.5f)
    val minAlphaWhite = minAlpha(this, Color.White, 4.5f)
    return if(minAlphaBlack != null)
        Color.Black.copy(alpha = sqrt(minAlphaBlack))
    else if(minAlphaWhite != null)
        Color.White.copy(alpha = sqrt(minAlphaWhite))
    else
        Color.White
}

fun blendForegroundContrast(color: Color, backColor: Color = Color.Black, blendFactor: Float): Float {
    val blended = color.copy(alpha = 1 - blendFactor).compositeOver(backColor)

    val backLuminance = blended.luminance() + 0.05f
    val luminance = color.luminance() + 0.05f
    var contrast = backLuminance / luminance
    if(luminance > backLuminance) contrast = 1 / contrast
    return contrast
}

fun minAlpha(color1: Color, color2: Color, r: Float): Float?{
    val a = blendForegroundContrast(color1, color2, 0f)
    if(a >= r) return null
    val l = blendForegroundContrast(color1, color2, 1f)
    if(r > l) return null

    var s = 0
    val m = 10
    val d = 0.01

    var n = 0f
    var c = 1f
    println("hi")
    while(m >= s && c - n > d){
        val h = (n + c) / 2f
        val p = blendForegroundContrast(color1, color2, h)
        if(r > p) n = h else c = h
        ++s
    }
    return if(s > m) null else c
}