package com.mcorder.mixin;

import com.mcorder.FFmpegManager;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_437.class})
public class ScreenMixin {
   @Inject(
      method = {"method_25394"},
      at = {@At("TAIL")}
   )
   private void onRenderScreen(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!FFmpegManager.isReady()) {
         String status = "§c[Mcorder] " + FFmpegManager.getStatusMessage();
         int w = context.method_51421();
         int h = context.method_51443();
         int tw = class_310.method_1551().field_1772.method_1727(status);
         context.method_25294(w - tw - 10, h - 20, w, h, -2013265920);
         context.method_25303(class_310.method_1551().field_1772, status, w - tw - 5, h - 15, 16777215);
      }
   }
}
