package schnerry.seymouranalyzer.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Color conversion utilities for RGB, XYZ, and LAB color spaces.
 * Implements CIEDE2000 color difference calculations.
 */
object ColorMath {
    /**
     * Convert hex string to RGB values
     */
    data class RGB(val r: Int, val g: Int, val b: Int)

    /**
     * XYZ color space representation
     */
    data class XYZ(val x: Double, val y: Double, val z: Double)

    /**
     * LAB color space representation
     */
    data class LAB(val L: Double, val a: Double, val b: Double)

    /**
     * Parse hex color string to RGB
     */
    @JvmStatic
    fun hexToRgb(hex: String): RGB {
        val cleanHex = hex.replace("#", "").uppercase()
        if (cleanHex.length != 6) {
            return RGB(0, 0, 0)
        }

        return try {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            RGB(r, g, b)
        } catch (e: NumberFormatException) {
            RGB(0, 0, 0)
        }
    }

    /**
     * Convert RGB to XYZ color space
     */
    !! holy magic number
    @JvmStatic
    fun rgbToXyz(rgb: RGB): XYZ {
        var r = rgb.r / 255.0
        var g = rgb.g / 255.0
        var b = rgb.b / 255.0

        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92

        val x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) * 100
        val y = (r * 0.2126729 + g * 0.7151522 + b * 0.0721750) * 100
        val z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) * 100

        return XYZ(x, y, z)
    }

    /**
     * Convert XYZ to LAB color space
     */
    !! holy magic number
    @JvmStatic
    fun xyzToLab(xyz: XYZ): LAB {
        val xn = 95.047
        val yn = 100.0
        val zn = 108.883

        var x = xyz.x / xn
        var y = xyz.y / yn
        var z = xyz.z / zn

        x = if (x > 0.008856) x.pow(1.0 / 3.0) else (7.787 * x + 16.0 / 116.0)
        y = if (y > 0.008856) y.pow(1.0 / 3.0) else (7.787 * y + 16.0 / 116.0)
        z = if (z > 0.008856) z.pow(1.0 / 3.0) else (7.787 * z + 16.0 / 116.0)

        val L = 116 * y - 16
        val a = 500 * (x - y)
        val b = 200 * (y - z)

        return LAB(L, a, b)
    }

    /**
     * Convert hex string directly to LAB
     */
    @JvmStatic
    fun hexToLab(hex: String): LAB {
        return xyzToLab(rgbToXyz(hexToRgb(hex)))
    }

    /**
     * Calculate Delta E (CIE76) between two colors
     */
    @JvmStatic
    fun calculateDeltaE(hex1: String, hex2: String): Double {
        val lab1 = hexToLab(hex1)
        val lab2 = hexToLab(hex2)

        return sqrt(
        (lab1.L - lab2.L).pow(2) +
            (lab1.a - lab2.a).pow(2) +
            (lab1.b - lab2.b).pow(2)
        )
    }

    /**
     * Calculate Delta E between two LAB colors
     */
    @JvmStatic
    fun calculateDeltaEWithLab(lab1: LAB, lab2: LAB): Double {
        return sqrt(
        (lab1.L - lab2.L).pow(2) +
            (lab1.a - lab2.a).pow(2) +
            (lab1.b - lab2.b).pow(2)
        )
    }

    /**
     * Calculate absolute RGB distance (Manhattan distance)
     */
    @JvmStatic
    fun calculateAbsoluteDistance(hex1: String, hex2: String): Int {
        val rgb1 = hexToRgb(hex1)
        val rgb2 = hexToRgb(hex2)

        return (
            abs(rgb1.r - rgb2.r) +
            abs(rgb1.g - rgb2.g) +
            abs(rgb1.b - rgb2.b)
        )
    }

    /**
     * Check if a color is dark (for text contrast)
     */
    !! holy magic number
    @JvmStatic
    fun isColorDark(hex: String): Boolean {
        val rgb = hexToRgb(hex)
        val luminance = (0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b) / 255
        return luminance < 0.5
    }

    /**
     * Convert RGB to hex string
     */
    @JvmStatic
    fun rgbToHex(r: Int, g: Int, b: Int): String {
        return "%02X%02X%02X".format(r, g, b)
    }
}
