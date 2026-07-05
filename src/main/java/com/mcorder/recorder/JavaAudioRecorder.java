package com.mcorder.recorder;

import com.mcorder.McorderClient;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.DataLine.Info;

public class JavaAudioRecorder {
   private TargetDataLine line;
   private Thread recordingThread;
   private volatile boolean running = false;

   public void start(File outputFile) {
      try {
         AudioFormat format = new AudioFormat(44100.0F, 16, 2, true, false);
         Info info = new Info(TargetDataLine.class, format);
         if (!AudioSystem.isLineSupported(info)) {
            McorderClient.LOGGER.error("[Mcorder] Line not supported for audio capture.");
            return;
         }

         this.line = (TargetDataLine)AudioSystem.getLine(info);
         this.line.open(format);
         this.line.start();
         this.running = true;
         this.recordingThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(this.line)) {
               AudioSystem.write(ais, Type.WAVE, outputFile);
            } catch (IOException var7) {
               McorderClient.LOGGER.error("[Mcorder] Audio write error: " + var7.getMessage());
            }
         }, "Mcorder-JavaAudio");
         this.recordingThread.start();
         McorderClient.LOGGER.info("[Mcorder] Java Audio Recorder started.");
      } catch (LineUnavailableException var4) {
         McorderClient.LOGGER.error("[Mcorder] Audio line unavailable: " + var4.getMessage());
      }
   }

   public void stop() {
      this.running = false;
      if (this.line != null) {
         this.line.stop();
         this.line.close();
      }

      if (this.recordingThread != null) {
         try {
            this.recordingThread.join(1000L);
         } catch (InterruptedException var2) {
         }
      }
   }

   public static void logMixers() {
      javax.sound.sampled.Mixer.Info[] mixers = AudioSystem.getMixerInfo();

      for (javax.sound.sampled.Mixer.Info mixerInfo : mixers) {
         McorderClient.LOGGER.info("[Mcorder-Mixer] " + mixerInfo.getName() + " - " + mixerInfo.getDescription());
      }
   }
}
