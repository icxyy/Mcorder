package com.mcorder.gui;

import com.mcorder.player.FFmpegDecoder;
import com.mcorder.player.VideoTexture;
import java.io.File;
import net.minecraft.class_10799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class VideoPlaybackScreen extends class_437 {
   private final class_437 parent;
   private final File videoFile;
   private FFmpegDecoder decoder;
   private VideoTexture texture;
   private class_2960 textureId;
   private Thread decodeThread;
   private boolean playing = false;

   public VideoPlaybackScreen(class_437 parent, File videoFile) {
      super(class_2561.method_43470("Playing: " + videoFile.getName()));
      this.parent = parent;
      this.videoFile = videoFile;
   }

   protected void method_25426() {
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Back"), button -> this.method_25419()).method_46434(10, 10, 100, 20).method_46431());
      this.startPlayback();
   }

   private void startPlayback() {
      this.decoder = new FFmpegDecoder(this.videoFile);
      if (this.decoder.start()) {
         this.texture = new VideoTexture(this.decoder.getWidth(), this.decoder.getHeight());
         this.textureId = class_2960.method_60655("mcorder", "video_playback");
         this.field_22787.method_1531().method_4616(this.textureId, this.texture);
         this.playing = true;
         this.decodeThread = new Thread(() -> {
            while (this.playing) {
               byte[] frame = this.decoder.readNextFrame();
               if (frame == null) {
                  this.playing = false;
                  break;
               } else {
                  this.texture.updateFrame(frame);

                  try {
                     Thread.sleep(16L);
                  } catch (InterruptedException var3) {
                     break;
                  }
               }
            }
         });
         this.decodeThread.setDaemon(true);
         this.decodeThread.start();
      }
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -1728053248);
      if (this.textureId != null && this.playing) {
         int sw = this.field_22789;
         int sh = this.field_22790;
         float vw = (float)this.decoder.getWidth();
         float vh = (float)this.decoder.getHeight();
         float scale = Math.min((float)sw / vw, (float)sh / vh) * 0.8F;
         int drawW = (int)(vw * scale);
         int drawH = (int)(vh * scale);
         int dx = (sw - drawW) / 2;
         int dy = (sh - drawH) / 2;
         context.method_52706(class_10799.field_56879, this.textureId, dx, dy, drawW, drawH);
      }

      if (!this.playing && this.decoder != null) {
         context.method_27534(this.field_22793, class_2561.method_43470("Playback finished"), this.field_22789 / 2, this.field_22790 / 2, 16777215);
      }

      super.method_25394(context, mouseX, mouseY, delta);
   }

   public void method_25419() {
      this.playing = false;
      if (this.decodeThread != null) {
         this.decodeThread.interrupt();
      }

      if (this.decoder != null) {
         this.decoder.stop();
      }

      if (this.textureId != null) {
         this.field_22787.method_1531().method_4615(this.textureId);
      }

      if (this.texture != null) {
         this.texture.close();
      }

      this.field_22787.method_1507(this.parent);
   }
}
