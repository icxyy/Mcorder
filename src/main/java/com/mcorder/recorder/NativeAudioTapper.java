package com.mcorder.recorder;

import com.mcorder.McorderClient;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.openal.ALC11;

public class NativeAudioTapper {
   private long device;
   private Thread thread;
   private volatile boolean running = false;
   private final File outputFile;

   public NativeAudioTapper(File outputFile) {
      this.outputFile = outputFile;
   }

   public void start() {
      try {
         this.device = ALC11.alcCaptureOpenDevice((ByteBuffer)null, 44100, 4355, 44100);
         if (this.device == 0L) {
            McorderClient.LOGGER.error("[Mcorder] Failed to open OpenAL capture device.");
            return;
         }

         ALC11.alcCaptureStart(this.device);
         this.running = true;
         this.thread = new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(this.outputFile)) {
               for (ByteBuffer buffer = ByteBuffer.allocateDirect(176400).order(ByteOrder.nativeOrder()); this.running; Thread.sleep(5L)) {
                  int samplesAvailable = ALC11.alcGetInteger(this.device, 786);
                  if (samplesAvailable > 0) {
                     int samplesToRead = Math.min(samplesAvailable, buffer.capacity() / 4);
                     ALC11.alcCaptureSamples(this.device, buffer, samplesToRead);
                     byte[] bytes = new byte[samplesToRead * 4];
                     buffer.get(bytes);
                     fos.write(bytes);
                     buffer.clear();
                  }
               }
            } catch (Exception var8) {
               McorderClient.LOGGER.error("[Mcorder] Audio tapping error: " + var8.getMessage());
            }
         }, "Mcorder-AudioTapper");
         this.thread.setPriority(10);
         this.thread.start();
      } catch (Exception var2) {
         McorderClient.LOGGER.error("[Mcorder] Native audio fail: " + var2.getMessage());
      }
   }

   public void stop() {
      this.running = false;
      if (this.thread != null) {
         try {
            this.thread.join(1000L);
         } catch (InterruptedException var2) {
         }
      }

      if (this.device != 0L) {
         ALC11.alcCaptureStop(this.device);
         ALC11.alcCaptureCloseDevice(this.device);
      }
   }
}
