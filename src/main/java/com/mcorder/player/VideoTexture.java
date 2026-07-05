package com.mcorder.player;

import net.minecraft.class_1011;
import net.minecraft.class_1043;

public class VideoTexture extends class_1043 {
   private final int vidWidth;
   private final int vidHeight;
   private boolean closed = false;

   public VideoTexture(int width, int height) {
      super("mcorder_video", width, height, false);
      this.vidWidth = width;
      this.vidHeight = height;
   }

   public void updateFrame(byte[] rgbData) {
      if (!this.closed && rgbData != null) {
         class_1011 image = this.method_4525();
         if (image != null) {
            for (int y = 0; y < this.vidHeight; y++) {
               for (int x = 0; x < this.vidWidth; x++) {
                  int i = (y * this.vidWidth + x) * 3;
                  if (i + 2 < rgbData.length) {
                     int r = rgbData[i] & 255;
                     int g = rgbData[i + 1] & 255;
                     int b = rgbData[i + 2] & 255;
                     int a = 255;
                     int color = a << 24 | b << 16 | g << 8 | r;
                     image.method_4305(x, y, color);
                  }
               }
            }

            this.method_4524();
         }
      }
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;
         super.close();
      }
   }
}
