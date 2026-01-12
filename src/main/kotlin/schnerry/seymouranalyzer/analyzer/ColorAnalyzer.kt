package schnerry.seymouranalyzer.analyzer;

import schnerry.seymouranalyzer.SeymourAnalyzer;
import schnerry.seymouranalyzer.config.ClothConfig;
import schnerry.seymouranalyzer.config.ConfigOption
import schnerry.seymouranalyzer.config.MatchPriority;
import schnerry.seymouranalyzer.data.ColorDatabase;
import schnerry.seymouranalyzer.util.ColorMath;

/**
 * Analyzes armor colors and finds best matches from the database
 */
object ColorAnalyzer {
    @JvmField
    val colorDatabase: ColorDatabase = ColorDatabase()

    /**
     * Analyze an armor piece and find best color matches
     */
    @JvmStatic
    fun analyzeArmorColor(hexcode: String, pieceName: String): AnalysisResult? {
        val config = ClothConfig.getInstance()
        val pieceType = detectPieceType(pieceName)

        val allMatches = mutableListOf<ColorMatch>()

        // Check custom colors first if enabled
        if (config.getOptionValue(ConfigOption.CUSTOM_COLORS_ENABLED)) {
            allMatches.addAll(findMatchesInMap(hexcode, pieceType, config.customColors, true, false))
        }

        // Check target colors
        allMatches.addAll(findMatchesInMap(hexcode, pieceType, colorDatabase.targetColors, false, false))

        // Check fade dyes if enabled
        if (config.getOptionValue(ConfigOption.FADE_DYES_ENABLED)) {
            allMatches.addAll(findMatchesInMap(hexcode, pieceType, colorDatabase.fadeDyes, false, true))
        }

        // Step 1: Sort all matches by deltaE to get the 10 closest color matches
        allMatches.sortBy { it.deltaE }

        // Step 2: Take top 10 closest matches by deltaE
        val top10ByDeltaE = allMatches.take(10)

        // Step 3: Separate exact matches (deltaE ~= 0) from regular matches
        // Exact matches should ALWAYS be prioritized above everything else
        val exactMatches = mutableListOf<ColorMatch>()
        val regularMatches = mutableListOf<ColorMatch>()

        for (match in top10ByDeltaE) {
            if (match.deltaE < 0.01) { // Exact match (accounting for floating point precision)
                exactMatches.add(match)
            } else {
                regularMatches.add(match)
            }
        }

        // Step 4: Split regular matches into prioritized (tier 0-2) and unprioritized (tier 3+)
        val prioritizedMatches = mutableListOf<ColorMatch>()
        val unprioritizedMatches = mutableListOf<ColorMatch>()

        for (match in regularMatches) {
            if (match.tier <= 2) {
                prioritizedMatches.add(match)
            } else {
                unprioritizedMatches.add(match)
            }
        }

        // Step 5: Sort prioritized matches by priority, then by deltaE
        prioritizedMatches.sortWith { m1, m2 ->
            val p1 = getMatchPriority(m1)
            val p2 = getMatchPriority(m2)

            val clothConfig = ClothConfig.getInstance()
            val idx1 = clothConfig.getPriorityIndex(p1)
            val idx2 = clothConfig.getPriorityIndex(p2)

            // If priorities are different, sort by priority (lower index = higher priority)
            if (idx1 != idx2) {
                idx1.compareTo(idx2)
            } else {
                // Same priority, sort by deltaE
                m1.deltaE.compareTo(m2.deltaE)
            }
        }

        // Step 6: Combine lists - exact matches FIRST, then prioritized, then unprioritized
        unprioritizedMatches.sortBy { it.deltaE }
        val finalList = mutableListOf<ColorMatch>()
        finalList.addAll(exactMatches)  // Exact matches always first
        finalList.addAll(prioritizedMatches)
        finalList.addAll(unprioritizedMatches)

        // Step 6: Get top 3 from the combined list
        val top3 = finalList.take(3)

        if (top3.isEmpty()) {
            SeymourAnalyzer.LOGGER.warn("[ColorAnalyzer] No matches found for hex: $hexcode")
            return null
        }

        val best = top3[0]
        val tier = calculateTier(best.deltaE, best.isCustom, best.isFade)

        return AnalysisResult(best, top3, tier)
    }

    @JvmStatic
    fun findMatchesInMap(
        itemHex: String,
        pieceType: String?,
        colorMap: Map<String, String>,
        isCustom: Boolean,
        isFade: Boolean
    ): List<ColorMatch> {
        val config = ClothConfig.getInstance()
        val matches = mutableListOf<ColorMatch>()
        val itemLab = colorDatabase.getLabForHex(itemHex)

        for ((colorName, targetHex) in colorMap) {
            // Piece-specific filtering
            if (config.getOptionValue(ConfigOption.PIECE_SPECIFIC_ENABLED) && !canMatchPiece(colorName, pieceType)) {
                continue
            }

            // 3-piece set filtering for top hats
            if (config.getOptionValue(ConfigOption.THREE_PIECE_SETS_ENABLED) && pieceType == "helmet") {
                if (colorName.contains("3p") && !colorName.lowercase().contains("top hat")) {
                    continue
                }
            }

            val targetLab = colorDatabase.getLabForHex(targetHex)
            val deltaE = ColorMath.calculateDeltaEWithLab(itemLab, targetLab)

            // High fade filtering - only show T0/T1 fades (deltaE <= 2.0) when disabled
            if (!config.getOptionValue(ConfigOption.SHOW_HIGH_FADES) && isFade && deltaE > 2.0) {
                continue // Skip fades with deltaE > 2.0 (T2+)
            }

            // Always add to matches (removed deltaE <= 5.0 filter to ensure we get top 3)
            val absoluteDist = ColorMath.calculateAbsoluteDistance(itemHex, targetHex)
            val tier = calculateTier(deltaE, isCustom, isFade)

            val match = ColorMatch(colorName, targetHex, deltaE, absoluteDist, tier, isCustom, isFade)
            matches.add(match)
        }

        return matches
    }

    @JvmStatic
    fun canMatchPiece(colorName: String, pieceType: String?): Boolean {
        if (pieceType == null) return true

        val lower = colorName.lowercase()

        // Special handling for multi-piece names (e.g., "Challenger's Leggings+Boots", "Speedster Set/Mercenary Boots")
        // If the name contains the current piece type, allow it
        when (pieceType) {
            "helmet" -> {
                if (lower.contains("helmet") || lower.contains("hat") || lower.contains("hood") ||
                    lower.contains("cap") || lower.contains("crown") || lower.contains("mask")) {
                    return true
                }
            }
            "chestplate" -> {
                if (lower.contains("chestplate") || lower.contains("chest") || lower.contains("tunic") ||
                    lower.contains("jacket") || lower.contains("shirt") || lower.contains("vest") ||
                    lower.contains("robe")) {
                    return true
                }
            }
            "leggings" -> {
                if (lower.contains("leggings") || lower.contains("pants") || lower.contains("trousers")) {
                    return true
                }
            }
            "boots" -> {
                if (lower.contains("boots") || lower.contains("shoes") || lower.contains("sandals") ||
                    lower.contains("sneakers")) {
                    return true
                }
            }
        }

        // If the name doesn't contain ANY piece type keywords, it's a generic color - allow it for all
        val containsAnyPieceType =
            lower.contains("helmet") || lower.contains("hat") || lower.contains("hood") ||
                    lower.contains("cap") || lower.contains("crown") || lower.contains("mask") ||
                    lower.contains("chestplate") || lower.contains("chest") || lower.contains("tunic") ||
                    lower.contains("jacket") || lower.contains("shirt") || lower.contains("vest") ||
                    lower.contains("robe") || lower.contains("leggings") || lower.contains("pants") ||
                    lower.contains("trousers") || lower.contains("boots") || lower.contains("shoes") ||
                    lower.contains("sandals") || lower.contains("sneakers") || lower.contains("3p")

        if (!containsAnyPieceType) {
            return true // Generic color, works for all piece types
        }

        // If we get here, the name contains piece type keywords but doesn't match our piece type
        return false
    }

    @JvmStatic
    fun calculateTier(deltaE: Double, isCustom: Boolean, isFade: Boolean): Int {
        if (isCustom) {
            if (deltaE <= 2) return 1  // Custom T1
            if (deltaE <= 5) return 2  // Custom T2
            return 3
        }

        if (isFade) {
            if (deltaE <= 1) return 0  // Fade T1<
            if (deltaE <= 2) return 1  // Fade T1
            if (deltaE <= 5) return 2  // Fade T2
            return 3
        }

        // Normal colors
        if (deltaE <= 1) return 0  // T1<
        if (deltaE <= 2) return 1  // T1
        if (deltaE <= 5) return 2  // T2
        return 3
    }

    @JvmStatic
    fun detectPieceType(pieceName: String?): String? {
        if (pieceName == null) return null

        val upper = pieceName.uppercase()

        if (upper.contains("HAT") || upper.contains("HELM") || upper.contains("CROWN") ||
            upper.contains("HOOD") || upper.contains("CAP") || upper.contains("MASK")) {
            return "helmet"
        }
        if (upper.contains("JACKET") || upper.contains("CHEST") || upper.contains("TUNIC") ||
            upper.contains("SHIRT") || upper.contains("VEST") || upper.contains("ROBE") ||
            upper.contains("COAT") || upper.contains("PLATE")) {
            return "chestplate"
        }
        if (upper.contains("TROUSERS") || upper.contains("LEGGINGS") || upper.contains("PANTS") ||
            upper.contains("LEGS") || upper.contains("SHORTS")) {
            return "leggings"
        }
        if (upper.contains("SHOES") || upper.contains("BOOTS") || upper.contains("SNEAKERS") ||
            upper.contains("FEET") || upper.contains("SANDALS")) {
            return "boots"
        }

        return null
    }

    /**
     * Determine the MatchPriority enum value for a ColorMatch
     * This is used to sort matches according to user priority settings
     */
    @JvmStatic
    fun getMatchPriority(match: ColorMatch): MatchPriority {
        if (match.isCustom) {
            if (match.tier == 1) return MatchPriority.CUSTOM_T1
            if (match.tier == 2) return MatchPriority.CUSTOM_T2
        }

        if (match.isFade) {
            if (match.tier == 0) return MatchPriority.FADE_T0
            if (match.tier == 1) return MatchPriority.FADE_T1
            if (match.tier == 2) return MatchPriority.FADE_T2
        }

        // Normal colors
        if (match.tier == 0) return MatchPriority.NORMAL_T0
        if (match.tier == 1) return MatchPriority.NORMAL_T1
        if (match.tier == 2) return MatchPriority.NORMAL_T2

        // Fallback to lowest priority
        return MatchPriority.NORMAL_T2
    }

    data class AnalysisResult(
        val bestMatch: ColorMatch,
        val top3Matches: List<ColorMatch>,
        val tier: Int
    )

    data class ColorMatch(
        val name: String,
        val targetHex: String,
        val deltaE: Double,
        val absoluteDistance: Int,
        val tier: Int,
        val isCustom: Boolean,
        val isFade: Boolean
    )
}
