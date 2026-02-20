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
package bluej.views;

import bluej.di.scopes.ProjectScoped;
import bluej.parser.context.CompilationUnitContextLoader;
import bluej.utility.javafx.JavaFXUtil;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-project factory and cache for {@link View} instances.
 *
 * <p>Each {@code Project} owns exactly one {@code ViewFactory},
 * injected by Guice as a {@code @ProjectScoped} dependency.
 * Views are created lazily on first access and cached by class.
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} for the cache.
 *
 * @see View
 */
@ProjectScoped
@OnThread(Tag.Any)
public final class ViewFactory {

    private final @NotNull CompilationUnitContextLoader contextLoader;
    private final ConcurrentHashMap<Class<?>, View> cache =
        new ConcurrentHashMap<>();

    /**
     * Creates a new ViewFactory.
     *
     * <p>Injected by Guice within the project scope.  The
     * {@link CompilationUnitContextLoader} is resolved from the
     * same scope.
     *
     * @param contextLoader the project's context loader
     */
    @Inject
    public ViewFactory(@NotNull CompilationUnitContextLoader contextLoader) {
        this.contextLoader = contextLoader;
    }

    /**
     * Return a {@link View} for the given class, creating and
     * caching it if necessary.
     *
     * @param cl the class to get a view for
     * @return the View, or {@code null} if {@code cl} is null
     */
    public @Nullable View getView(@Nullable Class<?> cl) {
        if (cl == null) {
            return null;
        }
        return cache.computeIfAbsent(cl, this::createView);
    }

    /**
     * Remove from the cache all views whose classes were loaded
     * by the given class loader.
     *
     * @param loader the class loader whose views to evict
     */
    public void removeAll(@NotNull ClassLoader loader) {
        cache.entrySet().removeIf(
            e -> e.getKey().getClassLoader() == loader);
    }

    /** Clear the entire cache. */
    public void clear() {
        cache.clear();
    }

    // ── internal ─────────────────────────────────────────────────────

    /**
     * Create a new {@link View} on the FX platform thread.
     *
     * <p>Uses {@link JavaFXUtil#runPlatformAndWait} which executes
     * synchronously when already on the FX thread, and blocks
     * otherwise.
     *
     * <p><b>Deadlock warning:</b> Do not call {@link #getView}
     * from a background thread that the FX thread is waiting on
     * (e.g. via {@code Future.get()} or {@code Thread.join()}).
     * This is the same constraint as the old static
     * {@code View.getView()}.
     */
    private @NotNull View createView(@NotNull Class<?> cl) {
        return JavaFXUtil.runPlatformAndWait(() -> new View(cl, contextLoader, this));
    }
}
