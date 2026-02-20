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
package bluej.utility.javafx.threading;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import bluej.di.scopes.ProjectScope;
import bluej.utility.Debug;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Static utility methods for cross-thread execution on the JavaFX platform
 * thread, organised in three tiers:
 *
 * <ol>
 *   <li>{@code runPlatformLater} — always defers via {@link Platform#runLater},
 *       returns a {@link Future}.</li>
 *   <li>{@code runPlatform} — executes synchronously when already on the FX
 *       thread, otherwise delegates to {@code runPlatformLater}.</li>
 *   <li>{@code runPlatformAndWait} — blocks on {@code runPlatform().get()}.</li>
 * </ol>
 *
 * <p>Each tier provides overloads for all six functional-interface shapes:
 * {@link FXPlatformRunnableThrowing Runnable},
 * {@link FXPlatformSupplierThrowing Supplier},
 * {@link FXPlatformConsumerThrowing Consumer},
 * {@link FXPlatformFunctionThrowing Function},
 * {@link FXPlatformBiConsumerThrowing BiConsumer}, and
 * {@link FXPlatformBiFunctionThrowing BiFunction}.</p>
 *
 * <p>Also includes {@link #unwrapCause(Throwable, Class)} for walking
 * exception cause chains and {@link #runAfterCurrent(FXPlatformRunnable)}
 * for deferring work on the FX platform thread.</p>
 *
 * @see bluej.utility.javafx.JavaFXUtil
 */
public class JavaFXThreadingUtil
{
    private JavaFXThreadingUtil() {} // non-instantiable

    /**
     * A method which runs the given action on the platform thread when it
     * later becomes available.  A specific way to call Platform.runLater
     * from the platform thread when you really mean to (thread checker will warn
     * you otherwise).
     */
    @OnThread(Tag.FXPlatform)
    public static void runAfterCurrent(FXPlatformRunnable r)
    {
        // Defeat thread checker:
        ((FXPlatformConsumer<Runnable>)(Platform::runLater)).accept(r::run);
    }

    // ========================================================================
    // Exception helpers
    // ========================================================================

    /**
     * Walk a {@link Throwable} cause chain and return the first cause
     * matching the requested type.
     *
     * @param throwable the root throwable to inspect
     * @param type      the cause type to find
     * @param <E>       cause type
     * @return matching cause, or {@code null} if absent
     */
    @OnThread(Tag.Any)
    public static <E extends Throwable> @Nullable E unwrapCause(@NotNull Throwable throwable, @NotNull Class<E> type)
    {
        Throwable cause = throwable;
        while (cause != null)
        {
            if (type.isInstance(cause))
            {
                return type.cast(cause);
            }
            cause = cause.getCause();
        }
        return null;
    }

    // ========================================================================
    // Cross-thread execution:
    //   runPlatformLater (always async) → runPlatform (smart) → runPlatformAndWait (blocking)
    //
    // All runPlatformLater overloads automatically propagate the calling
    // thread's project scope (if any) to the FX platform thread via
    // ProjectScope.captureScope().  This ensures that @ProjectScoped
    // dependencies are available in FX callbacks without manual wiring.
    //
    // Implementation note: the pattern
    //   try (var h = capture != null ? capture.get() : null) { ... }
    // relies on Java's try-with-resources accepting a null AutoCloseable
    // (JLS §14.20.3) — close() is simply not called when the resource
    // is null.  This avoids an extra if/else branch in every overload.
    // ========================================================================

    /**
     * Schedules the given task for execution on the JavaFX platform thread
     * via {@link Platform#runLater} and returns a {@link Future} representing
     * its completion.
     *
     * <p>The task is <em>always</em> deferred — even if called from the FX
     * platform thread, it will execute on the next pulse of the JavaFX event
     * loop.  If you want synchronous execution when already on the FX thread,
     * use {@link #runPlatform(FXPlatformRunnableThrowing)} instead.</p>
     *
     * <p>Any exception thrown by the task is captured in the returned future
     * and also logged via {@link Debug#reportError}.</p>
     *
     * @param task the task to execute on the FX platform thread
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatform(FXPlatformRunnableThrowing)
     * @see #runPlatformAndWait(FXPlatformRunnableThrowing)
     */
    @OnThread(Tag.Any)
    public static @NotNull Future<Void> runPlatformLater(@NotNull FXPlatformRunnableThrowing task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                task.run();
                future.complete(null);
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * Schedules the given supplier for execution on the JavaFX platform thread
     * via {@link Platform#runLater} and returns a {@link Future} representing
     * its result.
     *
     * <p>The supplier is <em>always</em> deferred.  If you want synchronous
     * execution when already on the FX thread, use
     * {@link #runPlatform(FXPlatformSupplierThrowing)} instead.</p>
     *
     * <p>Any exception thrown by the supplier is captured in the returned
     * future and also logged via {@link Debug#reportError}.</p>
     *
     * @param <T>  the return type of the supplier
     * @param task the supplier to execute on the FX platform thread
     * @return a {@link Future}{@code <T>} representing the supplier's result
     * @see #runPlatform(FXPlatformSupplierThrowing)
     * @see #runPlatformAndWait(FXPlatformSupplierThrowing)
     */
    @OnThread(Tag.Any)
    public static <T> @NotNull Future<T> runPlatformLater(@NotNull FXPlatformSupplierThrowing<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                future.complete(task.get());
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * Schedules the given consumer for execution on the JavaFX platform thread
     * via {@link Platform#runLater} with the provided argument, and returns a
     * {@link Future} representing completion.
     *
     * <p>The consumer is <em>always</em> deferred.  If you want synchronous
     * execution when already on the FX thread, use
     * {@link #runPlatform(FXPlatformConsumerThrowing, Object)} instead.</p>
     *
     * <p>Any exception thrown by the consumer is captured in the returned
     * future and also logged via {@link Debug#reportError}.</p>
     *
     * @param <T>  the type of the argument
     * @param task the consumer to execute on the FX platform thread
     * @param arg  the argument to pass to the consumer
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatform(FXPlatformConsumerThrowing, Object)
     * @see #runPlatformAndWait(FXPlatformConsumerThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T> @NotNull Future<Void> runPlatformLater(@NotNull FXPlatformConsumerThrowing<T> task, T arg) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                task.accept(arg);
                future.complete(null);
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * Schedules the given function for execution on the JavaFX platform thread
     * via {@link Platform#runLater} with the provided argument, and returns a
     * {@link Future} representing the result.
     *
     * <p>The function is <em>always</em> deferred.  If you want synchronous
     * execution when already on the FX thread, use
     * {@link #runPlatform(FXPlatformFunctionThrowing, Object)} instead.</p>
     *
     * <p>Any exception thrown by the function is captured in the returned
     * future and also logged via {@link Debug#reportError}.</p>
     *
     * @param <T>  the type of the argument
     * @param <R>  the return type of the function
     * @param task the function to execute on the FX platform thread
     * @param arg  the argument to pass to the function
     * @return a {@link Future}{@code <R>} representing the function's result
     * @see #runPlatform(FXPlatformFunctionThrowing, Object)
     * @see #runPlatformAndWait(FXPlatformFunctionThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T, R> @NotNull Future<R> runPlatformLater(@NotNull FXPlatformFunctionThrowing<T, R> task, T arg) {
        CompletableFuture<R> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                future.complete(task.apply(arg));
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * Schedules the given bi-consumer for execution on the JavaFX platform
     * thread via {@link Platform#runLater} with the provided arguments, and
     * returns a {@link Future} representing completion.
     *
     * <p>The bi-consumer is <em>always</em> deferred.  If you want
     * synchronous execution when already on the FX thread, use
     * {@link #runPlatform(FXPlatformBiConsumerThrowing, Object, Object)}
     * instead.</p>
     *
     * <p>Any exception thrown by the bi-consumer is captured in the returned
     * future and also logged via {@link Debug#reportError}.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param task the bi-consumer to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-consumer
     * @param arg2 the second argument to pass to the bi-consumer
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatform(FXPlatformBiConsumerThrowing, Object, Object)
     * @see #runPlatformAndWait(FXPlatformBiConsumerThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U> @NotNull Future<Void> runPlatformLater(@NotNull FXPlatformBiConsumerThrowing<T, U> task, T arg1, U arg2) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                task.accept(arg1, arg2);
                future.complete(null);
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * Schedules the given bi-function for execution on the JavaFX platform
     * thread via {@link Platform#runLater} with the provided arguments, and
     * returns a {@link Future} representing the result.
     *
     * <p>The bi-function is <em>always</em> deferred.  If you want
     * synchronous execution when already on the FX thread, use
     * {@link #runPlatform(FXPlatformBiFunctionThrowing, Object, Object)}
     * instead.</p>
     *
     * <p>Any exception thrown by the bi-function is captured in the returned
     * future and also logged via {@link Debug#reportError}.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param <R>  the return type of the bi-function
     * @param task the bi-function to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-function
     * @param arg2 the second argument to pass to the bi-function
     * @return a {@link Future}{@code <R>} representing the bi-function's result
     * @see #runPlatform(FXPlatformBiFunctionThrowing, Object, Object)
     * @see #runPlatformAndWait(FXPlatformBiFunctionThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U, R> @NotNull Future<R> runPlatformLater(@NotNull FXPlatformBiFunctionThrowing<T, U, R> task, T arg1, U arg2) {
        CompletableFuture<R> future = new CompletableFuture<>();
        var scopeCapture = ProjectScope.captureScope();
        Platform.runLater(() -> {
            try (var scopeHandle = scopeCapture != null ? scopeCapture.get() : null) {
                future.complete(task.apply(arg1, arg2));
            } catch (Throwable ex) {
                Debug.reportError("Exception in runPlatformLater task", ex);
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    // ========================================================================
    // runPlatformAndWait — blocking wrappers
    // ========================================================================

    /**
     * Executes the given task on the JavaFX platform thread and blocks the
     * calling thread until the task completes.
     *
     * <p>Delegates to {@link #runPlatform(FXPlatformRunnableThrowing)} and
     * blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the task is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param r the task to execute on the FX platform thread
     * @throws RuntimeException wrapping any exception thrown by the task
     * @see #runPlatformLater(FXPlatformRunnableThrowing)
     * @see #runPlatform(FXPlatformRunnableThrowing)
     */
    @OnThread(Tag.Any)
    public static void runPlatformAndWait(@NotNull FXPlatformRunnableThrowing r)
    {
        try {
            runPlatform(r).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    /**
     * Executes the given supplier on the JavaFX platform thread, blocks the
     * calling thread until it completes, and returns the result.
     *
     * <p>Delegates to {@link #runPlatform(FXPlatformSupplierThrowing)} and
     * blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the supplier is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param <T>  the return type of the supplier
     * @param task the supplier to execute on the FX platform thread
     * @return the value produced by the supplier
     * @throws RuntimeException wrapping any exception thrown by the supplier
     * @see #runPlatformLater(FXPlatformSupplierThrowing)
     * @see #runPlatform(FXPlatformSupplierThrowing)
     */
    @OnThread(Tag.Any)
    public static <T> T runPlatformAndWait(@NotNull FXPlatformSupplierThrowing<T> task)
    {
        try {
            return runPlatform(task).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    /**
     * Executes the given consumer on the JavaFX platform thread with the
     * provided argument, and blocks the calling thread until completion.
     *
     * <p>Delegates to {@link #runPlatform(FXPlatformConsumerThrowing, Object)}
     * and blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the consumer is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param <T>  the type of the argument
     * @param task the consumer to execute on the FX platform thread
     * @param arg  the argument to pass to the consumer
     * @throws RuntimeException wrapping any exception thrown by the consumer
     * @see #runPlatformLater(FXPlatformConsumerThrowing, Object)
     * @see #runPlatform(FXPlatformConsumerThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T> void runPlatformAndWait(@NotNull FXPlatformConsumerThrowing<T> task, T arg)
    {
        try {
            runPlatform(task, arg).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    /**
     * Executes the given function on the JavaFX platform thread with the
     * provided argument, blocks the calling thread until completion, and
     * returns the result.
     *
     * <p>Delegates to {@link #runPlatform(FXPlatformFunctionThrowing, Object)}
     * and blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the function is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param <T>  the type of the argument
     * @param <R>  the return type of the function
     * @param task the function to execute on the FX platform thread
     * @param arg  the argument to pass to the function
     * @return the value produced by the function
     * @throws RuntimeException wrapping any exception thrown by the function
     * @see #runPlatformLater(FXPlatformFunctionThrowing, Object)
     * @see #runPlatform(FXPlatformFunctionThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T, R> R runPlatformAndWait(@NotNull FXPlatformFunctionThrowing<T, R> task, T arg)
    {
        try {
            return runPlatform(task, arg).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    /**
     * Executes the given bi-consumer on the JavaFX platform thread with the
     * provided arguments, and blocks the calling thread until completion.
     *
     * <p>Delegates to
     * {@link #runPlatform(FXPlatformBiConsumerThrowing, Object, Object)}
     * and blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the bi-consumer is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param task the bi-consumer to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-consumer
     * @param arg2 the second argument to pass to the bi-consumer
     * @throws RuntimeException wrapping any exception thrown by the bi-consumer
     * @see #runPlatformLater(FXPlatformBiConsumerThrowing, Object, Object)
     * @see #runPlatform(FXPlatformBiConsumerThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U> void runPlatformAndWait(@NotNull FXPlatformBiConsumerThrowing<T, U> task, T arg1, U arg2)
    {
        try {
            runPlatform(task, arg1, arg2).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    /**
     * Executes the given bi-function on the JavaFX platform thread with the
     * provided arguments, blocks the calling thread until completion, and
     * returns the result.
     *
     * <p>Delegates to
     * {@link #runPlatform(FXPlatformBiFunctionThrowing, Object, Object)}
     * and blocks on the resulting {@link Future}.</p>
     *
     * <p>Any checked exception thrown by the bi-function is wrapped in a
     * {@link RuntimeException}.</p>
     *
     * <p><strong>Warning:</strong> Do not call this from a thread that the FX
     * platform thread is waiting on, as this would cause a deadlock.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param <R>  the return type of the bi-function
     * @param task the bi-function to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-function
     * @param arg2 the second argument to pass to the bi-function
     * @return the value produced by the bi-function
     * @throws RuntimeException wrapping any exception thrown by the bi-function
     * @see #runPlatformLater(FXPlatformBiFunctionThrowing, Object, Object)
     * @see #runPlatform(FXPlatformBiFunctionThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U, R> R runPlatformAndWait(@NotNull FXPlatformBiFunctionThrowing<T, U, R> task, T arg1, U arg2)
    {
        try {
            return runPlatform(task, arg1, arg2).get();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while running task on FX thread", e);
        }
    }

    // ========================================================================
    // runPlatform — sync-if-on-FX, async otherwise
    // ========================================================================

    /**
     * Schedules the given task for execution on the JavaFX platform thread and
     * returns a {@link Future} representing its completion.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the task
     * is executed synchronously and the returned future is already completed.
     * Otherwise the task is deferred via
     * {@link #runPlatformLater(FXPlatformRunnableThrowing)}.</p>
     *
     * <p>Any exception thrown by the task is captured in the returned future
     * (accessible via {@link Future#get()}).</p>
     *
     * @param task the task to execute on the FX platform thread
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatformLater(FXPlatformRunnableThrowing)
     * @see #runPlatformAndWait(FXPlatformRunnableThrowing)
     */
    @OnThread(Tag.Any)
    public static @NotNull Future<Void> runPlatform(@NotNull FXPlatformRunnableThrowing task) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                ((RunnableThrowing) task::run).run();
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task);
        }
    }

    /**
     * Schedules the given supplier for execution on the JavaFX platform thread
     * and returns a {@link Future} representing its result.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the
     * supplier is evaluated synchronously and the returned future is already
     * completed with the result. Otherwise the supplier is deferred via
     * {@link #runPlatformLater(FXPlatformSupplierThrowing)}.</p>
     *
     * <p>Any exception thrown by the supplier is captured in the returned
     * future.</p>
     *
     * @param <T>  the return type of the supplier
     * @param task the supplier to execute on the FX platform thread
     * @return a {@link Future}{@code <T>} representing the supplier's result
     * @see #runPlatformLater(FXPlatformSupplierThrowing)
     * @see #runPlatformAndWait(FXPlatformSupplierThrowing)
     */
    @OnThread(Tag.Any)
    public static <T> @NotNull Future<T> runPlatform(@NotNull FXPlatformSupplierThrowing<T> task) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                var value = ((SupplierThrowing<T>) task::get).get();
                return CompletableFuture.completedFuture(value);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task);
        }
    }

    /**
     * Schedules the given consumer for execution on the JavaFX platform thread
     * with the provided argument, and returns a {@link Future} representing
     * completion.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the
     * consumer is invoked synchronously and the returned future is already
     * completed. Otherwise the consumer is deferred via
     * {@link #runPlatformLater(FXPlatformConsumerThrowing, Object)}.</p>
     *
     * <p>Any exception thrown by the consumer is captured in the returned
     * future.</p>
     *
     * @param <T>  the type of the argument
     * @param task the consumer to execute on the FX platform thread
     * @param arg  the argument to pass to the consumer
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatformLater(FXPlatformConsumerThrowing, Object)
     * @see #runPlatformAndWait(FXPlatformConsumerThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T> @NotNull Future<Void> runPlatform(@NotNull FXPlatformConsumerThrowing<T> task, T arg) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                ((ConsumerThrowing<T>) task::accept).accept(arg);
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task, arg);
        }
    }

    /**
     * Schedules the given function for execution on the JavaFX platform thread
     * with the provided argument, and returns a {@link Future} representing
     * the result.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the
     * function is evaluated synchronously and the returned future is already
     * completed. Otherwise the function is deferred via
     * {@link #runPlatformLater(FXPlatformFunctionThrowing, Object)}.</p>
     *
     * <p>Any exception thrown by the function is captured in the returned
     * future.</p>
     *
     * @param <T>  the type of the argument
     * @param <R>  the return type of the function
     * @param task the function to execute on the FX platform thread
     * @param arg  the argument to pass to the function
     * @return a {@link Future}{@code <R>} representing the function's result
     * @see #runPlatformLater(FXPlatformFunctionThrowing, Object)
     * @see #runPlatformAndWait(FXPlatformFunctionThrowing, Object)
     */
    @OnThread(Tag.Any)
    public static <T, R> @NotNull Future<R> runPlatform(@NotNull FXPlatformFunctionThrowing<T, R> task, T arg) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                var value = ((FunctionThrowing<T, R>) task::apply).apply(arg);
                return CompletableFuture.completedFuture(value);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task, arg);
        }
    }

    /**
     * Schedules the given bi-consumer for execution on the JavaFX platform
     * thread with the provided arguments, and returns a {@link Future}
     * representing completion.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the
     * bi-consumer is invoked synchronously and the returned future is already
     * completed. Otherwise the bi-consumer is deferred via
     * {@link #runPlatformLater(FXPlatformBiConsumerThrowing, Object, Object)}.</p>
     *
     * <p>Any exception thrown by the bi-consumer is captured in the returned
     * future.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param task the bi-consumer to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-consumer
     * @param arg2 the second argument to pass to the bi-consumer
     * @return a {@link Future}{@code <Void>} representing the task completion
     * @see #runPlatformLater(FXPlatformBiConsumerThrowing, Object, Object)
     * @see #runPlatformAndWait(FXPlatformBiConsumerThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U> @NotNull Future<Void> runPlatform(@NotNull FXPlatformBiConsumerThrowing<T, U> task, T arg1, U arg2) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                ((BiConsumerThrowing<T, U>) task::accept).accept(arg1, arg2);
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task, arg1, arg2);
        }
    }

    /**
     * Schedules the given bi-function for execution on the JavaFX platform
     * thread with the provided arguments, and returns a {@link Future}
     * representing the result.
     *
     * <p>If the calling thread <em>is</em> the FX application thread, the
     * bi-function is evaluated synchronously and the returned future is
     * already completed. Otherwise the bi-function is deferred via
     * {@link #runPlatformLater(FXPlatformBiFunctionThrowing, Object, Object)}.</p>
     *
     * <p>Any exception thrown by the bi-function is captured in the returned
     * future.</p>
     *
     * @param <T>  the type of the first argument
     * @param <U>  the type of the second argument
     * @param <R>  the return type of the bi-function
     * @param task the bi-function to execute on the FX platform thread
     * @param arg1 the first argument to pass to the bi-function
     * @param arg2 the second argument to pass to the bi-function
     * @return a {@link Future}{@code <R>} representing the bi-function's result
     * @see #runPlatformLater(FXPlatformBiFunctionThrowing, Object, Object)
     * @see #runPlatformAndWait(FXPlatformBiFunctionThrowing, Object, Object)
     */
    @OnThread(Tag.Any)
    public static <T, U, R> @NotNull Future<R> runPlatform(@NotNull FXPlatformBiFunctionThrowing<T, U, R> task, T arg1, U arg2) {
        if (Platform.isFxApplicationThread()) {
            try {
                // Cast to suppress threadchecker, as we are sure we're on the right thread
                var value = ((BiFunctionThrowing<T, U, R>) task::apply).apply(arg1, arg2);
                return CompletableFuture.completedFuture(value);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        } else {
            return runPlatformLater(task, arg1, arg2);
        }
    }
}
