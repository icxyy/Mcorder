package com.mcorder.recorder;

import com.mcorder.FFmpegManager;
import com.mcorder.McorderClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FFmpegAudioCapture {
   private Process captureProcess;
   private OutputStream processStdin;
   private final File outputFile;
   private volatile boolean started = false;
   private static final String[] LOOPBACK_KEYWORDS = new String[]{"stereo mix", "what u hear", "loopback", "wave out mix", "sum"};
   private static final String[] VIRTUAL_KEYWORDS = new String[]{
      "voicewave", "voicemeeter", "vb-audio", "virtual audio", "virtual cable", "blackhole", "soundflower", "cable output"
   };
   private static final String[] HEADSET_KEYWORDS = new String[]{"headphone", "headset", "speaker", "output"};
   private static final String[] MIC_KEYWORDS = new String[]{"microphone", "mic array", "array"};

   public FFmpegAudioCapture(File outputFile) {
      this.outputFile = outputFile;
   }

   public boolean start() {
      String ffmpegPath = FFmpegManager.getFFmpegPath();
      if (ffmpegPath == null) {
         return false;
      } else {
         File logFile = new File(this.outputFile.getParentFile(), "ffmpeg_debug.log");
         String device = this.findBestDshowDevice(ffmpegPath, logFile);
         if (device == null) {
            McorderClient.LOGGER.warn("[Mcorder] No suitable dshow audio device found — audio disabled.");
            return false;
         } else {
            McorderClient.LOGGER.info("[Mcorder] Audio capture device selected: " + device);
            List<String> args = new ArrayList<>();
            args.add(ffmpegPath);
            args.add("-y");
            args.add("-f");
            args.add("dshow");
            args.add("-i");
            args.add("audio=" + device);
            args.add("-c:a");
            args.add("pcm_s16le");
            args.add("-ar");
            args.add("44100");
            args.add("-ac");
            args.add("2");
            args.add(this.outputFile.getAbsolutePath());

            try {
               ProcessBuilder pb = new ProcessBuilder(args);
               pb.redirectOutput(Redirect.appendTo(logFile));
               pb.redirectError(Redirect.appendTo(logFile));
               this.captureProcess = pb.start();
               this.processStdin = this.captureProcess.getOutputStream();
               Thread.sleep(600L);
               if (!this.captureProcess.isAlive()) {
                  McorderClient.LOGGER.error("[Mcorder] Audio FFmpeg died immediately — check ffmpeg_debug.log");
                  return false;
               } else {
                  this.started = true;
                  return true;
               }
            } catch (Exception var6) {
               McorderClient.LOGGER.error("[Mcorder] Audio capture start error: " + var6.getMessage());
               return false;
            }
         }
      }
   }

   public void stop() {
      if (this.started && this.captureProcess != null) {
         try {
            this.processStdin.write(113);
            this.processStdin.write(10);
            this.processStdin.flush();
         } catch (IOException var3) {
         }

         try {
            if (!this.captureProcess.waitFor(10L, TimeUnit.SECONDS)) {
               this.captureProcess.destroyForcibly();
            }

            McorderClient.LOGGER.info("[Mcorder] Audio FFmpeg exit: " + this.captureProcess.exitValue());
         } catch (InterruptedException var2) {
            this.captureProcess.destroyForcibly();
         }
      }
   }

   private String findBestDshowDevice(String ffmpegPath, File logFile) {
      List<String> devices = this.listDshowAudioDevices(ffmpegPath, logFile);
      if (devices.isEmpty()) {
         return null;
      } else {
         String best = null;
         int bestScore = -1;

         for (String d : devices) {
            int score = this.scoreDevice(d);
            McorderClient.LOGGER.info("[Mcorder] Audio device candidate: \"" + d + "\" score=" + score);
            if (score > bestScore) {
               bestScore = score;
               best = d;
            }
         }

         return best;
      }
   }

   private int scoreDevice(String name) {
      String low = name.toLowerCase();

      for (String kw : LOOPBACK_KEYWORDS) {
         if (low.contains(kw)) {
            return 100;
         }
      }

      for (String kwx : VIRTUAL_KEYWORDS) {
         if (low.contains(kwx)) {
            return 80;
         }
      }

      for (String kwxx : HEADSET_KEYWORDS) {
         if (low.contains(kwxx)) {
            return 40;
         }
      }

      for (String kwxxx : MIC_KEYWORDS) {
         if (low.contains(kwxxx)) {
            return 10;
         }
      }

      return 5;
   }

   private List<String> listDshowAudioDevices(String ffmpegPath, File logFile) {
      List<String> result = new ArrayList<>();

      try {
         ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-f", "dshow", "-list_devices", "true", "-i", "dummy");
         pb.redirectErrorStream(true);
         Process p = pb.start();
         boolean inAudio = false;

         String line;
         try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while ((line = br.readLine()) != null) {
               if (line.contains("(audio)")) {
                  inAudio = true;
               }

               if (inAudio && line.contains("\"")) {
                  int s = line.indexOf(34);
                  int e = line.indexOf(34, s + 1);
                  if (s >= 0 && e > s) {
                     String dev = line.substring(s + 1, e);
                     if (!dev.isBlank() && !result.contains(dev)) {
                        result.add(dev);
                     }
                  }
               }
            }
         }

         p.waitFor(5L, TimeUnit.SECONDS);
      } catch (Exception var14) {
         McorderClient.LOGGER.error("[Mcorder] dshow device list error: " + var14.getMessage());
      }

      return result;
   }
}
