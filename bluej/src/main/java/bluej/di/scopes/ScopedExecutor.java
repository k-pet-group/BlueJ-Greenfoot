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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Executor that automatically propagates the submitting thread's
 * project scope to worker threads.
 *
 * <p>When a task is submitted, the current thread's active
 * {@link ProjectScope} (if any) is captured.  When the task runs
 * on a worker thread, the captured scope is pushed onto that
 * thread's scope stack and popped on completion.
 *
 * <p>If no scope is active at submission time, the task runs
 * without a scope (no overhead).
 *
 * <p>Example:
 * <pre>
 * ScopedExecutor exec = new ScopedExecutor(Executors.newFixedThreadPool(4));
 * // inside a project scope:
 * exec.execute(() -&gt; {
 *     MyService svc = BlueJInjector.getInstance(MyService.class);
 *     svc.doWork();  // scope is active here
 * });
 * </pre>
 *
 * @see ProjectScope#propagateScope(Runnable)
 * @see ProjectHandle
 */
public class ScopedExecutor {

    private final ExecutorService delegate;

    /**
     * Creates a new ScopedExecutor wrapping the given ExecutorService.
     *
     * @param delegate the underlying executor service
     */
    public ScopedExecutor(@NotNull ExecutorService delegate) {
        this.delegate = delegate;
    }

    /**
     * Execute a task, propagating the current thread's project scope.
     *
     * @param task the task to execute
     */
    public void execute(@NotNull Runnable task) {
        delegate.execute(ProjectScope.propagateScope(task));
    }

    /**
     * Submit a callable, propagating the current thread's project scope.
     *
     * @param task the callable to submit
     * @param <T>  the return type
     * @return a Future representing the pending result
     */
    public <T> @NotNull Future<T> submit(@NotNull Callable<T> task) {
        // No propagateScope(Callable) overload — propagate manually
        var scopeSupplier = ProjectScope.captureScope();
        return delegate.submit(() -> {
            if (scopeSupplier != null) {
                try (var handle = scopeSupplier.get()) {
                    return task.call();
                }
            }
            return task.call();
        });
    }

    /** Shut down the underlying executor service. */
    public void shutdown() {
        delegate.shutdown();
    }

    /** Return the underlying executor service. */
    public @NotNull ExecutorService getDelegate() {
        return delegate;
    }
}
