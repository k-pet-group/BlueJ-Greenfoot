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

/**
 * A thread-bound token representing an active project scope.
 *
 * <p>{@code ProjectHandle} is created and pushed onto the
 * thread-local scope stack by the project-opening code
 * (or test utilities).  The only public operation is
 * {@link #close()}, which pops the stack.
 *
 * // TODO: re-add {@link} to ProjectFactory and Project.withScope() once introduced
 * <p>Callers should never need to create handles directly — use
 * {@link ProjectScope#enter} (or the project-lifecycle wrapper)
 * instead.
 *
 * @see ProjectScope
 * @see ProjectScoped
 */
public final class ProjectHandle implements AutoCloseable {

    private final ProjectId projectId;
    private final ProjectScope.ScopeContext context;
    private final Thread ownerThread;

    /**
     * Package-private constructor — only {@link ProjectScope#enter}
     * should create handles.
     */
    ProjectHandle(@NotNull ProjectId projectId, @NotNull ProjectScope.ScopeContext context) {
        this.projectId = projectId;
        this.context = context;
        this.ownerThread = Thread.currentThread();
    }

    /** The identity of the project this handle belongs to. */
    public @NotNull ProjectId projectId() {
        return projectId;
    }

    /** The scope context this handle activates. */
    @NotNull ProjectScope.ScopeContext context() {
        return context;
    }

    /**
     * Pop this handle from the current thread's scope stack.
     *
     * <p>Verifies that {@code close()} is called on the same thread
     * that created the handle.  Uses {@link ProjectScope#guardedPop}
     * to verify that <em>this</em> handle is the topmost entry.
     *
     * <p>If the stack is already empty the call is a safe no-op.
     *
     * @throws IllegalStateException if called from a different thread
     *         than the one that entered the scope
     */
    @Override
    public void close() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                "ProjectHandle for " + projectId +
                " was closed on a different thread than it was" +
                " entered on. Scope handles are thread-bound —" +
                " close on the same thread that called enter().");
        }
        ProjectScope.guardedPop(this);
    }
}
