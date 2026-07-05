package com.mcorder.recorder;

import com.mcorder.McorderClient;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public class FrameCapturer {
   private final int srcWidth;
   private final int srcHeight;
   private final int dstWidth;
   private final int dstHeight;
   private final int bufferSize;
   private int scaleFbo = -1;
   private int scaleTexture = -1;
   // Mcorder perf: allocate the native readback buffer ONCE and reuse it every frame,
   // instead of memAlloc/memFree per captured frame (constant native churn on weak machines).
   private java.nio.ByteBuffer pixelBuf;

   public FrameCapturer(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
      this.srcWidth = srcWidth;
      this.srcHeight = srcHeight;
      this.dstWidth = dstWidth;
      this.dstHeight = dstHeight;
      this.bufferSize = dstWidth * dstHeight * 3;
      this.pixelBuf = MemoryUtil.memAlloc(this.bufferSize);
      if (srcWidth != dstWidth || srcHeight != dstHeight) {
         this.scaleTexture = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.scaleTexture);
         GL11.glTexImage2D(3553, 0, 6407, dstWidth, dstHeight, 0, 6407, 5121, (ByteBuffer)null);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glBindTexture(3553, 0);
         this.scaleFbo = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, this.scaleFbo);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, this.scaleTexture, 0);
         int status = GL30.glCheckFramebufferStatus(36160);
         GL30.glBindFramebuffer(36160, 0);
         if (status != 36053) {
            McorderClient.LOGGER.error("[Mcorder] Scale FBO incomplete (status=0x" + Integer.toHexString(status) + "). Disabling downscale.");
            GL30.glDeleteFramebuffers(this.scaleFbo);
            GL11.glDeleteTextures(this.scaleTexture);
            this.scaleFbo = -1;
            this.scaleTexture = -1;
         }
      }

      McorderClient.LOGGER
         .info("[Mcorder] FrameCapturer ready: " + srcWidth + "x" + srcHeight + " → " + dstWidth + "x" + dstHeight + " scaleFbo=" + (this.scaleFbo != -1));
   }

   public byte[] captureFrame() {
      int savedFbo = GL11.glGetInteger(36006);
      int readFbo;
      if (this.scaleFbo != -1) {
         GL30.glBindFramebuffer(36008, savedFbo);
         GL30.glBindFramebuffer(36009, this.scaleFbo);
         GL30.glBlitFramebuffer(0, 0, this.srcWidth, this.srcHeight, 0, 0, this.dstWidth, this.dstHeight, 16384, 9729);
         readFbo = this.scaleFbo;
      } else {
         readFbo = savedFbo;
      }

      GL30.glBindFramebuffer(36008, readFbo);
      GL11.glPixelStorei(3333, 1);
      ByteBuffer nativeBuf = this.pixelBuf;
      nativeBuf.clear();
      GL11.glReadPixels(0, 0, this.dstWidth, this.dstHeight, 6407, 5121, nativeBuf);
      byte[] data = new byte[this.bufferSize];
      nativeBuf.get(data);

      GL30.glBindFramebuffer(36160, savedFbo);
      return data;
   }

   public void cleanup() {
      if (this.scaleFbo != -1) {
         GL30.glDeleteFramebuffers(this.scaleFbo);
      }

      if (this.scaleTexture != -1) {
         GL11.glDeleteTextures(this.scaleTexture);
      }

      if (this.pixelBuf != null) {
         MemoryUtil.memFree(this.pixelBuf);
         this.pixelBuf = null;
      }
   }
}
