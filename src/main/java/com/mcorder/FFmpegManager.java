package com.mcorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FFmpegManager {
   private static final String FFMPEG_DOWNLOAD_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
   private static Path ffmpegDir;
   private static Path ffmpegExe;
   private static boolean ready = false;
   private static boolean downloading = false;
   private static String statusMessage = "Initializing...";

   public static void init(File gameDir) {
      ffmpegDir = gameDir.toPath().resolve("mcorder").resolve("ffmpeg");
      ffmpegExe = ffmpegDir.resolve("ffmpeg.exe");
      if (Files.exists(ffmpegExe)) {
         ready = true;
         statusMessage = "FFmpeg ready.";
      } else {
         downloadInBackground();
      }
   }

   private static void downloadInBackground() {
      if (!downloading) {
         downloading = true;
         statusMessage = "Downloading FFmpeg...";
         Thread downloadThread = new Thread(() -> {
            Path zipFile = null;

            try {
               Files.createDirectories(ffmpegDir);
               zipFile = ffmpegDir.resolve("ffmpeg.zip");
               URL url = new URL("https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip");
               HttpURLConnection conn = (HttpURLConnection)url.openConnection();
               conn.setRequestProperty("User-Agent", "Mcorder/1.0");
               conn.connect();
               long totalSize = conn.getContentLengthLong();

               try (
                  InputStream in = conn.getInputStream();
                  FileOutputStream fos = new FileOutputStream(zipFile.toFile());
               ) {
                  byte[] buffer = new byte[8192];
                  long downloaded = 0L;

                  int bytesRead;
                  while ((bytesRead = in.read(buffer)) != -1) {
                     fos.write(buffer, 0, bytesRead);
                     downloaded += (long)bytesRead;
                     if (totalSize > 0L) {
                        statusMessage = "Downloading FFmpeg: " + (int)(downloaded * 100L / totalSize) + "%";
                     }
                  }
               }

               Path tempExe = ffmpegDir.resolve("ffmpeg.exe.tmp");
               boolean found = false;
               ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()));

               ZipEntry entry;
               try {
                  while ((entry = zis.getNextEntry()) != null) {
                     if (entry.getName().endsWith("ffmpeg.exe") && !entry.isDirectory()) {
                        try (FileOutputStream fos = new FileOutputStream(tempExe.toFile())) {
                           byte[] buffer = new byte[8192];

                           int len;
                           while ((len = zis.read(buffer)) > 0) {
                              fos.write(buffer, 0, len);
                           }
                        }

                        found = true;
                        break;
                     }
                  }
               } catch (Throwable var33) {
                  try {
                     zis.close();
                  } catch (Throwable var28) {
                     var33.addSuppressed(var28);
                  }

                  throw var33;
               }

               zis.close();
               if (found) {
                  Files.move(tempExe, ffmpegExe, StandardCopyOption.REPLACE_EXISTING);
                  ready = true;
                  statusMessage = "FFmpeg ready!";
               }
            } catch (Exception var36) {
               statusMessage = "FFmpeg failed! " + var36.getMessage();
            } finally {
               downloading = false;
               if (zipFile != null) {
                  try {
                     Files.deleteIfExists(zipFile);
                  } catch (IOException var27) {
                  }
               }
            }
         }, "Mcorder-FFmpeg-Download");
         downloadThread.setDaemon(true);
         downloadThread.start();
      }
   }

   public static String getFFmpegPath() {
      return ffmpegExe != null && Files.exists(ffmpegExe) ? ffmpegExe.toAbsolutePath().toString() : "ffmpeg";
   }

   public static List<String> getAudioDevices() {
      List<String> devices = new ArrayList<>();
      String path = getFFmpegPath();

      try {
         Process p = new ProcessBuilder(path, "-f", "dshow", "-list_devices", "true", "-i", "dummy").redirectErrorStream(true).start();

         String line;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while ((line = reader.readLine()) != null) {
               if (line.contains("\"")) {
                  int f = line.indexOf("\"");
                  int s = line.indexOf("\"", f + 1);
                  if (f != -1 && s != -1) {
                     String name = line.substring(f + 1, s);
                     if (!devices.contains(name)) {
                        devices.add(name);
                     }
                  }
               }
            }
         }

         p.waitFor();
      } catch (Exception var10) {
      }

      return devices;
   }

   public static String getGameAudioDevice(List<String> devices) {
      if (devices != null && !devices.isEmpty()) {
         String[] gameAudioNames = new String[]{
            "stereo mix",
            "what u hear",
            "loopback",
            "virtual-audio-capturer",
            "virtual audio capturer",
            "cable input",
            "voice meeter",
            "voicemeeter",
            "audio repeater"
         };

         for (String name : gameAudioNames) {
            for (String device : devices) {
               if (device.toLowerCase().equals(name) || device.toLowerCase().contains(name)) {
                  return device;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean isReady() {
      return ready;
   }

   public static boolean isDownloading() {
      return downloading;
   }

   public static String getStatusMessage() {
      return statusMessage;
   }
}
