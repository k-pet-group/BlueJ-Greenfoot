/*
 This file is part of the BlueJ program. 
 Copyright (C) 2026 Michael Kölling and John Rosenberg

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility.javafx;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaFXUtil}'s cross-thread execution methods:
 * {@code runPlatformAndWait} (blocking) and {@code runPlatformFuture} (async).
 *
 * <p>Covers all functional interface variants (Runnable, Supplier, Consumer,
 * Function, BiConsumer, BiFunction), testing both on-FX-thread (direct
 * execution) and off-FX-thread (scheduled execution) scenarios, as well as
 * exception propagation for checked and unchecked exceptions.</p>
 *
 * <p>Extends {@link ApplicationTest} to ensure the JavaFX toolkit is
 * initialised before tests run.</p>
 */
public class JavaFXUtilTest extends ApplicationTest {
    @Override
    public void start(Stage stage) throws Exception {
        // No UI needed; we just require the FX toolkit to be initialised.
    }

    // ========================================================================
    // Helper: run a Runnable from a non-FX thread and wait for its result
    // ========================================================================

    private void runOffFxThread(RunnableThrowing action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                latch.countDown();
            }
        });
        t.start();
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test thread timed out");
        if (error.get() != null) {
            throw new RuntimeException("Off-FX thread threw", error.get());
        }
    }

    // ========================================================================
    // 1. FXPlatformRunnableThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformRunnable_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        runOffFxThread(() -> {
            JavaFXUtil.runPlatformAndWait((FXPlatformRunnableThrowing) () -> {
                Thread.sleep(100);
                wasOnFxThread.set(Platform.isFxApplicationThread());
                runOrder.add("inside");
                threadIds.add(Thread.currentThread().threadId());
            });
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Task should have run on FX thread");
    }

    @Test
    public void testRunPlatformFutureRunnable_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        runOffFxThread(() -> {
            Future<Void> future = JavaFXUtil.runPlatformFuture((FXPlatformRunnableThrowing) () -> {
                Thread.sleep(100);
                wasOnFxThread.set(Platform.isFxApplicationThread());
                runOrder.add("inside");
                threadIds.add(Thread.currentThread().threadId());
            });
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Task should have run on FX thread");
    }

    @Test
    public void testRunPlatformRunnable_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        // Run from FX thread to test direct execution path
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformRunnableThrowing) () -> {
                    Thread.sleep(100);
                    runOrder.add("inside");
                    threadIds.add(Thread.currentThread().threadId());
                });
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(null);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        done.get(5, TimeUnit.SECONDS);
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformRunnable_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformRunnableThrowing) () -> {
                    throw new IOException("test exception");
                });
                fail("Should have thrown RuntimeException");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get(), "Should have caught RuntimeException");
        // The IOException is wrapped in ExecutionException by Future.get(),
        // then wrapped in RuntimeException by runPlatformAndWait
        assertTrue(findCause(caught.get(), IOException.class), "Root cause chain should contain IOException");
    }

    @Test
    public void testRunPlatformFutureRunnable_exceptionPropagation() throws Exception {
        AtomicReference<Future<Void>> futureRef = new AtomicReference<>();
        runOffFxThread(() -> {
            futureRef.set(JavaFXUtil.runPlatformFuture((FXPlatformRunnableThrowing) () -> {
                throw new IOException("async test exception");
            }));
        });
        Future<Void> future = futureRef.get();
        assertNotNull(future);
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Future.get() should have thrown ExecutionException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException, "Cause should be IOException");
            assertEquals("async test exception", e.getCause().getMessage());
        }
    }

    // ========================================================================
    // 2. FXPlatformSupplierThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformSupplier_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        runOffFxThread(() -> {
            String value = JavaFXUtil.runPlatformAndWait((FXPlatformSupplierThrowing<String>) () -> {
                Thread.sleep(100);
                wasOnFxThread.set(Platform.isFxApplicationThread());
                runOrder.add("inside");
                threadIds.add(Thread.currentThread().threadId());
                return "hello there";
            });
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            result.set(value);
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Supplier should have run on FX thread");
        assertEquals("hello there", result.get());
    }

    @Test
    public void testRunPlatformFutureSupplier_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<Future<Integer>> futureRef = new AtomicReference<>();
        runOffFxThread(() -> {
            Future<Integer> future = JavaFXUtil.runPlatformFuture((FXPlatformSupplierThrowing<Integer>) () -> {
                Thread.sleep(100);
                wasOnFxThread.set(Platform.isFxApplicationThread());
                runOrder.add("inside");
                threadIds.add(Thread.currentThread().threadId());
                return 42;
            });
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");
            futureRef.set(future);

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Supplier should have run on FX thread");
        assertEquals(Integer.valueOf(42), futureRef.get().get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testRunPlatformSupplier_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        CompletableFuture<String> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                String value = JavaFXUtil.runPlatformAndWait((FXPlatformSupplierThrowing<String>) () -> {
                    Thread.sleep(100);
                    runOrder.add("inside");
                    threadIds.add(Thread.currentThread().threadId());
                    return "direct";
                });
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(value);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        assertEquals("direct", done.get(5, TimeUnit.SECONDS));
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformSupplier_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformSupplierThrowing<String>) () -> {
                    throw new IOException("supplier error");
                });
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IOException.class));
    }

    // ========================================================================
    // 3. FXPlatformConsumerThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformConsumer_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> consumed = new AtomicReference<>();
        runOffFxThread(() -> {
            JavaFXUtil.runPlatformAndWait((FXPlatformConsumerThrowing<String>) s -> {
                Thread.sleep(100);
                wasOnFxThread.set(Platform.isFxApplicationThread());
                consumed.set(s);
                runOrder.add("inside");
                threadIds.add(Thread.currentThread().threadId());
            }, "world");
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Consumer should have run on FX thread");
        assertEquals("world", consumed.get());
    }

    @Test
    public void testRunPlatformFutureConsumer_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<Integer> consumed = new AtomicReference<>();
        runOffFxThread(() -> {
            Future<Void> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformConsumerThrowing<Integer>) i -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        consumed.set(i);
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                    }, 99);
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Consumer should have run on FX thread");
        assertEquals(Integer.valueOf(99), consumed.get());
    }

    @Test
    public void testRunPlatformConsumer_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformConsumerThrowing<String>) s -> {
                    Thread.sleep(100);
                    runOrder.add("inside");
                    threadIds.add(Thread.currentThread().threadId());
                }, "fx-direct");
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(null);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        done.get(5, TimeUnit.SECONDS);
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformConsumer_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformConsumerThrowing<String>) s -> {
                    throw new IOException("consumer error: " + s);
                }, "arg");
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IOException.class));
    }

    // ========================================================================
    // 4. FXPlatformFunctionThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformFunction_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<Integer> result = new AtomicReference<>();
        runOffFxThread(() -> {
            int value = JavaFXUtil.runPlatformAndWait(
                    (FXPlatformFunctionThrowing<String, Integer>) s -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                        return s.length();
                    }, "hello");
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            result.set(value);
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Function should have run on FX thread");
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testRunPlatformFutureFunction_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<Future<String>> futureRef = new AtomicReference<>();
        runOffFxThread(() -> {
            Future<String> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformFunctionThrowing<Integer, String>) i -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                        return "num:" + i;
                    }, 7);
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");
            futureRef.set(future);

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "Function should have run on FX thread");
        assertEquals("num:7", futureRef.get().get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testRunPlatformFunction_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        CompletableFuture<Integer> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                int value = JavaFXUtil.runPlatformAndWait(
                        (FXPlatformFunctionThrowing<String, Integer>) s -> {
                            Thread.sleep(100);
                            runOrder.add("inside");
                            threadIds.add(Thread.currentThread().threadId());
                            return s.length();
                        }, "ab");
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(value);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        assertEquals(Integer.valueOf(2), done.get(5, TimeUnit.SECONDS));
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformFunction_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformFunctionThrowing<String, Integer>) s -> {
                    throw new IOException("function error");
                }, "x");
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IOException.class));
    }

    // ========================================================================
    // 5. FXPlatformBiConsumerThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformBiConsumer_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> consumed = new AtomicReference<>();
        runOffFxThread(() -> {
            JavaFXUtil.runPlatformAndWait(
                    (FXPlatformBiConsumerThrowing<String, Integer>) (s, i) -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        consumed.set(s + ":" + i);
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                    },
                    "key", 42);
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "BiConsumer should have run on FX thread");
        assertEquals("key:42", consumed.get());
    }

    @Test
    public void testRunPlatformFutureBiConsumer_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> consumed = new AtomicReference<>();
        runOffFxThread(() -> {
            Future<Void> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformBiConsumerThrowing<String, String>) (a, b) -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        consumed.set(a + b);
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                    },
                    "foo", "bar");
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "BiConsumer should have run on FX thread");
        assertEquals("foobar", consumed.get());
    }

    @Test
    public void testRunPlatformBiConsumer_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                JavaFXUtil.runPlatformAndWait(
                        (FXPlatformBiConsumerThrowing<String, String>) (a, b) -> {
                            Thread.sleep(100);
                            runOrder.add("inside");
                            threadIds.add(Thread.currentThread().threadId());
                        },
                        "left", "right");
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(null);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        done.get(5, TimeUnit.SECONDS);
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformBiConsumer_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait(
                        (FXPlatformBiConsumerThrowing<String, String>) (a, b) -> {
                            throw new IOException("biconsumer error");
                        }, "a", "b");
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IOException.class));
    }

    // ========================================================================
    // 6. FXPlatformBiFunctionThrowing — runPlatformAndWait / runPlatformFuture
    // ========================================================================

    @Test
    public void testRunPlatformBiFunction_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        runOffFxThread(() -> {
            String value = JavaFXUtil.runPlatformAndWait(
                    (FXPlatformBiFunctionThrowing<String, Integer, String>) (s, i) -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                        return s.repeat(i);
                    },
                    "ab", 3);
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            result.set(value);
        });
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "BiFunction should have run on FX thread");
        assertEquals("ababab", result.get());
    }

    @Test
    public void testRunPlatformFutureBiFunction_fromNonFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        AtomicBoolean wasOnFxThread = new AtomicBoolean(false);
        AtomicReference<Future<Integer>> futureRef = new AtomicReference<>();
        runOffFxThread(() -> {
            Future<Integer> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformBiFunctionThrowing<Integer, Integer, Integer>) (a, b) -> {
                        Thread.sleep(100);
                        wasOnFxThread.set(Platform.isFxApplicationThread());
                        runOrder.add("inside");
                        threadIds.add(Thread.currentThread().threadId());
                        return Integer.sum(a, b);
                    },
                    10, 20);
            runOrder.add("outside");
            threadIds.add(Thread.currentThread().threadId());
            assertFalse(future.isDone(), "Future should not be immediately done from non-FX thread");
            futureRef.set(future);

            future.get(5, TimeUnit.SECONDS);
        });
        assertIterableEquals(List.of("outside", "inside"), runOrder, "Task should have run asynchronously");
        assertEquals(2, threadIds.stream().distinct().count(), "Task should have run on a different thread");
        assertTrue(wasOnFxThread.get(), "BiFunction should have run on FX thread");
        assertEquals(Integer.valueOf(30), futureRef.get().get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testRunPlatformBiFunction_directOnFxThread() throws Exception {
        ConcurrentLinkedQueue<String> runOrder = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> threadIds = new ConcurrentLinkedQueue<>();
        CompletableFuture<Integer> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                int value = JavaFXUtil.runPlatformAndWait(
                        (FXPlatformBiFunctionThrowing<Integer, Integer, Integer>) (a, b) -> {
                            Thread.sleep(100);
                            runOrder.add("inside");
                            threadIds.add(Thread.currentThread().threadId());
                            return Integer.sum(a, b);
                        },
                        3, 4);
                runOrder.add("outside");
                threadIds.add(Thread.currentThread().threadId());
                done.complete(value);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        assertEquals(Integer.valueOf(7), done.get(5, TimeUnit.SECONDS));
        assertIterableEquals(List.of("inside", "outside"), runOrder, "Task should have run synchronously");
        assertEquals(1, threadIds.stream().distinct().count(), "Task should have run on the same thread");
    }

    @Test
    public void testRunPlatformBiFunction_exceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait(
                        (FXPlatformBiFunctionThrowing<String, String, String>) (a, b) -> {
                            throw new IOException("bifunction error");
                        }, "a", "b");
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IOException.class));
    }

    // ========================================================================
    // 7. Future-specific behaviour tests
    // ========================================================================

    @Test
    public void testRunPlatformFutureRunnable_completedFutureOnFxThread() throws Exception {
        CompletableFuture<Boolean> isDone = new CompletableFuture<>();
        Platform.runLater(() -> {
            Future<Void> future = JavaFXUtil.runPlatformFuture((FXPlatformRunnableThrowing) () -> {
                Thread.sleep(100);
                // no-op
            });
            isDone.complete(future.isDone());
        });
        assertTrue(isDone.get(5, TimeUnit.SECONDS), "Future should be immediately done when called from FX thread");
    }

    @Test
    public void testRunPlatformFutureSupplier_completedFutureOnFxThread() throws Exception {
        CompletableFuture<Boolean> isDone = new CompletableFuture<>();
        Platform.runLater(() -> {
            Future<String> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformSupplierThrowing<String>) () -> {
                        Thread.sleep(100);
                        return "sync";
                    });
            isDone.complete(future.isDone());
        });
        assertTrue(isDone.get(5, TimeUnit.SECONDS), "Future should be immediately done when called from FX thread");
    }

    @Test
    public void testRunPlatformFutureFunction_completedFutureOnFxThread() throws Exception {
        CompletableFuture<Boolean> isDone = new CompletableFuture<>();
        Platform.runLater(() -> {
            Future<Integer> future = JavaFXUtil.runPlatformFuture(
                    (FXPlatformFunctionThrowing<String, Integer>) (string) -> {
                        Thread.sleep(100);
                        return string.length();
                    }, "test");
            isDone.complete(future.isDone());
        });
        assertTrue(isDone.get(5, TimeUnit.SECONDS), "Future should be immediately done when called from FX thread");
    }

    @Test
    public void testRunPlatformFutureRunnable_failedFutureOnFxThread() throws Exception {
        CompletableFuture<Future<Void>> futureRef = new CompletableFuture<>();
        Platform.runLater(() -> {
            Future<Void> future = JavaFXUtil.runPlatformFuture((FXPlatformRunnableThrowing) () -> {
                throw new IOException("direct-fail");
            });
            futureRef.complete(future);
        });
        Future<Void> future = futureRef.get(5, TimeUnit.SECONDS);
        assertTrue(future.isDone(), "Failed future should be done");
        try {
            future.get();
            fail("Should have thrown ExecutionException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    // ========================================================================
    // 8. Unchecked (RuntimeException) propagation
    // ========================================================================

    @Test
    public void testRunPlatformRunnable_uncheckedExceptionPropagation() throws Exception {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        runOffFxThread(() -> {
            try {
                JavaFXUtil.runPlatformAndWait((FXPlatformRunnableThrowing) () -> {
                    throw new IllegalStateException("unchecked");
                });
                fail("Should have thrown");
            } catch (RuntimeException e) {
                caught.set(e);
            }
        });
        assertNotNull(caught.get());
        assertTrue(findCause(caught.get(), IllegalStateException.class));
    }

    // ========================================================================
    // 9. Thread verification — tasks always execute on FX thread
    // ========================================================================

    @Test
    public void testAllVariants_executeOnFxThread() throws Exception {
        AtomicInteger fxThreadCount = new AtomicInteger(0);

        runOffFxThread(() -> {
            assertFalse(Platform.isFxApplicationThread(), "Test should not be on FX thread");

            // Runnable
            JavaFXUtil.runPlatformAndWait((FXPlatformRunnableThrowing) () -> {
                if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
            });

            // Supplier
            JavaFXUtil.runPlatformAndWait((FXPlatformSupplierThrowing<Boolean>) () -> {
                if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
                return true;
            });

            // Consumer
            JavaFXUtil.runPlatformAndWait((FXPlatformConsumerThrowing<String>) s -> {
                if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
            }, "x");

            // Function
            JavaFXUtil.runPlatformAndWait((FXPlatformFunctionThrowing<String, Integer>) s -> {
                if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
                return 0;
            }, "x");

            // BiConsumer
            JavaFXUtil.runPlatformAndWait((FXPlatformBiConsumerThrowing<String, String>) (a, b) -> {
                if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
            }, "a", "b");

            // BiFunction
            JavaFXUtil.runPlatformAndWait(
                    (FXPlatformBiFunctionThrowing<String, String, Integer>) (a, b) -> {
                        if (Platform.isFxApplicationThread()) fxThreadCount.incrementAndGet();
                        return 0;
                    }, "a", "b");
        });

        assertEquals(6, fxThreadCount.get(), "All 6 variants should have run on FX thread");
    }

    // ========================================================================
    // 10. Functional interface compilation / type-checking smoke tests
    // ========================================================================

    @Test
    public void testFunctionalInterfaceSmokeTest_generics() throws Exception {
        // Verify generic throwing interfaces compile and work correctly
        RunnableThrowing rt = () -> {
        };
        rt.run();

        ConsumerThrowing<String> ct = s -> {
        };
        ct.accept("test");

        SupplierThrowing<Integer> st = () -> 42;
        assertEquals(Integer.valueOf(42), st.get());

        FunctionThrowing<String, Integer> ft = String::length;
        assertEquals(Integer.valueOf(3), ft.apply("abc"));

        BiConsumerThrowing<String, Integer> bct = (s, i) -> {
        };
        bct.accept("a", 1);

        BiFunctionThrowing<String, Integer, String> bft = (s, i) -> s + i;
        assertEquals("x1", bft.apply("x", 1));
    }

    @Test
    public void testFunctionalInterfaceSmokeTest_fxBiFunction() {
        // Verify the newly created FXBiFunction compiles and works
        FXBiFunction<String, Integer, String> fn = String::repeat;
        assertEquals("aaa", fn.apply("a", 3));
    }

    /**
     * Tests that thread-checker tag inference works when a lambda is assigned
     * to a variable with an explicit functional interface type. The tag should
     * be inferred from the target type (e.g. FXPlatformRunnableThrowing →
     * Tag.FXPlatform). These lambdas should then be accepted by runPlatformAndWait.
     */
    @Test
    public void testLambdaTagInference_explicitType() throws Exception {
        // Explicit type: the thread checker sees the target type and infers
        // the lambda's tag from the interface annotation.
        FXPlatformRunnableThrowing runnable = () -> {};
        FXPlatformSupplierThrowing<String> supplier = () -> "typed";
        FXPlatformConsumerThrowing<String> consumer = s -> {};
        FXPlatformFunctionThrowing<String, Integer> function = String::length;
        FXPlatformBiConsumerThrowing<String, Integer> biConsumer = (s, i) -> {};
        FXPlatformBiFunctionThrowing<String, Integer, String> biFunction = (s, i) -> s.repeat(i);

        // Verify they work when passed to runPlatformAndWait from off-FX thread
        AtomicReference<String> result = new AtomicReference<>();
        runOffFxThread(() -> {
            JavaFXUtil.runPlatformAndWait(runnable);
            String s = JavaFXUtil.runPlatformAndWait(supplier);
            assertEquals("typed", s);
            JavaFXUtil.runPlatformAndWait(consumer, "x");
            int len = JavaFXUtil.runPlatformAndWait(function, "hello");
            assertEquals(5, len);
            JavaFXUtil.runPlatformAndWait(biConsumer, "a", 1);
            result.set(JavaFXUtil.runPlatformAndWait(biFunction, "ab", 3));
        });
        assertEquals("ababab", result.get());
    }

    /**
     * Tests that thread-checker tag inference works when a lambda is assigned
     * to a {@code var} variable. With {@code var}, the compiler infers the
     * type from the cast expression (e.g. {@code var r = (FXPlatformRunnableThrowing) () -> {}}).
     * This verifies the inferred type retains its thread tag.
     */
    @Test
    public void testLambdaTagInference_varType() throws Exception {
        // var type: requires a cast to guide type inference, but the thread
        // checker should still see the inferred functional interface type.
        var runnable = (FXPlatformRunnableThrowing) () -> {};
        var supplier = (FXPlatformSupplierThrowing<String>) () -> "var-typed";
        var consumer = (FXPlatformConsumerThrowing<String>) (String s) -> {};
        var function = (FXPlatformFunctionThrowing<String, Integer>) String::length;
        var biConsumer = (FXPlatformBiConsumerThrowing<String, Integer>) (String s, Integer i) -> {};
        var biFunction = (FXPlatformBiFunctionThrowing<String, Integer, String>) String::repeat;

        // Verify they work when passed to runPlatformAndWait off the FX thread
        AtomicReference<String> result = new AtomicReference<>();
        runOffFxThread(() -> {
            JavaFXUtil.runPlatformAndWait(runnable);
            String s = JavaFXUtil.runPlatformAndWait(supplier);
            assertEquals("var-typed", s);
            JavaFXUtil.runPlatformAndWait(consumer, "x");
            int len = JavaFXUtil.runPlatformAndWait(function, "hello");
            assertEquals(5, len);
            JavaFXUtil.runPlatformAndWait(biConsumer, "a", 1);
            result.set(JavaFXUtil.runPlatformAndWait(biFunction, "ab", 3));
        });
        assertEquals("ababab", result.get());
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Walks the cause chain of the given throwable looking for an instance of
     * the expected class.
     */
    private boolean findCause(Throwable throwable, Class<? extends Throwable> expected) {
        Throwable current = throwable;
        while (current != null) {
            if (expected.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
