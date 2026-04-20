# Stride Language API

A minimal public library providing utility classes for the Stride language. Bundled with compiled Stride programs and available to student code both inside the IDE and in standalone exports.

**2 Java files.** Zero external dependencies (stdlib only).

---

## Public Interface

- **`lang.stride.Terminal`** — `write(String)`, `read()`, `readInt()` — IDE-integrated terminal I/O (falls back to stdin/stdout outside IDE)
- **`lang.stride.Utility`** — `makeRange(int start, int end)` — Lazy inclusive integer range as `List<Integer>` via `AbstractList`
