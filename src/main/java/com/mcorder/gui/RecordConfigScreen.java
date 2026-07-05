package com.mcorder.gui;

import com.mcorder.recorder.RecordingManager;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class RecordConfigScreen extends class_437 {
   private long selectedDuration = 0L;
   private String selectedQuality = "720p";

   public RecordConfigScreen() {
      super(class_2561.method_43470("Recording Settings"));
   }

   protected void method_25426() {
      int cx = this.field_22789 / 2;
      int cy = this.field_22790 / 2;
      int btnW = 180;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Duration: " + this.getDurationText()), button -> {
         RecordConfigScreen.DurationOption[] vals = RecordConfigScreen.DurationOption.values();
         int nextIdx = 0;

         for (int i = 0; i < vals.length; i++) {
            if (vals[i].getMillis() == this.selectedDuration) {
               nextIdx = (i + 1) % vals.length;
               break;
            }
         }

         this.selectedDuration = vals[nextIdx].getMillis();
         button.method_25355(class_2561.method_43470("Duration: " + vals[nextIdx].text));
      }).method_46434(cx - btnW / 2, cy - 60, btnW, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Quality: " + this.selectedQuality), button -> {
         RecordConfigScreen.QualityOption[] vals = RecordConfigScreen.QualityOption.values();
         int nextIdx = 0;

         for (int i = 0; i < vals.length; i++) {
            if (vals[i].getId().equals(this.selectedQuality)) {
               nextIdx = (i + 1) % vals.length;
               break;
            }
         }

         this.selectedQuality = vals[nextIdx].getId();
         button.method_25355(class_2561.method_43470("Quality: " + vals[nextIdx].getId()));
      }).method_46434(cx - btnW / 2, cy - 35, btnW, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Sound: " + (RecordingManager.isAudioEnabled() ? "AUTO" : "OFF")), button -> {
         RecordingManager.setAudioEnabled(!RecordingManager.isAudioEnabled());
         button.method_25355(class_2561.method_43470("Sound: " + (RecordingManager.isAudioEnabled() ? "AUTO" : "OFF")));
      }).method_46434(cx - btnW / 2, cy - 10, btnW, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("● START"), button -> {
         RecordingManager.startRecording(this.selectedDuration, this.selectedQuality);
         this.method_25419();
      }).method_46434(cx - btnW / 2, cy + 50, btnW, 20).method_46431());
   }

   private String getDurationText() {
      for (RecordConfigScreen.DurationOption d : RecordConfigScreen.DurationOption.values()) {
         if (d.millis == this.selectedDuration) {
            return d.text;
         }
      }

      return "Manual";
   }

   private String truncate(String s, int n) {
      return s.length() > n ? s.substring(0, n) + ".." : s;
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -1157627904);
      int cx = this.field_22789 / 2;
      int cy = this.field_22790 / 2;
      int pw = 100;
      int ph = 90;
      context.method_25294(cx - pw, cy - ph, cx + pw, cy + ph, -16119286);
      context.method_25294(cx - pw, cy - ph, cx + pw, cy - ph + 2, -6750208);
      context.method_25303(this.field_22793, "MCORDER CONFIG", cx - pw + 10, cy - ph + 10, 16777215);
      super.method_25394(context, mouseX, mouseY, delta);
   }

   public boolean method_25421() {
      return false;
   }

   private static enum DurationOption {
      MANUAL(0L, "Manual"),
      SEC_30(30000L, "30s"),
      MIN_1(60000L, "1m"),
      MIN_2(120000L, "2m");

      private final long millis;
      private final String text;

      private DurationOption(long m, String t) {
         this.millis = m;
         this.text = t;
      }

      public long getMillis() {
         return this.millis;
      }
   }

   private static enum QualityOption {
      Q144("144p"),
      Q240("240p"),
      Q360("360p"),
      Q480("480p"),
      Q720("720p"),
      Q1080("1080p"),
      NATIVE("Native");

      private final String id;

      private QualityOption(String i) {
         this.id = i;
      }

      public String getId() {
         return this.id;
      }
   }
}
