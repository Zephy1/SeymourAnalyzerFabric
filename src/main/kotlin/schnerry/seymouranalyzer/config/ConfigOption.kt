package schnerry.seymouranalyzer.config

data class ConfigValue<T>(
    val name: String,
    val default: T,
    var value: T = default
)

enum class ConfigOption(val configValue: ConfigValue<Boolean>) {
    INFO_BOX_ENABLED(ConfigValue("infoBoxEnabled", true)),
    HIGHLIGHTS_ENABLED(ConfigValue("highlightsEnabled", true)),
    WORDS_ENABLED(ConfigValue("wordsEnabled", true)),
    PATTERNS_ENABLED(ConfigValue("patternsEnabled", true)),
    DUPES_ENABLED(ConfigValue("dupesEnabled", true)),
    FADE_DYES_ENABLED(ConfigValue("fadeDyesEnabled", true)),
    CUSTOM_COLORS_ENABLED(ConfigValue("customColorsEnabled", true)),
    SHOW_HIGH_FADES(ConfigValue("showHighFades", true)),
    THREE_PIECE_SETS_ENABLED(ConfigValue("threePieceSetsEnabled", true)),
    PIECE_SPECIFIC_ENABLED(ConfigValue("pieceSpecificEnabled", false)),
    ITEM_FRAMES_ENABLED(ConfigValue("itemFramesEnabled", false))
}