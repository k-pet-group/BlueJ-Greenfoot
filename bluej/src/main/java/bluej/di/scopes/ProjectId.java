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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Value type representing the unique identity of a BlueJ project.
 *
 * <p>Wraps the <b>canonical</b> path of the project directory so
 * that symlinks and {@code ..} components are resolved.  Using a
 * dedicated type instead of raw {@code String} provides type safety
 * and a single place to change the identity strategy.
 *
 * @param path the canonical path of the project directory
 */
public record ProjectId(@NotNull String path) {

    /**
     * Compact constructor — validates that path is non-blank.
     */
    public ProjectId {
        if (path.isBlank()) {
            throw new IllegalArgumentException("Project path cannot be blank");
        }
    }

    /**
     * Creates a {@code ProjectId} from a project directory.
     *
     * <p>Uses {@link File#getCanonicalPath()} to resolve symlinks
     * and relative path components, ensuring that two {@code File}
     * objects pointing at the same physical directory produce the
     * same {@code ProjectId}.
     *
     * @param projectDir the project directory
     * @return a new ProjectId
     * @throws UncheckedIOException if the canonical path cannot be resolved
     */
    public static @NotNull ProjectId of(@NotNull File projectDir) {
        try {
            return new ProjectId(projectDir.getCanonicalPath());
        }
        catch (IOException e) {
            throw new UncheckedIOException(
                "Cannot resolve canonical path for: " + projectDir, e);
        }
    }

    @Override
    public String toString() {
        return "ProjectId[" + path + "]";
    }
}
