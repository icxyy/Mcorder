package com.mcorder.recorder;

import com.mcorder.FFmpegManager;
import com.mcorder.McorderClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FFmpegEncoder {
   private final File outputFile;
   private final int outW;
   private final int outH;
   private final int fps;
   private final File audioFile;
   private File tempVideoFile;
   private Process ffmpegProcess;
   private OutputStream ffmpegStdin;
   // Mcorder perf: cap the in-flight frame buffer by BYTES, not frame count, so weak
   // machines don't OOM. 300 raw 1080p frames = ~1.8 GB; here we bound to ~96 MB and
   // let the encoder drop frames (lower effective fps) instead of exhausting RAM.
   private final LinkedBlockingQueue<byte[]> frameQueue;
   private Thread writerThread;
   private volatile boolean running = false;
   private long framesPushed = 0L;

   public FFmpegEncoder(File outputFile, int outW, int outH, int fps, File audioFile) {
      this.outputFile = outputFile;
      this.outW = outW;
      this.outH = outH;
      this.fps = fps;
      this.audioFile = audioFile;
      long frameBytes = (long)outW * outH * 3L;
      int cap = (int)(96L * 1024L * 1024L / Math.max(1L, frameBytes));
      this.frameQueue = new LinkedBlockingQueue<>(Math.max(8, Math.min(90, cap)));
   }

   public boolean start() {
      String ffmpegPath = FFmpegManager.getFFmpegPath();
      if (ffmpegPath == null) {
         return false;
      } else {
         try {
            if (this.outputFile.getParentFile() != null && !this.outputFile.getParentFile().exists()) {
               this.outputFile.getParentFile().mkdirs();
            }

            File pass1Output;
            if (this.audioFile != null) {
               String name = this.outputFile.getName().replace(".mp4", "_video_only.mp4");
               this.tempVideoFile = new File(this.outputFile.getParentFile(), name);
               pass1Output = this.tempVideoFile;
            } else {
               this.tempVideoFile = null;
               pass1Output = this.outputFile;
            }

            List<String> args = this.buildPass1Args(ffmpegPath, pass1Output);
            McorderClient.LOGGER.info("[Mcorder] FFmpeg pass-1 cmd: " + String.join(" ", args));
            File logFile = new File(this.outputFile.getParentFile(), "ffmpeg_debug.log");
            if (logFile.exists()) {
               logFile.delete();
            }

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectOutput(Redirect.to(logFile));
            pb.redirectError(Redirect.appendTo(logFile));
            this.ffmpegProcess = pb.start();
            this.ffmpegStdin = this.ffmpegProcess.getOutputStream();
            Thread.sleep(300L);
            if (!this.ffmpegProcess.isAlive()) {
               McorderClient.LOGGER.error("[Mcorder] FFmpeg pass-1 died immediately. See ffmpeg_debug.log");
               return false;
            } else {
               this.running = true;
               this.startWriterThread();
               return true;
            }
         } catch (Exception var6) {
            McorderClient.LOGGER.error("[Mcorder] FFmpeg start error: " + var6.getMessage());
            return false;
         }
      }
   }

   private void startWriterThread() {
      this.writerThread = new Thread(() -> {
         McorderClient.LOGGER.info("[Mcorder] Writer thread started.");

         while (this.running || !this.frameQueue.isEmpty()) {
            byte[] frame;
            try {
               frame = this.frameQueue.poll(200L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException var5) {
               Thread.currentThread().interrupt();
               break;
            }

            if (frame != null) {
               try {
                  this.ffmpegStdin.write(frame);
                  this.framesPushed++;
                  if (this.framesPushed % 60L == 0L) {
                     McorderClient.LOGGER.info("[Mcorder] Frames written: " + this.framesPushed);
                  }
               } catch (IOException var4) {
                  McorderClient.LOGGER.error("[Mcorder] Pipe write failed after " + this.framesPushed + " frames: " + var4.getMessage());
                  this.running = false;
                  break;
               }
            }
         }

         try {
            this.ffmpegStdin.flush();
         } catch (IOException var3) {
         }

         McorderClient.LOGGER.info("[Mcorder] Writer thread done. Total frames: " + this.framesPushed);
      }, "Mcorder-Writer");
      this.writerThread.setDaemon(true);
      this.writerThread.start();
   }

   public void pushFrame(byte[] rgbData) {
      if (this.running) {
         if (!this.frameQueue.offer(rgbData)) {
            McorderClient.LOGGER.warn("[Mcorder] Frame queue full — dropping frame.");
         }
      }
   }

   public void stop() {
      McorderClient.LOGGER.info("[Mcorder] Stopping encoder. Frames so far: " + this.framesPushed);
      this.running = false;
      if (this.writerThread != null) {
         try {
            this.writerThread.join(10000L);
         } catch (InterruptedException var5) {
         }
      }

      try {
         if (this.ffmpegStdin != null) {
            this.ffmpegStdin.close();
         }
      } catch (IOException var4) {
      }

      if (this.ffmpegProcess != null) {
         try {
            if (!this.ffmpegProcess.waitFor(60L, TimeUnit.SECONDS)) {
               McorderClient.LOGGER.error("[Mcorder] FFmpeg pass-1 timeout — killing.");
               this.ffmpegProcess.destroyForcibly();
            }

            McorderClient.LOGGER.info("[Mcorder] FFmpeg pass-1 exit code: " + this.ffmpegProcess.exitValue());
         } catch (InterruptedException var3) {
            this.ffmpegProcess.destroyForcibly();
         }
      }

      if (this.tempVideoFile != null
         && this.tempVideoFile.exists()
         && this.tempVideoFile.length() > 1000L
         && this.audioFile != null
         && this.audioFile.exists()
         && this.audioFile.length() > 44L) {
         this.runMuxPass();
      } else if (this.tempVideoFile != null && this.tempVideoFile.exists() && !this.tempVideoFile.renameTo(this.outputFile)) {
         try {
            copyFile(this.tempVideoFile, this.outputFile);
         } catch (IOException var2) {
            McorderClient.LOGGER.error("[Mcorder] Failed to rename temp video: " + var2.getMessage());
         }

         this.tempVideoFile.delete();
      }
   }

   private void runMuxPass() {
      String ffmpegPath = FFmpegManager.getFFmpegPath();
      File logFile = new File(this.outputFile.getParentFile(), "ffmpeg_debug.log");
      List<String> args = new ArrayList<>();
      args.add(ffmpegPath);
      args.add("-y");
      args.add("-loglevel");
      args.add("info");
      args.add("-i");
      args.add(this.tempVideoFile.getAbsolutePath());
      args.add("-i");
      args.add(this.audioFile.getAbsolutePath());
      args.add("-c:v");
      args.add("copy");
      args.add("-c:a");
      args.add("aac");
      args.add("-ac");
      args.add("2");
      args.add("-ar");
      args.add("44100");
      args.add("-b:a");
      args.add("192k");
      args.add("-shortest");
      args.add("-movflags");
      args.add("+faststart");
      args.add(this.outputFile.getAbsolutePath());
      McorderClient.LOGGER.info("[Mcorder] FFmpeg pass-2 mux: " + String.join(" ", args));

      try {
         ProcessBuilder pb = new ProcessBuilder(args);
         pb.redirectOutput(Redirect.appendTo(logFile));
         pb.redirectError(Redirect.appendTo(logFile));
         Process p = pb.start();
         if (p.waitFor(120L, TimeUnit.SECONDS)) {
            McorderClient.LOGGER.info("[Mcorder] FFmpeg pass-2 exit code: " + p.exitValue());
            return;
         }

         p.destroyForcibly();
         McorderClient.LOGGER.error("[Mcorder] FFmpeg pass-2 timed out.");
      } catch (Exception var9) {
         McorderClient.LOGGER.error("[Mcorder] FFmpeg pass-2 error: " + var9.getMessage());
         return;
      } finally {
         this.tempVideoFile.delete();
      }
   }

   private List<String> buildPass1Args(String ffmpegPath, File pass1Output) {
      List<String> a = new ArrayList<>();
      a.add(ffmpegPath);
      a.add("-y");
      a.add("-loglevel");
      a.add("info");
      a.add("-f");
      a.add("rawvideo");
      a.add("-pix_fmt");
      a.add("rgb24");
      a.add("-s");
      a.add(this.outW + "x" + this.outH);
      a.add("-r");
      a.add(String.valueOf(this.fps));
      a.add("-i");
      a.add("pipe:0");
      a.add("-c:v");
      a.add("libx264");
      a.add("-preset");
      a.add("ultrafast");
      a.add("-tune");
      a.add("zerolatency");
      a.add("-pix_fmt");
      a.add("yuv420p");
      a.add("-vf");
      a.add("vflip");
      a.add("-fflags");
      a.add("+genpts");
      a.add("-movflags");
      a.add("+faststart");
      a.add(pass1Output.getAbsolutePath());
      return a;
   }

   private static void copyFile(File src, File dst) throws IOException {
      try (
         InputStream in = new FileInputStream(src);
         OutputStream out = new FileOutputStream(dst);
      ) {
         byte[] buf = new byte[65536];

         int n;
         while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
         }
      }
   }
}
