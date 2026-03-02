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
package bluej.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * Global injector singleton for the BlueJ application.
 *
 * <p>This class provides centralised access to the Guice injector.
 * It must be initialised at application startup before any projects
 * are opened.
 *
 * <p>Usage:
 * <pre>
 * // At application startup (Main.java):
 * BlueJInjector.initialize(new ApplicationModule());
 *
 * // To get instances (must be inside a project scope):
 * MyService service = BlueJInjector.getInstance(MyService.class);
 * </pre>
 *
 * // TODO: re-add reference to Project.withScope() once introduced
 * <p>Scope management is handled by
 * {@link bluej.di.scopes.ProjectHandle} — there is no need to call
 * scope-related methods on this class.
 *
 * @see bluej.di.scopes.ProjectHandle
 */
public final class BlueJInjector {

    private static final Logger log = Logger.getLogger(BlueJInjector.class.getName());
    private static volatile Injector injector;
    private static final Object lock = new Object();
    private static final List<Runnable> resetListeners =
        new CopyOnWriteArrayList<>();

    private BlueJInjector() { /* utility class */ }

    // ── lifecycle ─────────────────────────────────────────────────────

    /**
     * Initialise the global Guice injector with the given modules.
     *
     * <p>Must be called once at application startup, before any
     * DI-dependent code is executed.  The method is idempotent.
     *
     * @param modules the Guice modules to install
     * @return the initialised injector
     */
    public static @NotNull Injector initialize(@NotNull Module... modules) {
        if (injector == null) {
            synchronized (lock) {
                if (injector == null) {
                    injector = Guice.createInjector(
                        Stage.PRODUCTION,
                        modules
                    );
                }
            }
        } else {
            log.warning("BlueJInjector.initialize() called after injector " +
                "was already created — ignoring the supplied modules. " +
                "This is harmless during normal startup but may indicate " +
                "a configuration error in tests.");
        }
        return injector;
    }

    /**
     * Returns the global injector.
     *
     * @return the Guice injector
     * @throws IllegalStateException if not yet initialised
     */
    public static @NotNull Injector getInjector() {
        if (injector == null) {
            throw new IllegalStateException(
                "BlueJInjector has not been initialized. " +
                "Call BlueJInjector.initialize() at application startup.");
        }
        return injector;
    }

    // ── convenience ───────────────────────────────────────────────────

    /**
     * Convenience method to get an instance from the injector.
     *
     * @param type the class to get an instance of
     * @param <T>  the type
     * @return an instance of the requested type
     * @throws IllegalStateException if not yet initialised
     */
    public static <T> @NotNull T getInstance(@NotNull Class<T> type) {
        return getInjector().getInstance(type);
    }

    /**
     * Inject dependencies into an existing instance.
     * Use for objects not created by Guice that need
     * their {@code @Inject} fields populated.
     *
     * @param instance the instance to inject into
     */
    public static void injectMembers(@NotNull Object instance) {
        getInjector().injectMembers(instance);
    }

    // ── query ─────────────────────────────────────────────────────────

    /**
     * Checks whether the injector has been initialised.
     *
     * @return {@code true} if initialised
     */
    public static boolean isInitialized() {
        return injector != null;
    }

    // ── testing ───────────────────────────────────────────────────────

    /**
     * Register a listener that is called when the injector is
     * reset for testing.  Used to clear caches that hold
     * references to injector-managed instances (e.g.
     * {@code Project.cachedFactory}).
     *
     * @param listener the listener to register
     */
    public static void onReset(@NotNull Runnable listener) {
        resetListeners.add(listener);
    }

    /**
     * Reset the injector for testing.
     *
     * <p><strong>Warning:</strong> not thread-safe — use only in
     * single-threaded test set-up / tear-down.
     *
     * <p>Package-private — test code should use
     * {@code BlueJInjectorTestUtils.reset()} instead.
     */
    @TestOnly
    static void resetForTesting() {
        synchronized (lock) {
            injector = null;
        }
        resetListeners.forEach(Runnable::run);
    }
}
