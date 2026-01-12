package schnerry.seymouranalyzer.analyzer;

import schnerry.seymouranalyzer.config.ClothConfig;
import schnerry.seymouranalyzer.config.ConfigOption

/**
 * Detects special hex patterns (paired, repeating, palindrome, AxBxCx) and word matches
 */
object PatternDetector {
    /**
     * Detect special hex pattern
     * Returns: "paired", "repeating", "palindrome", "axbxcx", or null
     */
    @JvmStatic
    fun detectPattern(hex: String?): String? {
        if (hex == null || hex.length != 6) return null

        val upperHex = hex.uppercase()
        val chars = upperHex.toCharArray()

        // Check paired (AABBCC)
        if (chars[0] == chars[1] && chars[2] == chars[3] && chars[4] == chars[5]) {
            return "paired"
        }

        // Check repeating (ABCABC)
        if (chars[0] == chars[3] && chars[1] == chars[4] && chars[2] == chars[5]) {
            return "repeating"
        }

        // Check palindrome (ABCCBA)
        if (chars[0] == chars[5] && chars[1] == chars[4] && chars[2] == chars[3]) {
            return "palindrome"
        }

        // Check AxBxCx pattern
        if (chars[0] == chars[2] && chars[2] == chars[4]) {
            return "axbxcx_${chars[0].uppercaseChar()}"
        }

        return null
    }

    /**
     * Check if hex contains a word pattern from the word list
     * Prioritizes longer word matches over shorter ones
     */
    @JvmStatic
    fun detectWordMatch(hex: String): String? {
        val config = ClothConfig.getInstance()
        if (!config.getOptionValue(ConfigOption.WORDS_ENABLED)) return null

        val upperHex = hex.uppercase()
        val wordList = config.wordList

        var longestMatch: String? = null
        var longestMatchLength = 0

        for ((word, pattern) in wordList) {
            val upperPattern = pattern.uppercase()

            if (matchesPattern(upperHex, upperPattern)) {
                // Count non-wildcard characters in the pattern to determine "length"
                val effectiveLength = upperPattern.replace("X", "").length

                if (effectiveLength > longestMatchLength) {
                    longestMatch = word
                    longestMatchLength = effectiveLength
                }
            }
        }

        return longestMatch
    }

    /**
     * Check if hex matches a pattern with X wildcards
     * Supports patterns shorter than hex (substring matching)
     * Matches old ChatTriggers behavior: checks if pattern exists anywhere in hex
     */
    @JvmStatic
    fun matchesPattern(hex: String, pattern: String): Boolean {
        // If pattern has wildcards (X), use sliding window with regex-style matching
        if (pattern.contains("X")) {
            val patternLen = pattern.length
            // Try all possible positions in the hex where this pattern could fit
            for (startIdx in 0..(hex.length - patternLen)) {
                var matches = true
                for (i in 0 until patternLen) {
                    val patternChar = pattern[i]
                    val hexChar = hex[startIdx + i]

                    // X is wildcard, anything else must match exactly
                    if (patternChar != 'X' && hexChar != patternChar) {
                        matches = false
                        break
                    }
                }
                if (matches) {
                    return true
                }
            }
            return false
        }

        // No wildcards: simple substring check (indexOf)
        return hex.contains(pattern)
    }

    /**
     * Get all pieces with a specific pattern
     */
    @SuppressWarnings("unused") // Public API method for future use
    @JvmStatic
    fun getPiecesWithPattern(patternType: String, hexcodeMap: Map<String, String>): Set<String> {
        val matches = mutableSetOf<String>()

        for ((uuid, hex) in hexcodeMap) {
            val detected = detectPattern(hex)

            if (detected != null && detected == patternType) {
                matches.add(uuid)
            }
        }

        return matches
    }

    /**
     * Get all pieces with word matches
     */
    @SuppressWarnings("unused") // Public API method for future use
    @JvmStatic
    fun getPiecesWithWords(hexcodeMap: Map<String, String>): Set<String> {
        val matches = mutableSetOf<String>()

        for ((uuid, hex) in hexcodeMap) {
            if (detectWordMatch(hex) != null) {
                matches.add(uuid)
            }
        }

        return matches
    }
}
