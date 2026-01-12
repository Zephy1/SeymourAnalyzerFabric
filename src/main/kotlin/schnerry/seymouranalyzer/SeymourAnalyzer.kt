package schnerry.seymouranalyzer

import schnerry.seymouranalyzer.config.ClothConfig
import schnerry.seymouranalyzer.data.CollectionManager
import schnerry.seymouranalyzer.data.ColorDatabase
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeymourAnalyzer : ModInitializer {
	override fun onInitialize() {
		LOGGER.info("Initializing Seymour Analyzer...")

		// Load config
		ClothConfig.getInstance().load()

		// Initialize color database
		ColorDatabase.getInstance()

		// Initialize collection
		CollectionManager.getInstance()

		LOGGER.info("Seymour Analyzer initialized successfully!")
	}

	companion object {
		const val MOD_ID = "seymouranalyzer"

		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	}
}
