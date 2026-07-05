package com.mcorder.gui;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class VideoListScreen extends class_437 {
   public VideoListScreen() {
      super(class_2561.method_43470("My Recordings"));
   }

   protected void method_25426() {
      int cx = this.field_22789 / 2;
      int y = 50;
      File recordingsDir = new File(class_310.method_1551().field_1697, "recordings");
      if (recordingsDir.exists() && recordingsDir.isDirectory()) {
         File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));
         if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

            for (File file : files) {
               if (y > this.field_22790 - 60) {
                  break;
               }

               this.method_37063(
                  class_4185.method_46430(
                        class_2561.method_43470("▶ " + file.getName()), button -> this.field_22787.method_1507(new VideoPlaybackScreen(this, file))
                     )
                     .method_46434(cx - 150, y, 300, 20)
                     .method_46431()
               );
               y += 24;
            }
         }
      }

      this.method_37063(
         class_4185.method_46430(class_2561.method_43470("Back"), button -> this.method_25419())
            .method_46434(cx - 100, this.field_22790 - 35, 200, 20)
            .method_46431()
      );
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -1442840576);
      int cx = this.field_22789 / 2;
      int pw = 170;
      context.method_25294(cx - pw, 10, cx + pw, this.field_22790 - 10, -871296751);
      context.method_25294(cx - pw, 10, cx + pw, 12, -3407872);
      int borderColor = 1157627903;
      context.method_25294(cx - pw, 10, cx + pw, 11, borderColor);
      context.method_25294(cx - pw, this.field_22790 - 11, cx + pw, this.field_22790 - 10, borderColor);
      context.method_25294(cx - pw, 10, cx - pw + 1, this.field_22790 - 10, borderColor);
      context.method_25294(cx + pw - 1, 10, cx + pw, this.field_22790 - 10, borderColor);
      context.method_25303(this.field_22793, "MY RECORDINGS", cx - pw + 10, 18, 11184810);
      context.method_25294(cx - pw + 10, 28, cx + pw - 10, 29, 587202559);
      super.method_25394(context, mouseX, mouseY, delta);
   }
}
