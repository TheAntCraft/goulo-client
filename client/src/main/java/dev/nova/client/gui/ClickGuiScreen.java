package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Modul-Menü (Standard: Rechte Shift-Taste).
 * Links Kategorien, rechts die Module der gewählten Kategorie.
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;
    private static final int SIDEBAR_WIDTH = 90;
    private static final int ROW_HEIGHT = 22;

    private static Category selected = Category.HUD; // bleibt beim erneuten Öffnen erhalten

    private int left;
    private int top;

    public ClickGuiScreen() {
        super(Text.literal(NovaClient.NAME));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;

        // Kategorien
        int y = top + 24;
        for (Category category : Category.values()) {
            Text label = category == selected
                    ? Text.literal(category.getDisplayName()).formatted(Formatting.AQUA)
                    : Text.literal(category.getDisplayName());
            addDrawableChild(ButtonWidget.builder(label, b -> {
                selected = category;
                clearAndInit();
            }).dimensions(left + 6, y, SIDEBAR_WIDTH - 12, 20).build());
            y += ROW_HEIGHT;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("HUD-Editor"),
                        b -> client.setScreen(new HudEditorScreen(this)))
                .dimensions(left + 6, top + PANEL_HEIGHT - 26, SIDEBAR_WIDTH - 12, 20).build());

        // Module
        int moduleX = left + SIDEBAR_WIDTH + 6;
        int moduleWidth = PANEL_WIDTH - SIDEBAR_WIDTH - 12;
        y = top + 24;
        for (Module module : NovaClient.get().getModuleManager().getModules(selected)) {
            if (y + 20 > top + PANEL_HEIGHT - 6) break; // TODO: Scrollen, wenn es viele Module werden

            addDrawableChild(ButtonWidget.builder(moduleLabel(module), b -> {
                        module.toggle();
                        b.setMessage(moduleLabel(module));
                    })
                    .dimensions(moduleX, y, moduleWidth - 24, 20)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(module.getDescription())))
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚙"),
                            b -> client.setScreen(new ModuleSettingsScreen(this, module)))
                    .dimensions(moduleX + moduleWidth - 20, y, 20, 20).build());
            y += ROW_HEIGHT;
        }
    }

    private static Text moduleLabel(Module module) {
        return Text.literal(module.getName() + "  ")
                .append(module.isEnabled()
                        ? Text.literal("AN").formatted(Formatting.GREEN)
                        : Text.literal("AUS").formatted(Formatting.RED));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // Hintergrund + Buttons
        // Rahmen und Titel über den Hintergrund, aber Buttons bleiben klickbar
        context.drawBorder(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF55AAFF);
        context.drawTextWithShadow(textRenderer, NovaClient.NAME + " " + NovaClient.getVersion(),
                left + 6, top + 8, 0xFF55AAFF);
        context.fill(left + SIDEBAR_WIDTH, top + 20, left + SIDEBAR_WIDTH + 1, top + PANEL_HEIGHT - 4, 0x8055AAFF);

        List<Module> list = NovaClient.get().getModuleManager().getModules(selected);
        if (list.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Noch keine Module in dieser Kategorie").formatted(Formatting.GRAY),
                    left + SIDEBAR_WIDTH + (PANEL_WIDTH - SIDEBAR_WIDTH) / 2, top + PANEL_HEIGHT / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public void removed() {
        NovaClient.get().saveConfig();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
