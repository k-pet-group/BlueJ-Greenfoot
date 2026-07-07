---
id: greenfoot-sound
type: submodule-design
title: "Greenfoot Audio System"
status: active
parent: greenfoot
---

# Greenfoot Audio System

> Auto-generated from code analysis. Review and refine.

Complete audio playback and management system (22 files). Supports WAV, AIFF, MIDI, and MP3 formats with play, loop, pause, and stop controls. Uses a factory pattern and caching for performance.

---

## Key Interfaces and Classes

| Class | Purpose |
|-------|---------|
| `Sound` (interface) | Defines sound operations: play, stop, pause, loop |
| `SoundFactory` | Creates appropriate Sound implementations from files |
| `SoundClip` | WAV/AIFF audio via Java Sound API clips |
| `MidiFileSound` | MIDI file playback |
| `SoundStream` | Streaming audio (MP3 via JLayer) |
| `ClipCache` | Caches frequently played sound clips |
| `MicLevelGrabber` | Microphone input level detection |

---

## Format Support

| Format | Implementation |
|--------|---------------|
| WAV / AIFF | `SoundClip` via javax.sound.sampled |
| MIDI | `MidiFileSound` via javax.sound.midi |
| MP3 | `SoundStream` via JLayer library |

---

## Threading Model

The sound system is the most thread-dense subsystem per file in the codebase, with **6 distinct thread types** and heavy synchronization.

### Thread Inventory

| Thread | Name | Type | Lifecycle | Purpose |
|--------|------|------|-----------|---------|
| `ClipProcessThread` | `"Clip process"` | Singleton daemon | Static; auto-restarts if killed | Executes SoundClip state transitions (opening, stopping, handling JDK bugs) |
| `ClipCloserThread` | `"Clip closer"` | Singleton daemon, lazy | Static; started on first `addClip()` | Closes `javax.sound.sampled.Clip` instances (can block on PulseAudio) |
| `SoundStream` playback | `"SoundStream:<source>"` | Per-stream, transient | Created per `startPlayback()`; idles for 1s after playback then dies | Reads audio data and writes to `SourceDataLine` |
| Recording thread | `"Start sound recording"` | Per-recording, transient | Created per `startRecording()`; dies when recording stops | Reads from `TargetDataLine` into buffers |
| `MicLevelGrabber` updater | `"Update mic level"` | Fire-and-forget, transient | Created per `getLevel()` call (guarded by `volatile running`) | Non-blocking mic level sampling for VU meter |
| JDK LineListener callback | (JDK-managed) | JDK internal | Fires on `LineEvent.Type.STOP` | Invokes `SoundClip.update()` when a clip finishes |

### Synchronization

**SoundClip** uses `synchronized(this)` on all public methods (`play`, `loop`, `stop`, `close`, `pause`, `setVolume`, `getVolume`, `isPlaying`, `isPaused`, `isStopped`).

**Critical deadlock avoidance in `processState()`**: OpenJDK's `Clip.stop()` synchronously dispatches the `LineListener` callback on a different thread but waits for it to return. Holding `SoundClip`'s lock while calling `stop()` would deadlock. The solution:
```
synchronized(this) { ... prepare for stop ... }
soundClip.stop();  // OUTSIDE synchronized — avoids deadlock
synchronized(this) { ... handle state changes that occurred while unlocked ... }
```

**`ClipProcessThread`** and **`ClipCloserThread`** use `synchronized(queue)` + `wait()`/`notify()` — standard producer-consumer.

**`SoundStream`** uses `synchronized(this)` + `wait()`/`notifyAll()` for pause/resume, plus `volatile AudioLine line` for lock-free access to the audio line reference from control methods.

**`SoundRecorder`** uses `AtomicBoolean keepRecording` (stop signal), `AtomicReference<List<byte[]>>` (lock-free partial result publication), and `ArrayBlockingQueue<byte[]>` capacity-1 (synchronous final result handoff).

### Producer-Consumer Patterns

| Pattern | Producer | Consumer | Queue Type |
|---------|----------|----------|------------|
| Clip processing | Any thread (via `addToQueue`) | `ClipProcessThread` | `LinkedList<SoundClip>` + `wait()`/`notify()` |
| Clip closing | Any thread (via `addClip`) | `ClipCloserThread` | `LinkedList<Clip>` + `wait()`/`notify()` |
| Recording result | Recording thread | Main thread (`stopRecording()`) | `ArrayBlockingQueue<byte[]>` capacity 1 |
| Image for sending | Simulation thread | VMCommsSimulation Worker | `AtomicReference` (see vmcomm) |

---

## Dependencies

**External:** JLayer 1.0.1 (MP3), Java Sound API

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Separate ClipProcessThread** | JDK audio operations (open, stop) can block for hundreds of milliseconds, especially on Linux/PulseAudio. Running them on a dedicated thread prevents blocking the simulation or UI. |
| **Separate ClipCloserThread** | `Clip.close()` can block for extended periods on some platforms. Isolating this prevents the process thread from stalling on slow close operations. |
| **Auto-restart on thread death** | In environments like Greenfoot's online gallery, threads may be killed but static fields survive. Auto-restart on next use ensures resilience. |
| **Lock-release-call-reacquire in processState()** | The only safe way to call `soundClip.stop()` without deadlocking with the JDK's synchronous LineListener callback dispatch. |
| **`volatile` AudioLine in SoundStream** | `getLongFramePosition()`, `setVolume()`, `getVolume()` need to see the current line without synchronization overhead on every call. The volatile ensures visibility of the reference. |
| **AtomicReference for partial recording results** | The recording thread produces data continuously; the UI reads it periodically. Lock-free publication avoids blocking either side. |
| **1-second idle timeout for SoundStream threads** | After playback ends, the thread waits briefly for a restart before dying. This avoids the overhead of creating a new thread for rapid play-stop-play sequences. |
