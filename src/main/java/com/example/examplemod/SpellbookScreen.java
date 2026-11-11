package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpellbookScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 238;
    private static final int BUTTON_WIDTH = 142;
    private static final int BUTTON_HEIGHT = 30;

    private int panelLeft;
    private int panelTop;
    private SpellbookItem.Spell hoveredSpell;

    public SpellbookScreen() {
        super(Component.translatable("screen.examplemod.spellbook.title"));
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;

        addSpellButton(SpellbookItem.Spell.ARCANE_BOLT, panelLeft + 18, panelTop + 62);
        addSpellButton(SpellbookItem.Spell.FROST_NOVA, panelLeft + 170, panelTop + 62);
        addSpellButton(SpellbookItem.Spell.BLINK, panelLeft + 18, panelTop + 100);
        addSpellButton(SpellbookItem.Spell.HEALING_LIGHT, panelLeft + 170, panelTop + 100);
    }

    private void addSpellButton(SpellbookItem.Spell spell, int x, int y) {
        Button button = Button.builder(
                        Component.translatable(spell.translationKey()),
                        ignored -> cast(spell)
                )
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(spell.descriptionKey())))
                .build();
        addRenderableWidget(button);
    }

    private void cast(SpellbookItem.Spell spell) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("screen.examplemod.spellbook.sending"), true);
        }
        PacketDistributor.sendToServer(new SpellCastPayload(spell.id()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(panelLeft - 3, panelTop - 3,
                panelLeft + PANEL_WIDTH + 3, panelTop + PANEL_HEIGHT + 3, 0xFF372611);
        graphics.fill(panelLeft, panelTop,
                panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xFFE0C898);
        graphics.fill(panelLeft + 5, panelTop + 5,
                panelLeft + PANEL_WIDTH - 5, panelTop + PANEL_HEIGHT - 5, 0xFFF0DDB4);
        graphics.fill(panelLeft + 12, panelTop + 44,
                panelLeft + PANEL_WIDTH - 12, panelTop + 45, 0xFF8F7042);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, panelTop + 17, 0xFF38230E);
        graphics.drawCenteredString(font,
                Component.translatable("screen.examplemod.spellbook.subtitle"),
                width / 2, panelTop + 32, 0xFF72552E);
        graphics.drawCenteredString(font,
                Component.translatable("screen.examplemod.spellbook.cooldown"),
                width / 2, panelTop + 143, 0xFF72552E);

        hoveredSpell = null;
        for (SpellbookItem.Spell spell : SpellbookItem.Spell.values()) {
            int column = spell.id() % 2;
            int row = spell.id() / 2;
            int x = panelLeft + 18 + column * 152;
            int y = panelTop + 62 + row * 38;
            if (mouseX >= x && mouseX < x + BUTTON_WIDTH
                    && mouseY >= y && mouseY < y + BUTTON_HEIGHT) {
                hoveredSpell = spell;
                break;
            }
        }

        Component description = Component.translatable(
                hoveredSpell == null
                        ? "screen.examplemod.spellbook.hint"
                        : hoveredSpell.descriptionKey()
        );
        int textY = panelTop + 161;
        for (var line : font.split(description, PANEL_WIDTH - 34)) {
            graphics.drawCenteredString(font, line, width / 2, textY, 0xFF49351D);
            textY += font.lineHeight;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
