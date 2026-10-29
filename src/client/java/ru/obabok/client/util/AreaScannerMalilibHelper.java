package ru.obabok.client.util;

import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import ru.obabok.client.gui.screens.ConfigGui;
import ru.obabok.client.handlers.InitHandler;
import ru.obabok.common.References;

public class AreaScannerMalilibHelper {
    public static void initMalilib(){
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(References.MOD_ID, References.MOD_NAME, ConfigGui::new));
    }
}
