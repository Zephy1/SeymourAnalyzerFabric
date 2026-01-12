package schnerry.seymouranalyzer.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import schnerry.seymouranalyzer.config.ConfigScreen;
import schnerry.seymouranalyzer.gui.DatabaseScreen;

/**
 * Keybinding to open GUIs - alternative to commands
 */
public class KeyBindings {
    private static KeyMapping openDatabaseGuiKey;
    private static KeyMapping openConfigGuiKey;

    public static void register() {
        openDatabaseGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.seymouranalyzer.opendatabasegui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.seymouranalyzer"
        ));

        // I for Config GUI
        openConfigGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.seymouranalyzer.openconfiggui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "category.seymouranalyzer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openDatabaseGuiKey.consumeClick()) {
                client.setScreen(new DatabaseScreen(null));
            }

            if (openConfigGuiKey.consumeClick()) {
                client.setScreen(ConfigScreen.createConfigScreen(client.screen));
            }
        });
    }
}
