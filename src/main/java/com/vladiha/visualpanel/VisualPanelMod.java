package com.vladiha.visualpanel;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.vladiha.visualpanel.ui.PanelScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Visual Panel: client-only mod. Panel on V, configurable HUD, HUD editor. */
@Mod(VisualPanelMod.MODID)
public final class VisualPanelMod {
    public static final String MODID = "visualpanel";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(MODID, "hud");

    private static KeyMapping openKey;

    public VisualPanelMod(FMLJavaModLoadingContext context) {
        Settings.get();
        RegisterKeyMappingsEvent.BUS.addListener(this::onRegisterKeys);
        AddGuiOverlayLayersEvent.BUS.addListener(this::onOverlayLayers);
        TickEvent.ClientTickEvent.Post.BUS.addListener(this::onClientTick);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "main"));
        openKey = new KeyMapping("key.visualpanel.open", InputConstants.KEY_V, category);
        event.register(openKey);
    }

    private void onOverlayLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, HUD_LAYER, ForgeLayeredDraw.BOSS_OVERLAY,
            (graphics, tracker) -> HudRenderer.renderLayer(graphics));
    }

    private void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (openKey == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (openKey.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new PanelScreen());
            }
        }
    }
}
