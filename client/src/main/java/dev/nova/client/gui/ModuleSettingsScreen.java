package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Module;
import dev.nova.client.setting.BooleanSetting;
import dev.nova.client.setting.NumberSetting;
import dev.nova.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/** Einstellungen eines einzelnen Moduls: Taste + alle Settings. */
public class ModuleSettingsScreen extends Screen {

    private static final int WIDGET_WIDTH = 200;

    private final Screen parent;
    private final Module module;

    private ButtonWidget keyButton;
    private boolean listeningForKey;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Text.literal(module.getName()));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = 50;

        keyButton = addDrawableChild(ButtonWidget.builder(keyLabel(), b -> {
            listeningForKey = true;
            b.setMessage(Text.literal("> Taste drücken… <").formatted(Formatting.YELLOW));
        }).dimensions(x, y, WIDGET_WIDTH, 20).build());
        y += 28;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                addDrawableChild(ButtonWidget.builder(boolLabel(bool), b -> {
                    bool.toggle();
                    b.setMessage(boolLabel(bool));
                }).dimensions(x, y, WIDGET_WIDTH, 20).build());
            } else if (setting instanceof NumberSetting number) {
                addDrawableChild(new NumberSlider(x, y, WIDGET_WIDTH, number));
            }
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(x, height - 30, WIDGET_WIDTH, 20).build());
    }

    private Text keyLabel() {
        int key = module.getKeyCode();
        Text keyName = key == Module.NO_KEY
                ? Text.literal("Keine").formatted(Formatting.GRAY)
                : InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText();
        return Text.literal("Taste: ").append(keyName);
    }

    private static Text boolLabel(BooleanSetting setting) {
        return Text.literal(setting.getName() + ": ")
                .append(setting.get() ? ScreenTexts.ON : ScreenTexts.OFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKey) {
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.setKeyCode(Module.NO_KEY);
            } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                module.setKeyCode(keyCode);
            }
            listeningForKey = false;
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFF55AAFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(module.getDescription()).formatted(Formatting.GRAY), width / 2, 28, 0xFFFFFFFF);
        if (listeningForKey) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("ESC = abbrechen, Entf = Taste entfernen").formatted(Formatting.GRAY),
                    width / 2, height - 45, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        NovaClient.get().saveConfig();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Schieberegler für {@link NumberSetting}. */
    private static class NumberSlider extends SliderWidget {

        private final NumberSetting setting;

        NumberSlider(int x, int y, int width, NumberSetting setting) {
            super(x, y, width, 20, Text.empty(), toSlider(setting));
            this.setting = setting;
            updateMessage();
        }

        private static double toSlider(NumberSetting s) {
            return (s.get() - s.getMin()) / (s.getMax() - s.getMin());
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(setting.getName() + ": " + setting.format()));
        }

        @Override
        protected void applyValue() {
            setting.set(setting.getMin() + value * (setting.getMax() - setting.getMin()));
        }
    }
}
