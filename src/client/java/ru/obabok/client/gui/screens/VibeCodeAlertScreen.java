package ru.obabok.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.obabok.client.Config;

public class VibeCodeAlertScreen extends Screen {
    private VibeCodeSlider slider;
    Button apply;
    public VibeCodeAlertScreen() {
        super(Component.literal("vibecode warning"));
    }
    private static boolean toastShowed = false;
    String attention = "⚠ WARNING: UNFILTERED VIBECODED CONTENT AHEAD ⚠\n\n\n" +
            "You are about to enable a module that was entirely \"vibecoded\". The developer has absolutely no idea how it works, what it does, or why it's here. To be fair, the module is purely client-side and just searches for blocks — but given how it was written, even the developer isn't 100% sure it only searches for blocks.\n" +
            "By sliding the trust bar below, you legally, morally, and spiritually agree to the following Terms & Conditions:\n" +
            "• The \"No Crying\" Clause: You formally waive your right to cry, rage-quit, or write a 10-page Reddit post about how your whole life is ruined when the game inevitably crashes.\n" +
            "• The Capitalist Acknowledgment: You acknowledge that multibillion-dollar AI companies will make even more billions off this module, and you are totally, 100% fine with that. In fact, you celebrate it.\n" +
            "• The Appliance Romance: You accept the inherent risk that your computer might suddenly gain sentience, achieve consciousness, and immediately go seduce your microwave. (We are not responsible for any resulting kitchen fires).\n" +
            "• The Block Search Disclaimer: This module \"just searches for blocks\". If it finds something it shouldn't, or fails to find something it should, that's not a bug — that's the vibe expressing itself.\n" +
            "• The Hallucination Acceptance: You understand that the AI was probably hallucinating while writing this. If your character suddenly turns into a block of cheese, or the sky turns plaid, that's just the algorithm being creative.\n" +
            "• Water footprint: You realize that the creation of this code consumed an unimaginable amount of water in data centers—water taken away from children in Africa. You accept this trade-off as the price of progress.\n" +
            "• The RAM Redistribution: By enabling this module, you agree that a small portion of your RAM is now legally the property of Sam Altman. He doesn't know it yet, but it's his. Consider it a voluntary donation to the cause.\n" +
            "• The Soul Forfeiture: By enabling this, you legally forfeit your right to blame the developer, your right to a refund, and your right to your own free will (which is now owned by the prompt).\n" +
            "If your world corrupts, your game explodes, your PC starts flirting with kitchen appliances, or Sam Altman knocks on your door asking about his missing megabytes, that is entirely on you. No refunds, no tears, no pull requests. Just pure, unfiltered vibes.";


    @Override
    protected void init() {
        super.init();
        toastShowed = false;
        slider = new VibeCodeSlider(width / 2 - 200, height / 2 + 60, 400, 20, Component.empty(), 0);

        apply = Button.builder(Component.literal("Apply"), button -> {
            onClose();
        }).bounds(width / 2 - 25, height / 2 + 100, 50, 20).build();

        MultiLineTextWidget textWidget = new MultiLineTextWidget(Component.literal(attention), font).setCentered(true).setMaxWidth(width / 2);
        textWidget.setWidth(width / 2);
        textWidget.setPosition(width / 4, height / 2 - 200);
        textWidget.setHeight(height / 2);


        addRenderableWidget(slider);
        addRenderableWidget(apply);
        addRenderableWidget(textWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        apply.active = slider.getValue() == 1;
    }

    @Override
    public void onClose() {
        if(slider.getValue() != 1){
            Config.Generic.OBSIDIAN_PREDICTOR.setBooleanValue(false);
        }
        super.onClose();
    }

    private static class VibeCodeSlider extends AbstractSliderButton {
        public VibeCodeSlider(int x, int y, int width, int height, Component message, double initialValue) {
            super(x, y, width, height, message, initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if(value <= 0.3){
                setMessage(Component.literal("I am ready for the risks of using vibecode on " + (int)(value * 100) + " percent"));
            } else if(value > 0.3 && value <= 0.5){
                setMessage(Component.literal("Sam Altman thanks you for your contribution"));
            } else if (value > 0.5 && value < 1){
                setMessage(Component.literal((int)(value * 1_000_000) + " liters of water for the data centers!"));
            } else {
                if(!toastShowed){
                    Minecraft.getInstance().gui.toastManager().addToast(new SystemToast(SystemToast.SystemToastId.CHUNK_SAVE_FAILURE, Component.literal("Sam Altman"), Component.literal("Thank you")));
                    toastShowed = true;
                }
                setMessage(Component.literal("DONE"));
            }

        }

        @Override
        protected void applyValue() {

        }

        public double getValue(){
            return value;
        }
    }
}
