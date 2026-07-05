package com.mcorder.recorder;

import com.mcorder.McorderClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import org.lwjgl.openal.ALC11;

public class OpenALAudioCapture {
   private long captureDevice = 0L;
   private Thread captureThread;
   private volatile boolean running = false;
   private volatile boolean captureOk = false;
   private final File outputFile;
   private static final int SAMPLE_RATE = 44100;
   private static final int CHANNELS = 2;
   private static final int BITS_PER_SAMPLE = 16;
   private static final int BYTES_PER_SAMPLE = 2;
   private static final int FRAMES_PER_CHUNK = 44100;

   public OpenALAudioCapture(File outputFile) {
      this.outputFile = outputFile;
   }

   public boolean start() {
      try {
         this.captureDevice = ALC11.alcCaptureOpenDevice((ByteBuffer)null, 44100, 4355, 88200);
         if (this.captureDevice == 0L) {
            McorderClient.LOGGER.error("[Mcorder] Failed to open OpenAL capture device (alcCaptureOpenDevice returned 0).");
            return false;
         } else {
            ALC11.alcCaptureStart(this.captureDevice);
            this.running = true;
            this.captureOk = false;
            this.captureThread = new Thread(this::captureLoop, "Mcorder-OpenALCapture");
            this.captureThread.setDaemon(true);
            this.captureThread.setPriority(9);
            this.captureThread.start();
            McorderClient.LOGGER.info("[Mcorder] OpenAL capture started.");
            return true;
         }
      } catch (Exception var2) {
         McorderClient.LOGGER.error("[Mcorder] Failed to start OpenAL capture: " + var2);
         return false;
      }
   }

   private void captureLoop() {
      int shortsPerChunk = 88200;
      ShortBuffer buffer = ByteBuffer.allocateDirect(shortsPerChunk * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
      int totalFrames = 0;

      try (RandomAccessFile raf = new RandomAccessFile(this.outputFile, "rw")) {
         raf.seek(44L);
         byte[] byteArray = new byte[shortsPerChunk * 2];

         while (this.running) {
            int framesAvailable = ALC11.alcGetInteger(this.captureDevice, 786);
            if (framesAvailable <= 0) {
               Thread.sleep(5L);
            } else {
               int framesToRead = Math.min(framesAvailable, 44100);
               buffer.clear();
               ALC11.alcCaptureSamples(this.captureDevice, buffer, framesToRead);
               int numShorts = framesToRead * 2;
               int bytesToWrite = numShorts * 2;

               for (int i = 0; i < numShorts; i++) {
                  short s = buffer.get(i);
                  byteArray[i * 2] = (byte)(s & 255);
                  byteArray[i * 2 + 1] = (byte)(s >> 8 & 0xFF);
               }

               raf.write(byteArray, 0, bytesToWrite);
               totalFrames += framesToRead;
               this.captureOk = true;
            }
         }

         raf.getChannel().force(true);
         this.updateWavHeader(raf, totalFrames);
         McorderClient.LOGGER.info("[Mcorder] OpenAL capture finished. Frames: " + totalFrames);
      } catch (Exception var14) {
         McorderClient.LOGGER
            .error("[Mcorder] OpenAL capture error: " + var14.getClass().getSimpleName() + (var14.getMessage() != null ? " — " + var14.getMessage() : ""));
      }
   }

   public void stop() {
      this.running = false;
      if (this.captureThread != null) {
         try {
            this.captureThread.join(3000L);
         } catch (InterruptedException var2) {
         }
      }

      if (this.captureDevice != 0L) {
         ALC11.alcCaptureStop(this.captureDevice);
         ALC11.alcCaptureCloseDevice(this.captureDevice);
         this.captureDevice = 0L;
      }
   }

   public boolean isCaptureWorking() {
      return this.captureOk;
   }

   private void updateWavHeader(RandomAccessFile raf, int totalFrames) throws IOException {
      int dataBytes = totalFrames * 2 * 2;
      raf.seek(4L);
      writeLEInt(raf, dataBytes + 36);
      raf.seek(40L);
      writeLEInt(raf, dataBytes);
   }

   private static void writeLEInt(RandomAccessFile raf, int v) throws IOException {
      raf.writeByte(v & 0xFF);
      raf.writeByte(v >> 8 & 0xFF);
      raf.writeByte(v >> 16 & 0xFF);
      raf.writeByte(v >> 24 & 0xFF);
   }

   public static void createWavFile(File file) throws IOException {
      int byteRate = 176400;
      int blockAlign = 4;
      byte[] h = new byte[44];
      h[0] = 82;
      h[1] = 73;
      h[2] = 70;
      h[3] = 70;
      leInt(h, 4, 36);
      h[8] = 87;
      h[9] = 65;
      h[10] = 86;
      h[11] = 69;
      h[12] = 102;
      h[13] = 109;
      h[14] = 116;
      h[15] = 32;
      leInt(h, 16, 16);
      leShort(h, 20, (short)1);
      leShort(h, 22, (short)2);
      leInt(h, 24, 44100);
      leInt(h, 28, byteRate);
      leShort(h, 32, (short)blockAlign);
      leShort(h, 34, (short)16);
      h[36] = 100;
      h[37] = 97;
      h[38] = 116;
      h[39] = 97;
      leInt(h, 40, 0);

      try (FileOutputStream fos = new FileOutputStream(file)) {
         fos.write(h);
      }
   }

   private static void leInt(byte[] a, int o, int v) {
      a[o] = (byte)(v & 0xFF);
      a[o + 1] = (byte)(v >> 8 & 0xFF);
      a[o + 2] = (byte)(v >> 16 & 0xFF);
      a[o + 3] = (byte)(v >> 24 & 0xFF);
   }

   private static void leShort(byte[] a, int o, short v) {
      a[o] = (byte)(v & 255);
      a[o + 1] = (byte)(v >> 8 & 0xFF);
   }
}
