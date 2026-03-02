/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.di.test;

import bluej.di.BlueJInjector;
import bluej.di.BlueJInjectorTestUtils;
import bluej.di.modules.ProjectScopeModule;
import bluej.di.scopes.*;
import bluej.di.scopes.ProjectScopeTestUtils;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests for the DI scope lifecycle.
 *
 * <p>These tests verify the core scope mechanics without requiring
 * a full BlueJ environment or real {@code Project} instances.
 *
 * <p><b>Thread safety:</b> Each test method resets the injector and
 * scope state in {@code setUp}/{@code tearDown}. Cross-thread tests
 * use {@link Future#get}, {@link CountDownLatch}, and
 * {@link AtomicReference} for happens-before guarantees. Executor
 * threads clean up their scope stacks via try-with-resources in
 * {@link ProjectScope#propagateScope}.
 */

public class ProjectScopeIntegrationTest {

    @Before
    public void setUp() {
        BlueJInjectorTestUtils.reset();
        ProjectScopeTestUtils.resetThreadLocal();
    }

    @After
    public void tearDown() {
        ProjectScopeTestUtils.resetThreadLocal();
        BlueJInjectorTestUtils.reset();
    }

    // =========================================================================
    // Scope enter / exit lifecycle
    // =========================================================================

    @Test
    public void enterAndClose_scopeIsActiveInsideAndInactiveOutside() {
        ProjectId id = new ProjectId("/test/project");
        var ctx = new ProjectScope.ScopeContext(id);

        assertNull("No scope active before enter", ProjectScope.current());

        try (var handle = ProjectScope.enter(id, ctx)) {
            assertNotNull("Scope active inside enter", ProjectScope.current());
            assertSame("Context matches", ctx, ProjectScope.current());
            assertEquals("ProjectId matches", id, ProjectScope.current().projectId());
        }

        assertNull("No scope active after close", ProjectScope.current());
    }

    @Test
    public void nestedScopes_innerScopeOverridesOuter() {
        ProjectId outer = new ProjectId("/test/outer");
        ProjectId inner = new ProjectId("/test/inner");
        var outerCtx = new ProjectScope.ScopeContext(outer);
        var innerCtx = new ProjectScope.ScopeContext(inner);

        try (var outerHandle = ProjectScope.enter(outer, outerCtx)) {
            assertSame(outerCtx, ProjectScope.current());

            try (var innerHandle = ProjectScope.enter(inner, innerCtx)) {
                assertSame("Inner scope active", innerCtx, ProjectScope.current());
            }

            assertSame("Outer scope restored", outerCtx, ProjectScope.current());
        }

        assertNull("No scope after all closed", ProjectScope.current());
    }

    @Test(expected = IllegalStateException.class)
    public void outOfOrderClose_throws() {
        ProjectId a = new ProjectId("/test/a");
        ProjectId b = new ProjectId("/test/b");
        var ctxA = new ProjectScope.ScopeContext(a);
        var ctxB = new ProjectScope.ScopeContext(b);

        var handleA = ProjectScope.enter(a, ctxA);
        var handleB = ProjectScope.enter(b, ctxB);

        // Closing A (the outer) before B (the inner) should throw
        handleA.close();
    }

    // =========================================================================
    // ScopeContext seeding and lookup
    // =========================================================================

    @Test
    public void seed_andGet_returnsSeededValue() {
        ProjectId id = new ProjectId("/test/seed");
        var ctx = new ProjectScope.ScopeContext(id);

        String value = "test-value";
        ctx.seed(Key.get(String.class), value);

        assertSame(value, ctx.get(Key.get(String.class)));
    }

    @Test
    public void get_unseededKey_returnsNull() {
        ProjectId id = new ProjectId("/test/unseeded");
        var ctx = new ProjectScope.ScopeContext(id);

        assertNull(ctx.get(Key.get(String.class)));
    }

    @Test
    public void clear_removesAllValues() {
        ProjectId id = new ProjectId("/test/clear");
        var ctx = new ProjectScope.ScopeContext(id);
        ctx.seed(Key.get(String.class), "value");

        ctx.clear();

        assertNull("Value cleared", ctx.get(Key.get(String.class)));
    }

    // =========================================================================
    // Guice scope() provider — requires injector
    // =========================================================================

    @Test
    public void scopedProvider_outsideScope_throwsOutOfScope() {
        BlueJInjector.initialize(new ProjectScopeModule());

        // Requesting a @ProjectScoped dependency outside of scope should throw.
        // Guice wraps the OutOfScopeException from the custom provider in a
        // ProvisionException, so we assert on the full cause chain.
        try {
            BlueJInjector.getInstance(
                bluej.parser.context.CompilationUnitContextLoader.class);
            fail("Expected ProvisionException wrapping OutOfScopeException");
        } catch (ProvisionException e) {
            Throwable cause = e.getCause();
            assertNotNull("ProvisionException should have a cause", cause);
            assertTrue(
                "Root cause should be OutOfScopeException, got: " + cause.getClass().getName(),
                cause instanceof OutOfScopeException);
        }
    }

    // =========================================================================
    // Cross-thread scope propagation
    // =========================================================================

    @Test
    public void propagateScope_runnable_scopeActiveOnOtherThread() throws Exception {
        ProjectId id = new ProjectId("/test/propagate");
        var ctx = new ProjectScope.ScopeContext(id);
        ctx.seed(Key.get(String.class), "propagated");

        AtomicReference<ProjectScope.ScopeContext> captured = new AtomicReference<>();

        try (var handle = ProjectScope.enter(id, ctx)) {
            Runnable scoped = ProjectScope.propagateScope(() ->
                captured.set(ProjectScope.current()));

            // Run on a different thread; .get() provides happens-before
            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                exec.submit(scoped).get(5, TimeUnit.SECONDS);
            } finally {
                exec.shutdown();
                exec.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        assertNotNull("Scope was propagated", captured.get());
        assertSame("Same context propagated", ctx, captured.get());
    }

    @Test
    public void propagateScope_supplier_scopeActiveOnOtherThread() throws Exception {
        ProjectId id = new ProjectId("/test/propagate-supplier");
        var ctx = new ProjectScope.ScopeContext(id);
        ctx.seed(Key.get(String.class), "supplier-value");

        try (var handle = ProjectScope.enter(id, ctx)) {
            var scoped = ProjectScope.propagateScope(
                () -> ProjectScope.current() != null ? "scoped" : "unscoped");

            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                String result = exec.submit(scoped::get).get(5, TimeUnit.SECONDS);
                assertEquals("scoped", result);
            } finally {
                exec.shutdown();
                exec.awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void propagateScope_noActiveScope_returnsTaskUnchanged() {
        Runnable original = () -> {};
        Runnable wrapped = ProjectScope.propagateScope(original);

        assertSame("No wrapping when no scope", original, wrapped);
    }

    @Test
    public void captureScope_noActiveScope_returnsNull() {
        assertNull(ProjectScope.captureScope());
    }

    @Test
    public void captureScope_activeScope_returnsSupplierThatEntersScope() throws Exception {
        ProjectId id = new ProjectId("/test/capture");
        var ctx = new ProjectScope.ScopeContext(id);

        try (var handle = ProjectScope.enter(id, ctx)) {
            var capture = ProjectScope.captureScope();
            assertNotNull("Capture should be non-null", capture);

            // Use on another thread
            AtomicReference<ProjectScope.ScopeContext> captured = new AtomicReference<>();

            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                exec.submit(() -> {
                    try (var h = capture.get()) {
                        captured.set(ProjectScope.current());
                    }
                }).get(5, TimeUnit.SECONDS);
            } finally {
                exec.shutdown();
                exec.awaitTermination(5, TimeUnit.SECONDS);
            }

            assertSame("Same context", ctx, captured.get());
        }
    }

    // =========================================================================
    // ScopedExecutor integration
    // =========================================================================

    @Test
    public void scopedExecutor_execute_propagatesScope() throws Exception {
        ProjectId id = new ProjectId("/test/scoped-exec");
        var ctx = new ProjectScope.ScopeContext(id);

        AtomicReference<ProjectScope.ScopeContext> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService rawExec = Executors.newSingleThreadExecutor();
        ScopedExecutor exec = new ScopedExecutor(rawExec);

        try {
            try (var handle = ProjectScope.enter(id, ctx)) {
                exec.execute(() -> {
                    captured.set(ProjectScope.current());
                    latch.countDown();
                });
            }

            // latch.await provides happens-before with latch.countDown
            assertTrue("Task completed", latch.await(5, TimeUnit.SECONDS));
            assertSame("Scope propagated", ctx, captured.get());
        } finally {
            exec.shutdown();
            rawExec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void scopedExecutor_submit_propagatesScope() throws Exception {
        ProjectId id = new ProjectId("/test/scoped-submit");
        var ctx = new ProjectScope.ScopeContext(id);
        ctx.seed(Key.get(String.class), "submitted");

        ExecutorService rawExec = Executors.newSingleThreadExecutor();
        ScopedExecutor exec = new ScopedExecutor(rawExec);

        try {
            String result;
            try (var handle = ProjectScope.enter(id, ctx)) {
                // .get() provides happens-before with task completion
                result = exec.submit(() -> {
                    var current = ProjectScope.current();
                    return current != null
                        ? current.get(Key.get(String.class))
                        : null;
                }).get(5, TimeUnit.SECONDS);
            }

            assertEquals("submitted", result);
        } finally {
            exec.shutdown();
            rawExec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void scopedExecutor_noScope_taskRunsWithoutScope() throws Exception {
        AtomicReference<ProjectScope.ScopeContext> captured = new AtomicReference<>();

        ExecutorService rawExec = Executors.newSingleThreadExecutor();
        ScopedExecutor exec = new ScopedExecutor(rawExec);

        try {
            // No scope active — task should run without scope
            CountDownLatch latch = new CountDownLatch(1);
            exec.execute(() -> {
                captured.set(ProjectScope.current());
                latch.countDown();
            });

            assertTrue("Task completed", latch.await(5, TimeUnit.SECONDS));
            assertNull("No scope propagated", captured.get());
        } finally {
            exec.shutdown();
            rawExec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // =========================================================================
    // Thread isolation
    // =========================================================================

    @Test
    public void differentThreads_haveSeparateScopeStacks() throws Exception {
        ProjectId id1 = new ProjectId("/test/thread1");
        ProjectId id2 = new ProjectId("/test/thread2");
        var ctx1 = new ProjectScope.ScopeContext(id1);
        var ctx2 = new ProjectScope.ScopeContext(id2);

        AtomicReference<ProjectId> thread1Scope = new AtomicReference<>();
        AtomicReference<ProjectId> thread2Scope = new AtomicReference<>();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try (var h = ProjectScope.enter(id1, ctx1)) {
                ready.countDown();
                ready.await(5, TimeUnit.SECONDS);
                var current = ProjectScope.current();
                thread1Scope.set(current != null ? current.projectId() : null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try (var h = ProjectScope.enter(id2, ctx2)) {
                ready.countDown();
                ready.await(5, TimeUnit.SECONDS);
                var current = ProjectScope.current();
                thread2Scope.set(current != null ? current.projectId() : null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        t1.start();
        t2.start();
        assertTrue("Threads completed", done.await(10, TimeUnit.SECONDS));

        // Join threads to ensure full cleanup
        t1.join(5000);
        t2.join(5000);

        assertEquals("Thread 1 has its own scope", id1, thread1Scope.get());
        assertEquals("Thread 2 has its own scope", id2, thread2Scope.get());
    }

    // =========================================================================
    // ProjectId canonical path
    // =========================================================================

    @Test
    public void projectId_blankPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectId(""));
        assertThrows(IllegalArgumentException.class, () -> new ProjectId("   "));
    }

    @Test
    public void projectId_sameCanonicalPath_areEqual() throws Exception {
        // Create a temp directory and verify canonical path resolution
        java.io.File tmpDir = java.io.File.createTempFile("test", "dir");
        tmpDir.delete();
        tmpDir.mkdirs();
        try {
            ProjectId id1 = ProjectId.of(tmpDir);
            ProjectId id2 = ProjectId.of(tmpDir.getCanonicalFile());
            assertEquals("Same directory produces same ProjectId", id1, id2);
        } finally {
            tmpDir.delete();
        }
    }

    // =========================================================================
    // ProjectHandle thread-ownership guard
    // =========================================================================

    @Test
    public void handle_closeOnSameThread_succeeds() {
        ProjectId id = new ProjectId("/test/same-thread");
        var ctx = new ProjectScope.ScopeContext(id);
        ProjectHandle handle = ProjectScope.enter(id, ctx);
        handle.close();
        assertNull("Scope should be inactive after close",
            ProjectScope.current());
    }

    @Test
    public void handle_closeOnDifferentThread_throwsIllegalState() throws Exception {
        ProjectId id = new ProjectId("/test/cross-thread-close");
        var ctx = new ProjectScope.ScopeContext(id);
        ProjectHandle handle = ProjectScope.enter(id, ctx);

        // Attempt to close the handle from a different thread
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                handle.close();
            } catch (Throwable t) {
                caught.set(t);
            }
        }, "cross-thread-close-test");
        other.start();
        other.join(5000);

        assertNotNull("close() on wrong thread should throw", caught.get());
        assertTrue(
            "Should be IllegalStateException, got: " +
                caught.get().getClass().getName(),
            caught.get() instanceof IllegalStateException);
        assertTrue(
            "Message should mention thread-bound",
            caught.get().getMessage().contains("thread-bound"));

        // Clean up: close on the owning thread
        handle.close();
    }

    @Test
    public void handle_closeOnDifferentThread_doesNotCorruptOwnerStack() throws Exception {
        ProjectId id = new ProjectId("/test/no-corruption");
        var ctx = new ProjectScope.ScopeContext(id);
        ProjectHandle handle = ProjectScope.enter(id, ctx);

        // The cross-thread close should fail
        Thread other = new Thread(() -> {
            try { handle.close(); } catch (IllegalStateException ignored) {}
        });
        other.start();
        other.join(5000);

        // The owner thread's scope should still be active
        assertNotNull("Scope should still be active on owner thread",
            ProjectScope.current());
        assertEquals("Scope context should be unchanged",
            id, ProjectScope.current().projectId());

        // Normal close on owner thread should work
        handle.close();
        assertNull("Scope should be inactive after proper close",
            ProjectScope.current());
    }
}
