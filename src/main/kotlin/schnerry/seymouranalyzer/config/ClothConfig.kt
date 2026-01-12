package schnerry.seymouranalyzer.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import schnerry.seymouranalyzer.SeymourAnalyzer;
import schnerry.seymouranalyzer.render.ItemSlotHighlighter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Configuration class compatible with Cloth Config
 * Stores all mod settings with proper getters/setters
 */
class ClothConfig private constructor() {
    private val configDir: File
    private val configFile: File
    private val dataFile: File

    private val allConfigValues get() = ConfigOption.entries.map { it.configValue }

    // Match priorities - Higher in list = higher priority for highlights
    var matchPriorities: MutableList<MatchPriority> = getDefaultMatchPriorities()
        set(value) {
            field = value
            // Clear highlight cache so items re-calculate with new priorities
            ItemSlotHighlighter.getInstance().clearCache()
        }

    // Custom data
    val customColors: MutableMap<String, String> = mutableMapOf()
    val wordList: MutableMap<String, String> = mutableMapOf()

    init {
        configDir = File(FabricLoader.getInstance().configDir.toFile(), "seymouranalyzer")
        configFile = File(configDir, "config.json")
        dataFile = File(configDir, "data.json")

        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        load()
    }

    fun load() {
        try {
            if (configFile.exists()) {
                val json = GSON.fromJson(FileReader(configFile), JsonObject::class.java)

                allConfigValues.forEach { config ->
                    if (json.has(config.name)) {
                        config.value = json.get(config.name).asBoolean
                    }
                }

                if (json.has("matchPriorities")) {
                    matchPriorities = mutableListOf()
                    json.getAsJsonArray("matchPriorities").forEach { element ->
                        MatchPriority.fromName(element.asString)?.let { priority ->
                            matchPriorities.add(priority)
                        }
                    }
                    // Add any missing priorities at the end
                    for (priority in MatchPriority.entries) {
                        if (!matchPriorities.contains(priority)) {
                            matchPriorities.add(priority)
                        }
                    }
                }

                SeymourAnalyzer.LOGGER.info("Loaded config from file")
            }
        } catch (e: Exception) {
            SeymourAnalyzer.LOGGER.error("Failed to load config", e)
        }

        // Load custom data
        try {
            if (dataFile.exists()) {
                val json = GSON.fromJson(FileReader(dataFile), JsonObject::class.java)

                if (json.has("customColors")) {
                    val colors = json.getAsJsonObject("customColors")
                    colors.entrySet().forEach { (key, value) ->
                        customColors[key] = value.asString
                    }
                }

                if (json.has("wordList")) {
                    val words = json.getAsJsonObject("wordList")
                    words.entrySet().forEach { (key, value) ->
                        wordList[key] = value.asString
                    }
                }
            }
        } catch (e: Exception) {
            SeymourAnalyzer.LOGGER.error("Failed to load data", e)
        }
    }

    fun save() {
        try {
            val json = JsonObject()

            // Save all config values
            allConfigValues.forEach { config ->
                json.addProperty(config.name, config.value as Boolean)
            }

            val prioritiesArray = com.google.gson.JsonArray()
            matchPriorities.forEach { priority -> prioritiesArray.add(priority.name) }
            json.add("matchPriorities", prioritiesArray)

            FileWriter(configFile).use { writer ->
                GSON.toJson(json, writer)
            }

            SeymourAnalyzer.LOGGER.info("Saved config to file")
        } catch (e: Exception) {
            SeymourAnalyzer.LOGGER.error("Failed to save config", e)
        }
    }

    fun saveData() {
        try {
            val json = JsonObject()

            val colors = JsonObject()
            customColors.forEach { (key, value) -> colors.addProperty(key, value) }
            json.add("customColors", colors)

            val words = JsonObject()
            wordList.forEach { (key, value) -> words.addProperty(key, value) }
            json.add("wordList", words)

            FileWriter(dataFile).use { writer ->
                GSON.toJson(json, writer)
            }
        } catch (e: Exception) {
            SeymourAnalyzer.LOGGER.error("Failed to save data", e)
        }
    }

    fun getOptionValue(option: ConfigOption): Boolean {
        return option.configValue.value
    }

    /**
     * Get priority index (lower number = higher priority)
     * Returns -1 if not found
     */
    fun getPriorityIndex(priority: MatchPriority): Int {
        return matchPriorities.indexOf(priority)
    }

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()
        private var INSTANCE: ClothConfig? = null

        @JvmStatic
        fun getInstance(): ClothConfig {
            if (INSTANCE == null) {
                INSTANCE = ClothConfig()
            }
            return INSTANCE!!
        }

        /**
         * Default priority order
         * Search > Dupe > Word > Pattern > Custom T1/T2 > Normal T0/T1/T2 > Fade T0/T1/T2
         * Note: Custom colors only have T1 and T2 (no T0)
         */
        @JvmStatic
        fun getDefaultMatchPriorities(): MutableList<MatchPriority> {
            return mutableListOf(
                MatchPriority.SEARCH,
                MatchPriority.DUPE,
                MatchPriority.WORD,
                MatchPriority.PATTERN,
                MatchPriority.CUSTOM_T1,
                MatchPriority.CUSTOM_T2,
                MatchPriority.NORMAL_T0,
                MatchPriority.NORMAL_T1,
                MatchPriority.NORMAL_T2,
                MatchPriority.FADE_T0,
                MatchPriority.FADE_T1,
                MatchPriority.FADE_T2
            )
        }
    }
}
