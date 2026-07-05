package com.mcorder.recorder;

import com.mcorder.McorderClient;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioMixCapture {
   public static final AudioMixCapture INSTANCE = new AudioMixCapture();
   private static final int OUT_RATE = 44100;
   private static final int OUT_CHANNELS = 2;
   private static final int AL_FORMAT_MONO8 = 4352;
   private static final int AL_FORMAT_MONO16 = 4353;
   private static final int AL_FORMAT_STEREO8 = 4354;
   private static final int AL_FORMAT_STEREO16 = 4355;
   private static final int AL_BUFFER = 4105;
   private static final int AL_GAIN = 4106;
   private static final float MIN_GAIN = 0.08F;
   private int samplesPerFrame = 1470;
   private final List<AudioMixCapture.ActiveSound> activeSounds = new ArrayList<>();
   private final Object mixLock = new Object();
   private final LinkedBlockingQueue<short[]> commitQueue = new LinkedBlockingQueue<>();
   private final Map<Integer, short[]> bufferStore = new ConcurrentHashMap<>();
   private final Map<Integer, Integer> sourceToBuffer = new ConcurrentHashMap<>();
   private final Map<Integer, Float> sourceGain = new ConcurrentHashMap<>();
   private volatile boolean capturing = false;
   private Thread writerThread;

   private AudioMixCapture() {
   }

   public synchronized boolean start(File wavFile, int fps) {
      if (this.capturing) {
         return false;
      } else {
         this.samplesPerFrame = 44100 / Math.max(fps, 1);
         this.commitQueue.clear();
         synchronized (this.mixLock) {
            this.activeSounds.clear();
         }

         this.sourceGain.clear();
         this.sourceToBuffer.clear();
         this.capturing = true;
         this.writerThread = new Thread(() -> this.writeLoop(wavFile), "Mcorder-AudioMix");
         this.writerThread.setDaemon(true);
         this.writerThread.start();
         McorderClient.LOGGER.info("[Mcorder] AudioMixCapture started (fps=" + fps + " samplesPerFrame=" + this.samplesPerFrame + ").");
         return true;
      }
   }

   public synchronized void stop() {
      if (this.capturing) {
         synchronized (this.mixLock) {
            for (int i = 0; i < 30 && !this.activeSounds.isEmpty(); i++) {
               this.commitFrame();
            }

            this.activeSounds.clear();
         }

         this.capturing = false;
         if (this.writerThread != null) {
            try {
               this.writerThread.join(8000L);
            } catch (InterruptedException var4) {
            }
         }
      }
   }

   public boolean isCapturing() {
      return this.capturing;
   }

   public void onVideoFrame() {
      if (this.capturing) {
         synchronized (this.mixLock) {
            this.commitFrame();
         }
      }
   }

   public void onBufferData(int bufferId, int format, ByteBuffer data, int freq) {
      if (data != null) {
         short[] s = this.toStereo16(format, data.duplicate().order(ByteOrder.LITTLE_ENDIAN), freq);
         if (s != null && s.length > 0) {
            this.bufferStore.put(bufferId, s);
         }
      }
   }

   public void onBufferDataShort(int bufferId, int format, ShortBuffer data, int freq) {
      if (data != null) {
         ShortBuffer d = data.duplicate();
         ByteBuffer bb = ByteBuffer.allocate(d.remaining() * 2).order(ByteOrder.LITTLE_ENDIAN);

         while (d.hasRemaining()) {
            bb.putShort(d.get());
         }

         bb.flip();
         this.onBufferData(bufferId, format, bb, freq);
      }
   }

   public void onSourcei(int source, int param, int value) {
      if (param == 4105) {
         this.sourceToBuffer.put(source, value);
      }
   }

   public void onSourcef(int source, int param, float value) {
      if (param == 4106) {
         this.sourceGain.put(source, value);
      }
   }

   public void onDeleteBuffers(IntBuffer buffers) {
      if (buffers != null) {
         IntBuffer dup = buffers.duplicate();

         while (dup.hasRemaining()) {
            this.bufferStore.remove(dup.get());
         }
      }
   }

   public void onDeleteBuffer(int buffer) {
      this.bufferStore.remove(buffer);
   }

   public void onSourceStop(int source) {
   }

   public void onSourcePlay(int source) {
      if (this.capturing) {
         Integer bufId = this.sourceToBuffer.get(source);
         if (bufId != null && bufId != 0) {
            short[] pcm = this.bufferStore.get(bufId);
            if (pcm != null) {
               float gain = this.sourceGain.getOrDefault(source, 1.0F);
               if (!(gain < 0.08F)) {
                  synchronized (this.mixLock) {
                     this.activeSounds.add(new AudioMixCapture.ActiveSound(pcm, gain));
                  }
               }
            }
         }
      }
   }

   public void onQueueBuffers(int source, IntBuffer bufferNames) {
      if (this.capturing && bufferNames != null) {
         float gain = this.sourceGain.getOrDefault(source, 1.0F);
         if (!(gain < 0.08F)) {
            IntBuffer dup = bufferNames.duplicate();

            while (dup.hasRemaining()) {
               short[] pcm = this.bufferStore.get(dup.get());
               if (pcm != null) {
                  synchronized (this.mixLock) {
                     this.activeSounds.add(new AudioMixCapture.ActiveSound(pcm, gain));
                  }
               }
            }
         }
      }
   }

   private void commitFrame() {
      int chunkLen = this.samplesPerFrame * 2;
      int[] mix = new int[chunkLen];
      Iterator<AudioMixCapture.ActiveSound> it = this.activeSounds.iterator();

      while (it.hasNext()) {
         AudioMixCapture.ActiveSound snd = it.next();
         int remaining = snd.pcm.length - snd.position;
         int toMix = Math.min(remaining, chunkLen);

         for (int i = 0; i < toMix; i++) {
            mix[i] += (int)((float)snd.pcm[snd.position + i] * snd.gain);
         }

         snd.position += toMix;
         if (snd.isDone()) {
            it.remove();
         }
      }

      short[] out = new short[chunkLen];

      for (int i = 0; i < chunkLen; i++) {
         out[i] = (short)Math.max(-32768, Math.min(32767, mix[i]));
      }

      this.commitQueue.offer(out);
   }

   private void writeLoop(File wavFile) {
      long totalSamples = 0L;

      try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
         this.writeWavHeader(raf);

         while (this.capturing || !this.commitQueue.isEmpty()) {
            short[] chunk = this.commitQueue.poll(100L, TimeUnit.MILLISECONDS);
            if (chunk != null) {
               byte[] bytes = new byte[chunk.length * 2];
               ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

               for (short s : chunk) {
                  bb.putShort(s);
               }

               raf.write(bytes);
               totalSamples += (long)(chunk.length / 2);
            }
         }

         long dataBytes = totalSamples * 2L * 2L;
         raf.seek(4L);
         writeLEInt(raf, (int)(dataBytes + 36L));
         raf.seek(40L);
         writeLEInt(raf, (int)dataBytes);
         McorderClient.LOGGER.info("[Mcorder] AudioMixCapture done. Samples=" + totalSamples);
      } catch (Exception var14) {
         McorderClient.LOGGER.error("[Mcorder] AudioMix error: " + var14);
      }
   }

   private short[] toStereo16(int fmt, ByteBuffer data, int freq) {
      boolean mono = fmt == 4352 || fmt == 4353;
      boolean bit8 = fmt == 4352 || fmt == 4354;
      int srcCh = mono ? 1 : 2;
      int bps = bit8 ? 1 : 2;
      int frames = data.remaining() / (bps * srcCh);
      if (frames == 0) {
         return null;
      } else {
         double ratio = 44100.0 / (double)Math.max(freq, 1);
         int outFrames = (int)((double)frames * ratio);
         if (outFrames == 0) {
            return null;
         } else {
            short[] out = new short[outFrames * 2];

            for (int i = 0; i < outFrames; i++) {
               int s = Math.min((int)((double)i / ratio), frames - 1);
               short L;
               short R;
               if (bit8) {
                  L = (short)((data.get(s * srcCh) & 255) - 128 << 8);
                  R = mono ? L : (short)((data.get(s * srcCh + 1) & 255) - 128 << 8);
               } else {
                  L = data.getShort(s * srcCh * 2);
                  R = mono ? L : data.getShort(s * srcCh * 2 + 2);
               }

               out[i * 2] = L;
               out[i * 2 + 1] = R;
            }

            return out;
         }
      }
   }

   private void writeWavHeader(RandomAccessFile raf) throws IOException {
      int br = 176400;
      raf.write(
         new byte[]{
            82,
            73,
            70,
            70,
            0,
            0,
            0,
            0,
            87,
            65,
            86,
            69,
            102,
            109,
            116,
            32,
            16,
            0,
            0,
            0,
            1,
            0,
            2,
            0,
            68,
            -84,
            0,
            0,
            (byte)(br & 0xFF),
            (byte)(br >> 8 & 0xFF),
            (byte)(br >> 16 & 0xFF),
            (byte)(br >> 24 & 0xFF),
            4,
            0,
            16,
            0,
            100,
            97,
            116,
            97,
            0,
            0,
            0,
            0
         }
      );
   }

   private static void writeLEInt(RandomAccessFile raf, int v) throws IOException {
      raf.write(v & 0xFF);
      raf.write(v >> 8 & 0xFF);
      raf.write(v >> 16 & 0xFF);
      raf.write(v >> 24 & 0xFF);
   }

   private static class ActiveSound {
      final short[] pcm;
      final float gain;
      int position;

      ActiveSound(short[] pcm, float gain) {
         this.pcm = pcm;
         this.gain = gain;
         this.position = 0;
      }

      boolean isDone() {
         return this.position >= this.pcm.length;
      }
   }
}
