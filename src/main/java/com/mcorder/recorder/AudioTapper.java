package com.mcorder.recorder;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.openal.ALC11;

public class AudioTapper {
   public static List<String> getCaptureDevices() {
      List<String> devices = new ArrayList<>();
      String deviceList = ALC11.alcGetString(0L, 784);
      if (deviceList != null) {
         String[] split = deviceList.split("\u0000");

         for (String s : split) {
            if (!s.trim().isEmpty()) {
               devices.add(s);
            }
         }
      }

      return devices;
   }
}
