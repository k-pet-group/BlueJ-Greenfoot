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
package bluej.pkgmgr;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.di.BlueJInjector;
import bluej.di.scopes.ProjectId;
import bluej.di.scopes.ProjectScope;
import bluej.extmgr.ExtensionsManager;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Application-scoped singleton that manages the lifecycle of
 * {@link Project} instances and their DI scopes.
 *
 * <p>This factory owns the registry of open projects and all
 * scope state.  Being in the same package as {@link Project},
 * it can access the package-private constructor and fields
 * directly.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Opening projects (path resolution, scope creation,
 *       construction, seeding, {@code initialPackageName})</li>
 *   <li>Closing projects (debugger shutdown, scope disposal,
 *       map removal)</li>
 *   <li>Creating new project directory structures</li>
 *   <li>Querying open projects</li>
 *   <li>Validating project paths</li>
 *   <li>Providing scope access for {@code Project.withScope()}</li>
 * </ul>
 *
 * @see ProjectScope
 * @see ProjectScope.ScopeContext
 */
@Singleton
// ignoreParent required: bluej.pkgmgr package defaults to FXPlatform,
// but ProjectFactory methods must be callable from any thread.
@OnThread(value = Tag.Any, ignoreParent = true)
public class ProjectFactory {

    /** Everything the factory tracks for a single open project. */
    @OnThread(value = Tag.Any, ignoreParent = true)
    private record ProjectEntry(
        Project project,
        ProjectScope.ScopeContext context
    ) {}

    /** Registry of open projects, keyed by {@link ProjectId}. */
    private final Map<ProjectId, ProjectEntry> entries = new ConcurrentHashMap<>();

    // ── open ──────────────────────────────────────────────────────────

    /**
     * Open a project from a path string.
     *
     * <p>This does <b>all</b> non-GUI work: path resolution, package
     * walking, scope creation, construction, seeding,
     * {@code initialPackageName}, analytics, and recent-projects.
     *
     * @param projectPath directory path or bluej.pkg file path
     * @return the opened Project, or {@code null} if the path is invalid
     * @throws IOException if project construction fails
     */
    public @Nullable Project open(@NotNull String projectPath) throws IOException {
        File startingDir;
        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        }
        catch (IOException ioe) {
            Debug.reportError(
                "could not resolve directory " + projectPath, ioe);
            return null;
        }

        if (startingDir == null || !Package.isPackage(startingDir)) {
            return null;
        }

        File projectDir = walkToProjectRoot(startingDir);
        Project proj = open(projectDir);

        // Compute starting package name from relative path
        String startingPackageName =
            computeStartingPackageName(projectDir, startingDir);

        if (startingPackageName.isEmpty()) {
            Package startingPackage = proj.getPackage("");
            while (startingPackage != null) {
                Package sub = startingPackage.getBoringSubPackage();
                if (sub == null) break;
                startingPackage = sub;
            }
            proj.initialPackageName = startingPackage.getQualifiedName();
        }
        else {
            proj.initialPackageName = startingPackageName;
        }

        // Post-open lifecycle — these APIs require the FX thread.
        JavaFXUtil.runPlatformLater(() -> {
            DataCollector.projectOpened(proj,
                ExtensionsManager.getInstance().getLoadedExtensions(proj));
            proj.getImportScanner().startScanning();
            PrefMgr.addRecentProject(proj.getProjectDir());
        });

        return proj;
    }

    /**
     * Open a project from its root directory.
     *
     * <p>If the project is already open, the existing instance is
     * returned.  Otherwise a new {@link Project} is constructed
     * inside a borrowed DI scope, seeded, and registered.
     *
     * <p>Synchronized to prevent two threads from concurrently
     * opening the same project (the check-then-construct-then-put
     * sequence is not atomic otherwise).  Project opening is not
     * a hot path, so coarse locking is acceptable here.
     *
     * @param projectDir the project root directory
     * @return the opened (or already-open) Project
     * @throws IOException if the Project constructor fails
     */
    public synchronized @NotNull Project open(@NotNull File projectDir) throws IOException {
        ProjectId id = ProjectId.of(projectDir);

        ProjectEntry existing = entries.get(id);
        if (existing != null) {
            return existing.project();
        }

        var context = new ProjectScope.ScopeContext(id);

        Project proj;
        try (var handle = ProjectScope.enter(id, context)) {
            // Project constructor requires the FX platform thread
            // (package default for bluej.pkgmgr).
            proj = JavaFXUtil.runPlatformAndWait(() -> new Project(projectDir));
            context.seed(Project.class, proj);
            // Inject @Inject fields (contextLoader, viewFactory, etc.)
            // while the scope is active on this thread.
            BlueJInjector.injectMembers(proj);
        }
        catch (Exception e) {
            context.clear();
            if (e instanceof IOException ioe) throw ioe;
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }

        entries.put(id, new ProjectEntry(proj, context));
        return proj;
    }

    // ── scope access ─────────────────────────────────────────────────

    /**
     * Execute an action within a project's DI scope.
     *
     * <p>Creates a {@link ProjectHandle} that pushes the scope onto
     * the current thread's stack for the duration of the callback.
     *
     * <p>Called by {@code Project.withScope()}.
     */
    void withScope(@NotNull Project project, @NotNull Runnable action) {
        ProjectEntry entry = requireEntry(project);
        try (var handle = ProjectScope.enter(
                entry.context().projectId(), entry.context())) {
            action.run();
        }
    }

    /**
     * Execute a supplier within a project's DI scope and return
     * the result.
     */
    <T> T withScope(@NotNull Project project, @NotNull Supplier<T> action) {
        ProjectEntry entry = requireEntry(project);
        try (var handle = ProjectScope.enter(
                entry.context().projectId(), entry.context())) {
            return action.get();
        }
    }

    private @NotNull ProjectEntry requireEntry(@NotNull Project project) {
        ProjectId id = ProjectId.of(project.getProjectDir());
        ProjectEntry entry = entries.get(id);
        if (entry == null) {
            throw new IllegalStateException(
                "No scope registered for project: " +
                project.getProjectDir());
        }
        return entry;
    }

    // ── close ─────────────────────────────────────────────────────────

    /**
     * Non-GUI cleanup for a project that is being closed.
     *
     * <p>Shuts down the debugger, clears scope values, and
     * removes the project from the registry.
     *
     * <p>GUI cleanup (terminal, status frame, exec controls,
     * inspectors) must be done by the caller <em>before</em>
     * calling this method.
     */
    public void close(@NotNull Project project) {
        project.getDebugger().removeDebuggerListener(project);
        project.getDebugger().close(false);

        ProjectId id = ProjectId.of(project.getProjectDir());
        ProjectEntry entry = entries.remove(id);
        if (entry != null) {
            entry.context().clear();
        }
    }

    // ── create ────────────────────────────────────────────────────────

    /**
     * Create a new project directory structure at the given path.
     *
     * <p>Does <b>not</b> open the project — call
     * {@link #open(String)} or {@link #open(File)} afterward.
     *
     * @param projectPath path for the new project directory
     * @return {@code true} if creation succeeded
     */
    public boolean createNew(@Nullable String projectPath) {
        if (projectPath == null) {
            return false;
        }

        File dir = new File(projectPath);

        if (dir.exists() && (!dir.isDirectory() || dir.list().length > 0)) {
            return false;
        }

        if (dir.exists() || dir.mkdir()) {
            File newreadmeFile = new File(dir, Package.readmeName);

            PackageFile pkgFile = PackageFileFactory.getPackageFile(dir);
            try {
                if (pkgFile.create()) {
                    Properties props = new Properties();
                    if (Config.isGreenfoot()) {
                        props.put("mainWindow.width", "850");
                        props.put("mainWindow.height", "600");
                        props.put("mainWindow.x", "40");
                        props.put("mainWindow.y", "40");
                    }
                    props.put("project.charset", "UTF-8");
                    try {
                        pkgFile.save(props);
                        FileUtility.copyFile(
                            Config.getTemplateFile("readme"),
                            newreadmeFile);
                        return true;
                    }
                    catch (IOException ioe) {
                        Debug.message("I/O error while creating project: "
                            + ioe.getMessage());
                    }
                }
            }
            catch (IOException ioe) {
                // Failed to create package file
            }
        }

        Debug.message("Unable to create project directory: " + projectPath);
        return false;
    }

    // ── queries ───────────────────────────────────────────────────────

    /** Returns the number of currently open projects. */
    public int openCount() {
        return entries.size();
    }

    /** Returns the currently open projects (unmodifiable view). */
    public @NotNull Collection<Project> openProjects() {
        return entries.values().stream()
            .map(ProjectEntry::project)
            .toList();
    }

    /**
     * Look up an open project by its directory.
     *
     * @param projectDir the project directory
     * @return the Project, or {@code null} if not open
     */
    public @Nullable Project get(@NotNull File projectDir) {
        ProjectEntry entry = entries.get(ProjectId.of(projectDir));
        return entry != null ? entry.project() : null;
    }

    // ── path resolution ────────────────────────────────────────────

    /**
     * Resolve a path string to the project root directory.
     *
     * <p>Handles both directory paths and {@code bluej.pkg} /
     * {@code package.bluej} file paths.  Walks up the directory
     * tree to find the topmost package directory.
     *
     * @param projectPath the path to resolve
     * @return the project root directory, or {@code null} if the
     *         path is not a valid project
     */
    public @Nullable File resolveProjectRoot(@NotNull String projectPath) {
        File startingDir;
        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        }
        catch (IOException ioe) {
            Debug.reportError(
                "could not resolve directory " + projectPath, ioe);
            return null;
        }

        if (startingDir == null || !Package.isPackage(startingDir)) {
            return null;
        }

        return walkToProjectRoot(startingDir);
    }

    // ── validation ───────────────────────────────────────────────────

    /**
     * Check whether a path points to a valid BlueJ project.
     */
    public boolean isProject(@NotNull String projectPath) {
        return resolveProjectRoot(projectPath) != null;
    }

    // ── path helpers (private) ───────────────────────────────────────

    /**
     * Walk up from a starting directory to find the topmost
     * directory that contains a BlueJ package file and whose
     * intermediate directory names are valid Java identifiers.
     */
    private static @NotNull File walkToProjectRoot(@NotNull File startingDir) {
        File curDir = startingDir;
        File lastDir = null;

        while (curDir != null && Package.isPackage(curDir)) {
            if (lastDir != null
                    && !JavaNames.isIdentifier(lastDir.getName())) {
                break;
            }
            lastDir = curDir;
            curDir = curDir.getParentFile();
        }

        return lastDir != null ? lastDir : startingDir;
    }

    /**
     * Compute the starting package name from the relative path
     * between the project root and the directory the user pointed at.
     */
    private static @NotNull String computeStartingPackageName(
            @NotNull File projectRoot, @NotNull File startingDir) {
        if (projectRoot.equals(startingDir)) {
            return "";
        }
        java.nio.file.Path relative =
            projectRoot.toPath().relativize(startingDir.toPath());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < relative.getNameCount(); i++) {
            String segment = relative.getName(i).toString();
            if (!JavaNames.isIdentifier(segment)) break;
            if (sb.length() > 0) sb.append('.');
            sb.append(segment);
        }
        return sb.toString();
    }

    private static @Nullable File pathIntoStartingDirectory(@NotNull String projectPath)
            throws IOException {
        File startingDir = new File(projectPath).getCanonicalFile();

        if (startingDir.isDirectory()) {
            return startingDir;
        }

        if (startingDir.isFile()) {
            if (Package.isPackageFileName(startingDir.getName())) {
                return startingDir.getParentFile();
            }
        }

        return null;
    }
}
