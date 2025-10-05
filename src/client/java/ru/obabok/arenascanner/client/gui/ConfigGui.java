package ru.obabok.arenascanner.client.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import ru.obabok.arenascanner.Config;
import ru.obabok.arenascanner.References;
import java.util.Collections;
import java.util.List;

public class ConfigGui extends GuiConfigsBase {
    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;
    public ConfigGui() {
        super(10, 50, References.MOD_ID, null, "arenascanner.gui.title.configs");
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearOptions();
        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            x += this.createButton(x, y, -1, tab) + 2;
        }
        ButtonGeneric button = new ButtonGeneric(10, getScreenHeight() - 20, -1, 20, "Whitelists");
        this.addButton(button, (button1, mouseButton) -> openGui(new WhitelistSelectorScreen(this, 0)));
    }
    private int createButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(ConfigGui.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));

        return button.getWidth();
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final ConfigGui parent;
        private final ConfigGuiTab tab;

        public ButtonListener(ConfigGuiTab tab, ConfigGui parent)
        {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            ConfigGui.tab = this.tab;

            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }


    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;
        ConfigGuiTab tab = ConfigGui.tab;
        if (tab == ConfigGuiTab.GENERIC){
            configs = Config.Generic.OPTIONS;
        }else if (tab == ConfigGuiTab.HUD)
        {
            configs = Config.Hud.OPTIONS;
        }
        else
        {
            return Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }


    public enum ConfigGuiTab
    {
        GENERIC ("arenascanner.gui.title.generic"),
        HUD ("arenascanner.gui.title.hud");

        private final String translationKey;

        ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
