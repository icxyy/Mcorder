package com.mcorder.player;

import com.mcorder.FFmpegManager;
import com.mcorder.McorderClient;
import java.io.File;
import java.io.InputStream;

public class FFmpegDecoder {
   private Process ffmpegProcess;
   private InputStream ffmpegOutput;
   private final File inputFile;
   private int width = 1920;
   private int height = 1080;
   private int frameSize;

   public FFmpegDecoder(File inputFile) {
      this.inputFile = inputFile;
      this.frameSize = this.width * this.height * 3;
   }

   public boolean start() {
      try {
         ProcessBuilder pb = new ProcessBuilder(
            FFmpegManager.getFFmpegPath(), "-i", this.inputFile.getAbsolutePath(), "-f", "image2pipe", "-vcodec", "rawvideo", "-pix_fmt", "rgb24", "-"
         );
         pb.redirectErrorStream(false);
         this.ffmpegProcess = pb.start();
         this.ffmpegOutput = this.ffmpegProcess.getInputStream();
         new Thread(() -> {
            try {
               InputStream is = this.ffmpegProcess.getErrorStream();
               byte[] buffer = new byte[1024];

               while (is.read(buffer) != -1) {
               }
            } catch (Exception var3) {
            }
         }).start();
         return true;
      } catch (Exception var2) {
         McorderClient.LOGGER.error("Failed to start FFmpeg decoder", var2);
         return false;
      }
   }

   public byte[] readNextFrame() {
      if (this.ffmpegOutput == null) {
         return null;
      } else {
         byte[] frameData = new byte[this.frameSize];
         int bytesRead = 0;

         try {
            while (bytesRead < this.frameSize) {
               int read = this.ffmpegOutput.read(frameData, bytesRead, this.frameSize - bytesRead);
               if (read == -1) {
                  return null;
               }

               bytesRead += read;
            }

            return frameData;
         } catch (Exception var4) {
            return null;
         }
      }
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void stop() {
      try {
         if (this.ffmpegOutput != null) {
            this.ffmpegOutput.close();
         }

         if (this.ffmpegProcess != null) {
            this.ffmpegProcess.destroy();
         }
      } catch (Exception var2) {
         McorderClient.LOGGER.error("Error stopping FFmpeg decoder", var2);
      }
   }
}
