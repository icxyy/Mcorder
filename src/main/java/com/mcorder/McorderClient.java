package com.mcorder;

import com.mcorder.gui.RecordConfigScreen;
import com.mcorder.gui.VideoListScreen;
import com.mcorder.recorder.RecordingManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_304.class_11900;
import net.minecraft.class_3675.class_307;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McorderClient implements ClientModInitializer {
   public static final String MOD_ID = "mcorder";
   public static final Logger LOGGER = LoggerFactory.getLogger("mcorder");
   private static class_304 recordMenuKey;
   private static class_304 playerMenuKey;
   private static class_304 toggleHudKey;

   public void onInitializeClient() {
      LOGGER.info("Initializing Mcorder Client...");
      FFmpegManager.init(class_310.method_1551().field_1697);
      recordMenuKey = KeyBindingHelper.registerKeyBinding(new class_304("key.mcorder.record_menu", class_307.field_1668, 82, class_11900.field_62556));
      playerMenuKey = KeyBindingHelper.registerKeyBinding(new class_304("key.mcorder.player_menu", class_307.field_1668, 86, class_11900.field_62556));
      toggleHudKey = KeyBindingHelper.registerKeyBinding(new class_304("key.mcorder.toggle_hud", class_307.field_1668, 72, class_11900.field_62556));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (recordMenuKey.method_1436()) {
            if (!RecordingManager.isRecording()) {
               client.method_1507(new RecordConfigScreen());
            } else {
               RecordingManager.stopRecording();
            }
         }

         if (playerMenuKey.method_1436()) {
            client.method_1507(new VideoListScreen());
         }

         if (toggleHudKey.method_1436()) {
            RecordingManager.setHudVisible(!RecordingManager.isHudVisible());
            client.field_1705.method_1743().method_1812(class_2561.method_43470("§c[Mcorder] HUD " + (RecordingManager.isHudVisible() ? "Visible" : "Hidden")));
         }

         RecordingManager.tick();
      });
   }
}
