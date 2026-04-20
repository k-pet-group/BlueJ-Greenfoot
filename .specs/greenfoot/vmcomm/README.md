# VM Communication

Inter-VM communication protocol (4 files) using memory-mapped shared memory with a three-lock synchronization protocol between IDE (Main VM) and user scenario code (Debug VM).

---

## Key Entry Points

| Class | Purpose |
|-------|---------|
| `VMCommsMain` | IDE-side: writes commands, reads rendered frames and state |
| `VMCommsSimulation` | Debug VM-side: reads commands, writes rendered world frames |
| `Command` | Command constants and data holder with sequence numbering |

---

## Protocol

### Commands (Main -> Debug)

KEY_DOWN, KEY_UP, KEY_TYPED, MOUSE_*, COMMAND_RUN, COMMAND_PAUSE, COMMAND_ACT, INSTANTIATE_WORLD, SET_SPEED, etc.

### Synchronization

Three-lock protocol (server A, debug B, sync C) using file locks. Double-buffering with sequence numbers detects frame updates.

---

## Threading Model

6 threads across 2 JVM processes, coordinated by file locks and atomic variables.

### Atomic Variables

| Variable | Purpose |
|----------|---------|
| `lastSeq` (AtomicInteger) | Frame sequence tracking; bumped by +1000 on VM termination to invalidate stale data |
| `userVMReadyForInvocations` (AtomicBoolean) | VM readiness flag |
| `worldImageForSending` (AtomicReference) | Lock-free single-slot image handoff (producer overwrites unconsumed frames) |

### Image Double-Buffering Protocol

1. Simulation thread renders into `BufferedImage` from pool (capacity 3), places in `worldImageForSending.getAndSet(newImage)`
2. VMCommsSimulation Worker atomically takes image, writes pixels to shared memory
3. VMCommsMain Worker reads paintSeq; if changed, sets `haveUpdatedImage`
4. FXPlatform thread copies image to stage
5. Rate limiting: `paintRemote()` skips if less than ~8.33ms (1/120 FPS) since last paint

### Command Reliability

Commands use monotonically increasing sequence IDs. Debug VM writes back highest processed ID. VMCommsMain removes acknowledged commands; unacknowledged commands persist and are re-transmitted.

### Deadlock Freedom Invariant

- Each process holds at most 2 of 3 locks at any time
- No process ever acquires a lock it already holds
- The acquisition pattern forms no circular wait

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Memory-mapped file with FileLock | Zero-copy, low-latency IPC for high-frequency frame transfer. File locks work cross-process without JNI. |
| Three-lock protocol | Both processes read and write concurrently -- doubles throughput vs single lock |
| AtomicReference for image handoff | Allows frame dropping without blocking simulation thread |
| BufferedImage pool (capacity 3) | Avoids per-frame allocation. 1 rendering + 1 writing + 1 spare. |
| Command sequence acknowledgment | Reliable delivery -- commands persist until debug VM confirms receipt |
| `lastSeq` bump on VM termination | +1000 ensures stale data is always "older" than new VM's first frame |

---

## Dependencies

Uses: `core/` (simulation state), `gui/` (input events)
