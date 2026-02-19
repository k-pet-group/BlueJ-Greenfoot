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
package bluej.di.scopes;

import com.google.inject.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Custom Guice {@link Scope} for project-scoped dependencies.
 *
 * // TODO: re-add {@link} to ProjectFactory once introduced
 * <p>Each open project owns exactly <b>one</b> {@link ScopeContext}
 * for its entire lifetime, created by the project-opening code
 * and disposed when the project closes.
 *
 * <h3>Thread-local stack</h3>
 * <p>A thread-local stack of {@link ProjectHandle} instances tracks
 * which scope is active on each thread.  Using a stack means nested
 * scope entries (e.g. a callback inside a {@code withScope} block)
 * restore the outer context automatically on exit.
 *
 * <p>The stack stores {@code ProjectHandle} references so that
 * {@link #guardedPop} can verify we are popping the correct handle
 * (catches out-of-order or double-close bugs immediately).
 *
 * @see ProjectScoped
 * @see ProjectHandle
 */
public class ProjectScope implements Scope {

    // ── thread-local scope stack ──────────────────────────────────────

    private static final ThreadLocal<Deque<ProjectHandle>> handleStack =
        ThreadLocal.withInitial(ArrayDeque::new);

    // ── ScopeContext ──────────────────────────────────────────────────

    /**
     * Holds the scoped values for a single project.
     *
     * <p>One instance is created per project and shared across all
     * threads that enter the scope via {@link ProjectHandle}.
     * After initial seeding the context should be treated as
     * read-only (mutations come only from Guice's
     * {@code computeIfAbsent} for lazily-resolved bindings).
     */
    public static final class ScopeContext {

        private final ProjectId projectId;
        private final ConcurrentHashMap<Key<?>, Object> values =
            new ConcurrentHashMap<>();

        public ScopeContext(@NotNull ProjectId projectId) {
            this.projectId = projectId;
        }

        /** Returns the identity of the project this context belongs to. */
        public @NotNull ProjectId projectId() {
            return projectId;
        }

        // ── seeding ──────────────────────────────────────────────────

        /** Seed a value by class type. */
        public <T> void seed(@NotNull Class<T> clazz, @NotNull T value) {
            seed(Key.get(clazz), value);
        }

        /** Seed a value by Guice {@link Key}. */
        public <T> void seed(@NotNull Key<T> key, @NotNull T value) {
            values.put(key, value);
        }

        // ── lookup ───────────────────────────────────────────────────

        /** Retrieve a previously-seeded or cached value. */
        @SuppressWarnings("unchecked")
        public <T> @Nullable T get(@NotNull Key<T> key) {
            return (T) values.get(key);
        }

        /**
         * Atomically retrieve or create-and-cache a value.
         *
         * <p>Uses {@link ConcurrentHashMap#computeIfAbsent} so that
         * concurrent first-access creates at most one instance.
         *
         * <p>Circular proxies produced by Guice during dependency
         * resolution are <b>not</b> cached (the lambda returns
         * {@code null}, which {@code ConcurrentHashMap} does not
         * store).  When that happens, the provider is called once
         * more to obtain the proxy to return to Guice.
         */
        @SuppressWarnings("unchecked")
        <T> T computeIfAbsent(Key<T> key, Provider<T> provider) {
            T result = (T) values.computeIfAbsent(key, k -> {
                T val = provider.get();
                if (Scopes.isCircularProxy(val)) {
                    return null;
                }
                return val;
            });
            // null → circular proxy was not cached; re-invoke to
            // obtain the proxy instance for Guice.
            return result != null ? result : provider.get();
        }

        // ── disposal ─────────────────────────────────────────────────

        /**
         * Clear all values.
         *
         * <p>Called during project disposal.
         */
        public void clear() {
            values.clear();
        }
    }

    // ── Guice Scope implementation ────────────────────────────────────

    @Override
    public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
        return () -> {
            ScopeContext ctx = current();
            if (ctx == null) {
                throw new OutOfScopeException(
                    "Cannot access " + key + " outside of project scope. " +
                    "Ensure this code runs inside an active project scope.");
            }
            return ctx.computeIfAbsent(key, unscoped);
        };
    }

    // ── stack operations ─────────────────────────────────────────────

    /**
     * Create a new {@link ProjectHandle}, push it onto the current
     * thread's stack, and return it.
     *
     * <p>The handle must be {@link ProjectHandle#close() closed}
     * (ideally via try-with-resources) to pop the stack.
     */
    public static @NotNull ProjectHandle enter(@NotNull ProjectId id, @NotNull ScopeContext ctx) {
        ProjectHandle h = new ProjectHandle(id, ctx);
        handleStack.get().push(h);
        return h;
    }

    /**
     * Pop the given handle from the stack, verifying it is the
     * topmost entry.
     *
     * <ul>
     *   <li>Stack empty -- no-op (tolerates double-close).</li>
     *   <li>Top == expected (reference identity) -- pop.</li>
     *   <li>Top != expected -- throw (out-of-order close).</li>
     * </ul>
     */
    static void guardedPop(@NotNull ProjectHandle expected) {
        Deque<ProjectHandle> stack = handleStack.get();
        if (stack.isEmpty()) {
            return;
        }
        ProjectHandle top = stack.peek();
        if (top != expected) {
            throw new IllegalStateException(
                "Scope stack corruption: expected to pop " +
                expected.projectId() + " but top is " +
                top.projectId() +
                ".  Close scope handles in LIFO order.");
        }
        stack.pop();
        if (stack.isEmpty()) {
            handleStack.remove();
        }
    }

    /**
     * Return the {@link ScopeContext} active on the current thread,
     * or {@code null} if no scope is active.
     */
    public static @Nullable ScopeContext current() {
        ProjectHandle top = handleStack.get().peek();
        return top != null ? top.context() : null;
    }

    // ── scope propagation helpers ────────────────────────────────────

    /**
     * Wrap a {@link Runnable} so that when executed on any thread,
     * the current thread's project scope is active.
     *
     * <p>If no scope is active on the calling thread, the task is
     * returned unchanged.  This is the recommended way to propagate
     * scope across thread boundaries (e.g. when submitting work to
     * an executor or scheduling on the FX platform thread).
     *
     * <p>Example:
     * <pre>
     * Runnable scoped = ProjectScope.propagateScope(() -&gt; {
     *     // @ProjectScoped dependencies are available here
     * });
     * Platform.runLater(scoped);
     * </pre>
     *
     * @param task the task to wrap
     * @return a scope-propagating wrapper, or {@code task} itself
     *         if no scope is active
     */
    public static @NotNull Runnable propagateScope(@NotNull Runnable task) {
        ScopeContext ctx = current();
        if (ctx == null) return task;
        ProjectId id = ctx.projectId();
        return () -> {
            try (var h = enter(id, ctx)) {
                task.run();
            }
        };
    }

    /**
     * Wrap a {@link Supplier} so that when executed on any thread,
     * the current thread's project scope is active.
     *
     * <p>If no scope is active on the calling thread, the supplier
     * is returned unchanged.
     *
     * @param <T>  the return type
     * @param task the supplier to wrap
     * @return a scope-propagating wrapper, or {@code task} itself
     *         if no scope is active
     */
    public static <T> @NotNull Supplier<T> propagateScope(@NotNull Supplier<T> task) {
        ScopeContext ctx = current();
        if (ctx == null) return task;
        ProjectId id = ctx.projectId();
        return () -> {
            try (var h = enter(id, ctx)) {
                return task.get();
            }
        };
    }

    /**
     * Capture the current thread's active scope context for
     * propagation to another thread.
     *
     * <p>Returns a {@code Supplier<ProjectHandle>} that, when
     * called on the target thread, pushes the captured scope
     * and returns a handle that must be closed (ideally via
     * try-with-resources).
     *
     * <p>Returns {@code null} if no scope is currently active.
     *
     * <p>Example:
     * <pre>
     * var scope = ProjectScope.captureScope();
     * executor.submit(() -&gt; {
     *     if (scope != null) {
     *         try (var h = scope.get()) {
     *             // scope is active here
     *         }
     *     }
     * });
     * </pre>
     *
     * @return a supplier of handles for the captured scope,
     *         or {@code null} if no scope is active
     */
    public static @Nullable Supplier<ProjectHandle> captureScope() {
        ScopeContext ctx = current();
        if (ctx == null) return null;
        ProjectId id = ctx.projectId();
        return () -> enter(id, ctx);
    }

    // ── testing ──────────────────────────────────────────────────────

    /** Remove all thread-local state.  <b>Test-only.</b> */
    static void resetThreadLocal() {
        handleStack.remove();
    }
}
