package schnerry.seymouranalyzer.data;

import com.google.gson.Gson
import com.google.gson.JsonObject
import schnerry.seymouranalyzer.SeymourAnalyzer
import schnerry.seymouranalyzer.util.ColorMath
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the color database with target colors and fade dyes
 */
class ColorDatabase {
    private val _targetColors = linkedMapOf<String, String>()
    private val _fadeDyes = linkedMapOf<String, String>()
    private val labCache = ConcurrentHashMap<String, ColorMath.LAB>()
    private val fadeDyeNames = mutableSetOf<String>()

    val targetColors: Map<String, String>
    get() = _targetColors

    val fadeDyes: Map<String, String>
    get() = _fadeDyes

    init {
        loadColors()
    }

    private fun loadColors() {
        // Load target colors from JSON resource
        try {
            javaClass.getResourceAsStream("/data/seymouranalyzer/colors.json")?.use { stream ->
                val json = Gson().fromJson(InputStreamReader(stream), JsonObject::class.java)

                if (json.has("TARGET_COLORS")) {
                    val colors = json.getAsJsonObject("TARGET_COLORS")
                    colors.entrySet().forEach { (key, value) ->
                        _targetColors[key] = value.asString
                    }
                }

                if (json.has("FADE_DYES")) {
                    val fades = json.getAsJsonObject("FADE_DYES")
                    fades.entrySet().forEach { (key, value) ->
                        _fadeDyes[key] = value.asString
                        fadeDyeNames.add(key.split(" - ")[0])
                    }
                }

                SeymourAnalyzer.LOGGER.info(
                    "Loaded {} target colors and {} fade dyes",
                    _targetColors.size,
                    _fadeDyes.size
                )
            }
        } catch (e: Exception) {
            SeymourAnalyzer.LOGGER.error("Failed to load color database", e)
        }
    }

    fun isFadeDye(colorName: String): Boolean {
        return fadeDyeNames.any { fadeName ->
            colorName.startsWith("$fadeName - Stage")
        }
    }

    fun getLabForHex(hex: String): ColorMath.LAB {
        return labCache.computeIfAbsent(hex.uppercase()) { ColorMath.hexToLab(it) }
    }

    fun rebuildLabCache() {
        labCache.clear()
        _targetColors.values.forEach { getLabForHex(it) }
        _fadeDyes.values.forEach { getLabForHex(it) }
    }

    fun clearLabCache() {
        labCache.clear()
    }

    companion object {
        private var INSTANCE: ColorDatabase? = null

        fun getInstance(): ColorDatabase {
            if (INSTANCE == null) {
                INSTANCE = ColorDatabase()
            }
            return INSTANCE!!
        }
    }
}
