package ru.obabok.areascanner.client.handlers;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import ru.obabok.areascanner.client.Config;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.client.gui.ConfigGui;
import ru.obabok.areascanner.client.util.RenderUtil;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
        super();
        setsCallbacks();
    }

    @Override
    public void addKeysToMap(IKeybindManager iKeybindManager) {
        Config.HOTKEYS.forEach(iHotkey -> iKeybindManager.addKeybindToMap(iHotkey.getKeybind()));
    }

    @Override
    public void addHotkeys(IKeybindManager iKeybindManager) {
        iKeybindManager.addHotkeysForCategory(References.MOD_ID, "areascanner.hotkeys.category.generic_hotkeys", Config.HOTKEYS);
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }
    private void setsCallbacks() {
        Config.Generic.MAIN.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new ConfigGui());
            return true;
        });
        Config.Generic.LOOK_RANDOM_SELECTED_BLOCK.getKeybind().setCallback((action, key) -> {
            RenderUtil.lookRandomSelectedBlock();
            return true;
        });
    }
}
