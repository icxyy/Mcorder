package com.mcorder.recorder;

import com.mcorder.FFmpegManager;
import com.mcorder.McorderClient;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.class_2561;
import net.minecraft.class_310;

public class RecordingManager {
   private static boolean isRecording = false;
   private static boolean isSaving = false;
   private static long recordingStartTime = 0L;
   private static FFmpegEncoder encoder;
   private static FrameCapturer capturer;
   private static File finalOutputFile;
   private static File tempAudioFile;
   private static int currentFps = 30;
   private static boolean showHud = true;
   private static boolean isAudioEnabled = true;
   private static long lastCaptureNs = 0L;

   public static void startRecording(long durationMillis, String quality) {
      if (!isRecording) {
         if (!FFmpegManager.isReady()) {
            class_310.method_1551().field_1705.method_1743().method_1812(class_2561.method_43470("§c[Mcorder] FFmpeg is still downloading! Please wait..."));
         } else {
            File recordingsDir = new File(class_310.method_1551().field_1697, "recordings");
            if (!recordingsDir.exists()) {
               recordingsDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            finalOutputFile = new File(recordingsDir, "mcorder_" + timeStamp + ".mp4");
            File audioFileForEncoder = null;
            if (isAudioEnabled) {
               tempAudioFile = new File(recordingsDir, "temp_audio_" + timeStamp + ".wav");

               try {
                  boolean ok = AudioMixCapture.INSTANCE.start(tempAudioFile, currentFps);
                  if (ok) {
                     audioFileForEncoder = tempAudioFile;
                     McorderClient.LOGGER.info("[Mcorder] AudioMixCapture ready.");
                  } else {
                     McorderClient.LOGGER.warn("[Mcorder] AudioMixCapture failed — video only.");
                     tempAudioFile = null;
                  }
               } catch (Exception var11) {
                  McorderClient.LOGGER.error("[Mcorder] Audio setup error: " + var11.getMessage());
                  tempAudioFile = null;
               }
            }

            int width = class_310.method_1551().method_22683().method_4489();
            int height = class_310.method_1551().method_22683().method_4506();
            int targetHeight = resolveTargetHeight(quality, height);
            int targetWidth = (int)Math.round((double)width / (double)height * (double)targetHeight);
            targetWidth = Math.max(16, targetWidth / 16 * 16);
            targetHeight = Math.max(16, targetHeight / 16 * 16);
            McorderClient.LOGGER
               .info("[Mcorder] Quality=" + quality + " → capture " + targetWidth + "x" + targetHeight + " (native " + width + "x" + height + ")");
            capturer = new FrameCapturer(width, height, targetWidth, targetHeight);
            encoder = new FFmpegEncoder(finalOutputFile, targetWidth, targetHeight, currentFps, audioFileForEncoder);
            if (encoder.start()) {
               isRecording = true;
               lastCaptureNs = 0L;
               recordingStartTime = System.currentTimeMillis();
               String msg = audioFileForEncoder != null ? "§a[Mcorder] Recording Started (video + audio)." : "§e[Mcorder] Recording Started (video only).";
               class_310.method_1551().field_1705.method_1743().method_1812(class_2561.method_43470(msg));
            } else {
               AudioMixCapture.INSTANCE.stop();
               if (capturer != null) {
                  capturer.cleanup();
                  capturer = null;
               }

               class_310.method_1551()
                  .field_1705
                  .method_1743()
                  .method_1812(class_2561.method_43470("§c[Mcorder] Failed to start recording! Check ffmpeg_debug.log"));
            }
         }
      }
   }

   public static void stopRecording() {
      if (isRecording) {
         isRecording = false;
         isSaving = true;
         FFmpegEncoder oldEncoder = encoder;
         FrameCapturer oldCapturer = capturer;
         encoder = null;
         capturer = null;
         if (oldCapturer != null) {
            class_310.method_1551().execute(oldCapturer::cleanup);
         }

         new Thread(
               () -> {
                  try {
                     AudioMixCapture.INSTANCE.stop();
                     if (oldEncoder != null) {
                        oldEncoder.stop();
                     }
                  } finally {
                     isSaving = false;
                     class_310.method_1551()
                        .execute(
                           () -> {
                              if (finalOutputFile.exists() && finalOutputFile.length() > 1000L) {
                                 class_310.method_1551()
                                    .field_1705
                                    .method_1743()
                                    .method_1812(class_2561.method_43470("§c[Mcorder] Recording Saved: " + finalOutputFile.getName()));
                                 class_310.method_1551()
                                    .field_1705
                                    .method_1743()
                                    .method_1812(class_2561.method_43470("§c[Mcorder] Folder: " + finalOutputFile.getParentFile().getAbsolutePath()));
                              } else {
                                 class_310.method_1551()
                                    .field_1705
                                    .method_1743()
                                    .method_1812(class_2561.method_43470("§c[Mcorder] Recording Failed! (Empty file). Check ffmpeg_debug.log"));
                              }

                              if (tempAudioFile != null && tempAudioFile.exists()) {
                                 tempAudioFile.delete();
                              }
                           }
                        );
                  }
               },
               "Mcorder-Finalizer"
            )
            .start();
      }
   }

   public static void onRenderFrame() {
      if (isRecording && capturer != null && encoder != null) {
         long now = System.nanoTime();
         long intervalNs = 1000000000L / (long)currentFps;
         if (now - lastCaptureNs >= intervalNs) {
            lastCaptureNs += intervalNs;
            if (now - lastCaptureNs > intervalNs) {
               lastCaptureNs = now;
            }

            byte[] frameData = capturer.captureFrame();
            if (frameData != null) {
               encoder.pushFrame(frameData);
               AudioMixCapture.INSTANCE.onVideoFrame();
            }
         }
      }
   }

   public static void tick() {
   }

   public static boolean isRecording() {
      return isRecording;
   }

   public static boolean isSaving() {
      return isSaving;
   }

   public static boolean isHudVisible() {
      return showHud;
   }

   public static void setHudVisible(boolean visible) {
      showHud = visible;
   }

   public static long getRecordingTime() {
      return isRecording ? System.currentTimeMillis() - recordingStartTime : 0L;
   }

   public static int getFps() {
      return currentFps;
   }

   public static void setFps(int fps) {
      currentFps = fps;
   }

   public static boolean isAudioEnabled() {
      return isAudioEnabled;
   }

   public static void setAudioEnabled(boolean enabled) {
      isAudioEnabled = enabled;
   }

   public static int resolveTargetHeight(String quality, int nativeHeight) {
      return Math.min(switch (quality) {
         case "144p" -> 144;
         case "240p" -> 240;
         case "360p" -> 360;
         case "480p" -> 480;
         case "720p" -> 720;
         case "1080p" -> 1080;
         default -> nativeHeight;
      }, nativeHeight);
   }
}
