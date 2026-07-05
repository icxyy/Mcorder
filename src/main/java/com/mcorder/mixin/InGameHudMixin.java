package com.mcorder.mixin;

import com.mcorder.FFmpegManager;
import com.mcorder.recorder.RecordingManager;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_329.class})
public class InGameHudMixin {
   @Inject(
      method = {"method_1753"},
      at = {@At("TAIL")}
   )
   private void onRenderHud(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
      if (RecordingManager.isHudVisible()) {
         if (RecordingManager.isRecording()) {
            long timeMs = RecordingManager.getRecordingTime();
            long seconds = timeMs / 1000L % 60L;
            long minutes = timeMs / 60000L;
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            if (timeMs / 500L % 2L == 0L) {
               context.method_25294(10, 10, 16, 16, -65536);
            }

            context.method_25303(((class_329)this).method_1756(), "REC " + timeStr, 22, 10, 16777215);
         }

         if (RecordingManager.isSaving()) {
            int w = context.method_51421();
            String msg = "SAVING...";
            int tw = ((class_329)this).method_1756().method_1727(msg);
            context.method_25303(((class_329)this).method_1756(), msg, w - tw - 10, 10, 16763904);
         }

         if (!FFmpegManager.isReady()) {
            String status = FFmpegManager.getStatusMessage();
            int w = context.method_51421();
            int h = context.method_51443();
            int tw = ((class_329)this).method_1756().method_1727(status);
            context.method_25303(((class_329)this).method_1756(), status, w - tw - 5, h - 15, 11184810);
         }
      }
   }
}
