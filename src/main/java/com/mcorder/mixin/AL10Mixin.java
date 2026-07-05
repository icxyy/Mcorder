package com.mcorder.mixin;

import com.mcorder.recorder.AudioMixCapture;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {AL10.class},
   remap = false
)
public class AL10Mixin {
   @Inject(
      method = {"alBufferData(IILjava/nio/ByteBuffer;I)V"},
      at = {@At("HEAD")}
   )
   private static void onBufferDataByte(int buffer, int format, ByteBuffer data, int freq, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onBufferData(buffer, format, data, freq);
   }

   @Inject(
      method = {"alBufferData(IILjava/nio/ShortBuffer;I)V"},
      at = {@At("HEAD")}
   )
   private static void onBufferDataShort(int buffer, int format, ShortBuffer data, int freq, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onBufferDataShort(buffer, format, data, freq);
   }

   @Inject(
      method = {"alSourcei"},
      at = {@At("HEAD")}
   )
   private static void onSourcei(int source, int param, int value, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onSourcei(source, param, value);
   }

   @Inject(
      method = {"alSourcef"},
      at = {@At("HEAD")}
   )
   private static void onSourcef(int source, int param, float value, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onSourcef(source, param, value);
   }

   @Inject(
      method = {"alSourcePlay(I)V"},
      at = {@At("HEAD")}
   )
   private static void onSourcePlay(int source, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onSourcePlay(source);
   }

   @Inject(
      method = {"alSourceStop(I)V"},
      at = {@At("HEAD")}
   )
   private static void onSourceStop(int source, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onSourceStop(source);
   }

   @Inject(
      method = {"alSourceQueueBuffers(ILjava/nio/IntBuffer;)V"},
      at = {@At("HEAD")}
   )
   private static void onQueueBuffers(int source, IntBuffer buffers, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onQueueBuffers(source, buffers);
   }

   @Inject(
      method = {"alDeleteBuffers(Ljava/nio/IntBuffer;)V"},
      at = {@At("HEAD")}
   )
   private static void onDeleteBuffers(IntBuffer buffers, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onDeleteBuffers(buffers);
   }

   @Inject(
      method = {"alDeleteBuffers(I)V"},
      at = {@At("HEAD")}
   )
   private static void onDeleteBuffer(int buffer, CallbackInfo ci) {
      AudioMixCapture.INSTANCE.onDeleteBuffer(buffer);
   }
}
