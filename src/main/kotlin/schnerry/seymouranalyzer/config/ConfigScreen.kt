package schnerry.seymouranalyzer.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Cloth Config GUI screen provider
 */
object ConfigScreen {
    fun createConfigScreen(parent: Screen): Screen {
        val config = ClothConfig.getInstance()

        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Seymour Analyzer Config"))
            .setSavingRunnable { config.save() }

        // Analysis Features Category
        builder.getOrCreateCategory(Component.literal("Analysis Features")).apply {
            addBooleanToggle("Info Box", ConfigOption.INFO_BOX_ENABLED, "Show detailed color analysis info box when hovering items")
            addBooleanToggle("Item Highlights", ConfigOption.HIGHLIGHTS_ENABLED, "Highlight items in chests based on tier and duplicate status")
            addBooleanToggle("Word Matches", ConfigOption.WORDS_ENABLED, "Detect and highlight hexes that spell words")
            addBooleanToggle("Pattern Detection", ConfigOption.PATTERNS_ENABLED, "Detect special patterns (palindrome, repeating, etc.)")
            addBooleanToggle("Duplicate Detection", ConfigOption.DUPES_ENABLED, "Warn when you have duplicate colors in your collection")
        }

        // Match Priority Editor - Create a subcategory
        builder.getOrCreateCategory(Component.literal("Match Priorities")).apply {
            addTextDescription("§7Configure which match types take priority for highlights.\n§7Drag items to reorder - higher = priority.")

            addTextDescription(buildString {
                append("§eCurrent Order:\n")
                config.matchPriorities.forEachIndexed { index, priority ->
                    append("§7${index + 1}. §f${priority.displayName}\n")
                }
            })

            addTextDescription("§c(Note: This config is available with /seymour priorities)")
        }

        // Filter Options Category
        builder.getOrCreateCategory(Component.literal("Filter Options")).apply {
            addBooleanToggle("Fade Dyes", ConfigOption.FADE_DYES_ENABLED, "Include fade dyes in color matching")
            addBooleanToggle("Custom Colors", ConfigOption.CUSTOM_COLORS_ENABLED, "Include custom colors in color matching")
            addBooleanToggle("Show High Fades", ConfigOption.SHOW_HIGH_FADES, "Show fade dye matches with ΔE > 2.00 (T3+)")
            addBooleanToggle("3-Piece Sets", ConfigOption.THREE_PIECE_SETS_ENABLED, "Show matches for 3-piece sets (helmet + chestplate + boots)")
            addBooleanToggle("Piece Specific", ConfigOption.PIECE_SPECIFIC_ENABLED, "Only show matches for the specific piece type")
        }

        // Scanning Category
        builder.getOrCreateCategory(Component.literal("Scanning")).apply {
            addBooleanToggle("Item Frames", ConfigOption.ITEM_FRAMES_ENABLED, "Enable scanning of items in item frames")
        }

        return builder.build()
    }

    private fun ConfigCategory.addBooleanToggle(
        name: String,
        configOption: ConfigOption,
        tooltip: String,
    ) {
        addEntry(
            entryBuilder().startBooleanToggle(Component.literal(name), configOption.configValue.value)
                .setDefaultValue(configOption.configValue.default)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer { configOption.configValue.value = it }
                .build()
        )
    }

    private fun ConfigCategory.addTextDescription(text: String) {
        addEntry(
            entryBuilder().startTextDescription(Component.literal(text))
                .build()
        )
    }

    private fun ConfigCategory.entryBuilder() =
        ConfigBuilder.create().entryBuilder()
}
