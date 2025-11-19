package bluej.parser.psi;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * OverlayReader (streaming, non-buffering).
 *
 * Build with the Builder. The Builder collects overlay rules (insert/replace/delete/until-exhausted).
 * The OverlayReader precomputes the rules (sorted by start) and then reads streaming:
 * - it keeps a stack of SegmentReader (top is active)
 * - before each read attempt it checks if the next rule should activate at the current outPos
 * - when a segment reader EOFs, it is popped and a number of chars to skip is returned and applied
 *   to the new top reader (skipping means reading & discarding).
 */
public class OverlayReader extends Reader {

    // ----- Public API: Builder and rule factories -----

    public static class Builder {
        private final Reader upstream;
        private final List<SegmentRule> rules = new ArrayList<>();

        public Builder(Reader upstream) {
            this.upstream = Objects.requireNonNull(upstream);
        }

        /** Replace range [start, end] (inclusive). Use end == -1 for "to end". */
        public Builder replaceRange(long start, long endInclusive, Reader replacement) {
            rules.add(new ReplaceRangeRule(start, endInclusive, replacement));
            return this;
        }

        /** Insert replacement at position start (does not consume upstream). */
        public Builder insertAt(long start, Reader replacement) {
            rules.add(new InsertAtRule(start, replacement));
            return this;
        }

        /** Delete range [start, end] (inclusive) — produces no chars but consumes upstream. */
        public Builder deleteRange(long start, long endInclusive) {
            rules.add(new DeleteRangeRule(start, endInclusive));
            return this;
        }

        /**
         * Replace starting at start with replacement until replacement EOFs; after that, skip the
         * same number of characters from upstream as were produced by replacement.
         */
        public Builder replaceUntilExhausted(long start, Reader replacement) {
            rules.add(new ReplaceUntilExhaustedRule(start, replacement));
            return this;
        }

        public OverlayReader build() {
            // sort rules by activation start (ascending), stable
            rules.sort(Comparator.comparingLong(SegmentRule::activationPos));
            return new OverlayReader(upstream, rules);
        }
    }

    // ----- SegmentRule and SegmentReader interfaces -----

    private interface SegmentRule {
        /** The out-position (index) at which this rule becomes eligible for activation. */
        long activationPos();

        /** Test whether rule should activate at the given outPos (usually `outPos == activationPos()`). */
        boolean shouldActivate(long outPos);

        /** Activate the rule and produce a SegmentReader to push. */
        SegmentReader activate();
    }

    /**
     * SegmentReader is a lightweight reader-like object. It must:
     * - return -1 on EOF
     * - supply skipAfterFinish() which returns how many characters should be skipped from the next reader
     *   in the stack when this reader finishes (0 if none).
     * - be closeable
     */
    private interface SegmentReader {
        int read(char[] buf, int off, int len) throws IOException;

        /** Single char convenience. */
        default int read() throws IOException {
            char[] tmp = new char[1];
            int n = read(tmp, 0, 1);
            return (n <= 0) ? -1 : tmp[0];
        }

        /** When this segment finishes (EOF), how many characters must be consumed from the next reader? */
        long skipAfterFinish();

        void close() throws IOException;
    }

    // ----- Concrete SegmentRule implementations -----

    // Replace a fixed range [start, end] with a replacement Reader.
    private static class ReplaceRangeRule implements SegmentRule {
        private final long start;
        private final long endInclusive; // -1 -> to end
        private final Reader replacement;

        ReplaceRangeRule(long start, long endInclusive, Reader replacement) {
            if (start < 0) throw new IllegalArgumentException("start < 0");
            if (endInclusive < -1) throw new IllegalArgumentException("end < -1");
            this.start = start;
            this.endInclusive = endInclusive;
            this.replacement = replacement;
        }

        @Override
        public long activationPos() {
            return start;
        }

        @Override
        public boolean shouldActivate(long outPos) {
            return outPos == start;
        }

        @Override
        public SegmentReader activate() {
            long maxChars = (endInclusive < 0) ? Long.MAX_VALUE : (endInclusive - start + 1);
            return new ReplacementSegmentReader(replacement, maxChars, /*skipOriginalAfter=*/true);
        }
    }

    // Insert at position: produce replacement, skip none from upstream.
    private static class InsertAtRule implements SegmentRule {
        private final long start;
        private final Reader replacement;

        InsertAtRule(long start, Reader replacement) {
            if (start < 0) throw new IllegalArgumentException("start < 0");
            this.start = start;
            this.replacement = replacement;
        }

        @Override
        public long activationPos() {
            return start;
        }

        @Override
        public boolean shouldActivate(long outPos) {
            return outPos == start;
        }

        @Override
        public SegmentReader activate() {
            return new ReplacementSegmentReader(replacement, Long.MAX_VALUE, /*skipOriginalAfter=*/false);
        }
    }

    // Delete range: does not produce any characters but causes skipping of upstream region.
    private static class DeleteRangeRule implements SegmentRule {
        private final long start;
        private final long endInclusive;

        DeleteRangeRule(long start, long endInclusive) {
            if (start < 0) throw new IllegalArgumentException("start < 0");
            if (endInclusive < start) throw new IllegalArgumentException("end < start");
            this.start = start;
            this.endInclusive = endInclusive;
        }

        @Override
        public long activationPos() {
            return start;
        }

        @Override
        public boolean shouldActivate(long outPos) {
            return outPos == start;
        }

        @Override
        public SegmentReader activate() {
            long count = endInclusive - start + 1;
            return new DeleteSegmentReader(count);
        }
    }

    // Replace starting at start with reader until replacement EOFs; skip same amount of upstream produced.
    private static class ReplaceUntilExhaustedRule implements SegmentRule {
        private final long start;
        private final Reader replacement;

        ReplaceUntilExhaustedRule(long start, Reader replacement) {
            if (start < 0) throw new IllegalArgumentException("start < 0");
            this.start = start;
            this.replacement = replacement;
        }

        @Override
        public long activationPos() {
            return start;
        }

        @Override
        public boolean shouldActivate(long outPos) {
            return outPos == start;
        }

        @Override
        public SegmentReader activate() {
            return new ReplacementSegmentReader(replacement, Long.MAX_VALUE, /*skipOriginalAfter=*/true, /*skipEqualToProduced=*/true);
        }
    }

    // ----- Concrete SegmentReader implementations -----

    // ReplacementSegmentReader reads from an underlying replacement Reader.
    // Options:
    // - maxChars: maximum number of chars to produce (if replacement produces more, extra are ignored)
    // - skipOriginalAfter: if true, when replacement EOFs early we will skip remaining original chars in the declared range
    // - skipEqualToProduced: when true (used for replace-until-exhausted), skip the same number of original chars as produced
    private static class ReplacementSegmentReader implements SegmentReader {
        private final Reader source;
        private final long maxChars; // limit on produced chars (Long.MAX_VALUE => no explicit limit)
        private final boolean skipOriginalAfter;
        private final boolean skipEqualToProduced; // overrides other skip semantics when true

        private long produced = 0;
        private boolean closed = false;

        ReplacementSegmentReader(Reader source, long maxChars, boolean skipOriginalAfter) {
            this(source, maxChars, skipOriginalAfter, false);
        }

        ReplacementSegmentReader(Reader source, long maxChars, boolean skipOriginalAfter, boolean skipEqualToProduced) {
            this.source = Objects.requireNonNull(source);
            this.maxChars = maxChars;
            this.skipOriginalAfter = skipOriginalAfter;
            this.skipEqualToProduced = skipEqualToProduced;
        }

        @Override
        public int read(char[] buf, int off, int len) throws IOException {
            if (produced >= maxChars) return -1; // reached declared limit
            int toRead = (int) Math.min(len, maxChars - produced);
            int n = source.read(buf, off, toRead);
            if (n == -1) {
                return -1;
            }
            produced += n;
            return n;
        }

        @Override
        public long skipAfterFinish() {
            try {
                // If skipEqualToProduced: skip same number of original chars as produced.
                if (skipEqualToProduced) {
                    return produced;
                }
                if (!skipOriginalAfter) {
                    return 0L;
                }
                if (maxChars == Long.MAX_VALUE) {
                    // replace until end-of-stream with skipOriginalAfter=true means no upstream skip
                    // (common for 'replace to end' semantics). But for a replaceRange with end==-1,
                    // we interpret that as "replace to end" with no upstream left anyway.
                    return 0L;
                }
                long remainingInRange = Math.max(0L, maxChars - produced);
                return remainingInRange;
            } finally {
                // nothing else
            }
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                source.close();
                closed = true;
            }
        }
    }

    // A segment that produces no characters but causes skipping of `count` characters from the next reader.
    private static class DeleteSegmentReader implements SegmentReader {
        private final long count;
        private boolean finished = false;

        DeleteSegmentReader(long count) {
            this.count = count;
        }

        @Override
        public int read(char[] buf, int off, int len) {
            // produces no characters at all; immediate EOF
            finished = true;
            return -1;
        }

        @Override
        public long skipAfterFinish() {
            return count;
        }

        @Override
        public void close() {
            // nothing to close
        }
    }

    // Upstream wrapper that reads from the actual base Reader.
    // We treat it as a SegmentReader to push on the stack initially.
    private static class UpstreamSegmentReader implements SegmentReader {
        private final Reader upstream;
        private boolean closed = false;

        UpstreamSegmentReader(Reader upstream) {
            this.upstream = Objects.requireNonNull(upstream);
        }

        @Override
        public int read(char[] buf, int off, int len) throws IOException {
            return upstream.read(buf, off, len);
        }

        @Override
        public long skipAfterFinish() {
            return 0L;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                upstream.close();
                closed = true;
            }
        }
    }

    // ----- OverlayReader internals -----

    private final Deque<SegmentReader> readerStack = new ArrayDeque<>();
    private final Deque<SegmentRule> rules; // precomputed & sorted by activation position
    private long outPos = 0L;              // number of characters emitted so far
    private final char[] skipBuffer = new char[8192]; // used for skipping by reading

    private boolean closed = false;

    private OverlayReader(Reader upstream, List<SegmentRule> rules) {
        this.rules = new ArrayDeque<>(rules); // already sorted by Builder
        // push upstream as the bottom of the stack
        readerStack.push(new UpstreamSegmentReader(upstream));
    }

    // Skip `toSkip` characters from the current top-of-stack reader by reading and discarding.
    // This will consume whatever the top reader produces (including possibly activating further rules
    // in edge cases — but per model, we only call skip right after popping a finished segment).
    private void skipFromTop(long toSkip) throws IOException {
        long remaining = toSkip;
        while (remaining > 0) {
            if (readerStack.isEmpty()) return;
            SegmentReader top = readerStack.peek();
            // read up to skipBuffer.length or remaining
            int read = top.read(skipBuffer, 0, (int) Math.min(skipBuffer.length, remaining));
            if (read == -1) {
                // top exhausted — pop it and ask for its skip; apply recursively
                readerStack.pop();
                top.close(); // close popped reader
                long skipMore = top.skipAfterFinish();
                // append skipMore to remaining, because skipFromTop must ensure the total skipping is applied
                remaining += skipMore;
                continue;
            }
            remaining -= read;
            // we are discarding the characters; outPos does NOT advance for skipped characters
        }
    }

    // ----- Read implementations -----

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (closed) throw new IOException("OverlayReader is closed");
        if (cbuf == null) throw new NullPointerException("cbuf is null");
        if (off < 0 || len < 0 || off + len > cbuf.length) throw new IndexOutOfBoundsException();

        if (len == 0) return 0;

        int totalRead = 0;

        while (totalRead < len) {
            // Check activation rules before each attempt to read
            SegmentRule rule = rules.peek();

            if (rule != null && rule.shouldActivate(outPos)) {
                rules.pop();

                SegmentReader reader = rule.activate();
                readerStack.push(reader);
            }

            if (readerStack.isEmpty()) {
                // Nothing left to read
                break;
            }

            SegmentReader top = readerStack.peek();
            int n = top.read(cbuf, off + totalRead, len - totalRead);

            if (n == -1) {
                // top finished; pop and perform skip
                readerStack.pop();
                // ensure top is closed
                top.close();
                long skip = top.skipAfterFinish();
                if (skip > 0) {
                    skipFromTop(skip);
                }
                // after pop & skip, loop to attempt reading again (may activate further rules)
                continue;
            }

            // produced characters — consume them and advance outPos
            totalRead += n;
            outPos += n;
            // continue loop to fill more if requested
        }

        if (totalRead == 0) {
            // either EOF or couldn't produce anything
            return -1;
        }
        return totalRead;
    }

    @Override
    public int read() throws IOException {
        char[] tmp = new char[1];
        int n = read(tmp, 0, 1);
        return (n == -1) ? -1 : tmp[0];
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        // close everything on stack
        IOException exc = null;
        while (!readerStack.isEmpty()) {
            SegmentReader sr = readerStack.pop();
            try {
                sr.close();
            } catch (IOException e) {
                if (exc == null) exc = e;
                else exc.addSuppressed(e);
            }
        }
        closed = true;
        if (exc != null) throw exc;
    }

    // Convenience: read entire result to String
    public String readAll() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = read(buf, 0, buf.length)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
//
//    // ----- Example usage & small self-test -----
//    public static void main(String[] args) throws Exception {
//        // Example 1: simple replace from index 5 to end with "hack"
//        String src = "some text";
//        OverlayReader r1 = new OverlayReader.Builder(new StringReader(src))
//                .replaceRange(5, -1, new StringReader("hack")) // replace from char index 5 to end
//                .build();
//
//        System.out.println(r1.readAll()); // expected "some hack"
//        r1.close();
//
//        // Example 2: insert at position 4
//        OverlayReader r2 = new OverlayReader.Builder(new StringReader("HelloWorld"))
//                .insertAt(5, new StringReader(" ")) // insert a space between Hello and World
//                .build();
//        System.out.println(r2.readAll()); // expected "Hello World"
//        r2.close();
//
//        // Example 3: delete range
//        OverlayReader r3 = new OverlayReader.Builder(new StringReader("0123456789"))
//                .deleteRange(3, 5) // remove chars '3','4','5'
//                .build();
//        System.out.println(r3.readAll()); // expected "0126789"
//        r3.close();
//
//        // Example 4: replace until exhausted then skip same amount in source
//        OverlayReader r4 = new OverlayReader.Builder(new StringReader("ABCDEFGHIJ"))
//                .replaceUntilExhausted(2, new StringReader("XXY"))
//                .build();
//        // starting at position 2 (after 'A','B'): output "XXY", then skip 3 original chars ('C','D','E'), continue with 'F'...
//        System.out.println(r4.readAll()); // expected "ABXXYFGHIJ"
//        r4.close();
//
//        // Example 5: overlapping insert & replace at same position - inserts applied in declaration order (builder sorts by pos only)
//        OverlayReader r5 = new OverlayReader.Builder(new StringReader("ABCD"))
//                .insertAt(2, new StringReader("X"))
//                .insertAt(2, new StringReader("Y")) // both activate at pos 2; builder order preserved for equal-start
//                .build();
//        System.out.println(r5.readAll()); // expected "ABXYCD"
//        r5.close();
//    }
}
