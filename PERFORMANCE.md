# Performance & memory notes (potato-device focus)

This mod records the screen with FFmpeg. Real-time video encoding is inherently
CPU/RAM heavy — there is a hard floor to how light a screen recorder can be. That
said, the original had two concrete RAM/CPU problems that hurt weak machines, plus
several further wins.

## Applied in this source tree (safe, version-independent)

### 1. Bounded the frame buffer by BYTES, not frame count  — `FFmpegEncoder`
**Before:** `new LinkedBlockingQueue<>(300)`. At 1080p rgb24 each frame is
`1920*1080*3 ≈ 6.2 MB`, so 300 buffered frames = **~1.8 GB** of heap worst-case —
an instant OOM on a low-RAM machine.
**After:** capacity is computed to cap the in-flight buffer at **~96 MB** regardless
of resolution (clamped to 8–90 frames). When the encoder can't keep up, frames drop
(lower effective fps) instead of exhausting RAM — the correct trade-off on a potato.

### 2. Reused the native readback buffer  — `FrameCapturer`
**Before:** `MemoryUtil.memAlloc(bufferSize)` + `memFree` on **every captured frame**
→ constant native alloc/free churn.
**After:** one native `ByteBuffer` allocated in the constructor, reused each frame,
freed in `cleanup()`.

> ⚠️ These edits are applied to **decompiled** source and have **not** been compiled
> or run in this environment (no JDK/Minecraft here). Treat them as a reviewed
> starting point to fold into a real build.

## Recommended next (higher effort, needs build+profiling)

### 3. Asynchronous GPU readback with a PBO  — biggest FPS win on weak GPUs
`FrameCapturer.captureFrame()` calls **`glReadPixels` synchronously**, stalling the
render thread until the GPU finishes. Use a **double-buffered Pixel Buffer Object**:
issue the read into PBO *A* this frame, map/copy PBO *B* (last frame's result) — the
CPU never waits on the GPU. This is the single largest source of FPS loss during
recording on integrated/old GPUs.

### 4. Reuse the heap `byte[]` frames via a small pool
Each frame still does `new byte[bufferSize]` handed to the writer thread. A tiny
bounded pool (writer returns buffers after `ffmpegStdin.write`) removes per-frame GC
pressure. Needs care because producer (render thread) and consumer (writer thread)
share it — implement with the same bounded capacity as the queue.

### 5. Sensible potato defaults
- Default capture to **720p @ 30fps** (or lower), not native resolution. Native 1080p+
  rgb24 piping is the main CPU/RAM cost; the quality selector already supports
  144p–1080p (`RecordingManager.resolveTargetHeight`).
- FFmpeg is already invoked with `-preset ultrafast -tune zerolatency`, which is the
  right low-CPU choice. Consider exposing a hardware-encoder option
  (`h264_nvenc`/`h264_qsv`/`h264_vaapi`) when present — offloads encoding off the CPU.
- Consider capturing every Nth frame at low fps to cut both GPU readback and encode load.

### 6. Audio capture threads
There are multiple audio-capture strategies (`OpenALAudioCapture`,
`NativeAudioTapper`, `JavaAudioRecorder`, `FFmpegAudioCapture`). Ensure only one runs,
and that its thread is daemon + properly stopped (the writer/finalizer threads already
are) so nothing lingers after recording.

## Honest ceiling
Even fully optimized, a software screen-recorder that pipes raw frames to x264 will
use noticeable CPU and some RAM while recording. The changes above keep it from
*killing* RAM and reduce stalls, but "records smoothly on the weakest devices with no
overhead" is not physically achievable for this class of tool — the realistic lever
is low resolution/fps defaults + hardware encoding.
