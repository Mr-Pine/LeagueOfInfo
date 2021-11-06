package design

import androidx.compose.ui.graphics.Color

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
    println(lightness)
    return Color(java.awt.Color(newRGB).red, java.awt.Color(newRGB).green, java.awt.Color(newRGB).blue)
}