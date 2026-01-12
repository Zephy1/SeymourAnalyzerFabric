package schnerry.seymouranalyzer.data

/**
 * Represents a scanned armor piece with color analysis
 */
class ArmorPiece(
    var uuid: String? = null,
    var pieceName: String? = null,
    var hexcode: String? = null,
    var chestLocation: ChestLocation? = null,
    var bestMatch: BestMatch? = null,
    var allMatches: List<ColorMatch>? = null,
    var wordMatch: String? = null,
    var specialPattern: String? = null,
    var timestamp: Long = 0 // Hypixel Skyblock timestamp
) {
    // Transient fields for hex search (not serialized)
    @Transient
    var cachedSearchHex: String? = null

    @Transient
    var cachedSearchDeltaE: Double? = null

    @Transient
    var cachedSearchDistance: Int? = null

    data class ChestLocation(
        var x: Int = 0,
        var y: Int = 0,
        var z: Int = 0
    ) {
        override fun toString(): String = "$x, $y, $z"
    }

    data class BestMatch(
        var colorName: String,
        var targetHex: String,
        var deltaE: Double,
        var absoluteDistance: Int,
        var tier: Int
    )

    data class ColorMatch(
        var colorName: String,
        var targetHex: String,
        var deltaE: Double,
        var absoluteDistance: Int,
        var tier: Int,
        var isCustom: Boolean = false,
        var isFade: Boolean = false
    )

    // Convenience method for rebuild commands
    fun setBestMatch(colorName: String, targetHex: String, deltaE: Double, absoluteDistance: Int, tier: Int) {
        this.bestMatch = BestMatch(colorName, targetHex, deltaE, absoluteDistance, tier)
    }
}
