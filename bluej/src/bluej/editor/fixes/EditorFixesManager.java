/*
 This file is part of the BlueJ program.
 Copyright (C) 2020 Michael Kölling and John Rosenberg

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
package bluej.editor.fixes;

import bluej.editor.fixes.SuggestionList.SuggestionShown;
import bluej.parser.AssistContentThreadSafe;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.role.Kind;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import javafx.util.Pair;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to be used by editors (Java and Stride) to manage quick fixes.
 */
public class EditorFixesManager
{
    @OnThread(Tag.Any)
    private final CompletableFuture<ProjectImportInformation> projectImportInformation;
    
    // The following variables are to be used on a *per editor* basis
    @OnThread(Tag.Any)
    private final ReadWriteLock importedTypesLock;
    @OnThread(Tag.Any) // but only when the lock is acquired!
    private final List<Future<List<AssistContentThreadSafe>>> importedTypes;
    @OnThread(Tag.Any)
    private Project project;

    /**
     * The constructor of EditorFixesManager is called by an Editor,
     * so this is where *per editor* variables are initialised.
     * @param projectImportInformation
     */
    @OnThread(Tag.Any)
    public EditorFixesManager(CompletableFuture<ProjectImportInformation> projectImportInformation)
    {
        this.projectImportInformation = projectImportInformation;
        importedTypesLock = new ReentrantReadWriteLock();
        importedTypes = new ArrayList<>();
    }

    /**
     * Scans for imports in a background thread that match the given import string.
     * @param importString A Java import like "java.util.List" or "java.awt.*"
     */
    @OnThread(Tag.Any)
    public Future<List<AssistContentThreadSafe>> scanImports(final String importString)
    {
        CompletableFuture<List<AssistContentThreadSafe>> f = new CompletableFuture<>();
        Utility.runBackground(() -> {
            try
            {
                f.complete(projectImportInformation.get().scanImports(importString));
            }
            catch (Throwable t)
            {
                Debug.reportError("Exception while scanning for import " + importString, t);
                f.complete(Collections.emptyList());
            }
        });
        return f;
    }

    /**
     * Gets a list of classes that are commonly imported in Java programs,
     * e.g. classes from java.util, java.io, and so on.
     *
     * This list will not feature any class that is already imported in the program.
     */
    @OnThread(Tag.Worker)
    public Map<SuggestionList.SuggestionShown, Collection<AssistContentThreadSafe>> getImportSuggestions()
    {
        HashMap<String, Pair<SuggestionShown, AssistContentThreadSafe>> imports = new HashMap<>();
        try
        {
            List<AssistContentThreadSafe> popularImports = projectImportInformation.get().getPopularImports();
            List<AssistContentThreadSafe> rarerImports = projectImportInformation.get().getRarerImports();
        
            // Add popular:
            Stream.concat(
                    popularImports.stream().map(ac -> new Pair<>(SuggestionList.SuggestionShown.COMMON, ac)),
                    rarerImports.stream().map(ac -> new Pair<>(SuggestionList.SuggestionShown.RARE, ac))
            )
                .filter(imp -> imp.getValue().getPackage() != null)
                .forEach(imp -> {
                    String fullName = imp.getValue().getPackage() + ".";
                    if (imp.getValue().getDeclaringClass() != null)
                    {
                        fullName += imp.getValue().getDeclaringClass() + ".";
                    }
                    fullName += imp.getValue().getName();
                    imports.put(fullName, imp);
                });
            // Remove what we already import:
            getAllImportedTypes()
                    .filter(imp -> imp.getPackage() != null)
                    .forEach(imp -> imports.remove(imp.getPackage() + "." + imp.getName()));
            // Remove imports we want to hide (for instance that are unused and confusing for users)
            imports.remove("java.awt.List");
        
        }
        catch (InterruptedException | ExecutionException ex)
        {
            Debug.reportError(ex);
        }

        // And return the result:
        HashMap<SuggestionList.SuggestionShown, Collection<AssistContentThreadSafe>> ret = new HashMap<>();
        imports.values().forEach(p -> ret.merge(p.getKey(), new ArrayList<AssistContentThreadSafe>(Arrays.asList(p.getValue())), (BiFunction<Collection<AssistContentThreadSafe>, Collection<AssistContentThreadSafe>, Collection<AssistContentThreadSafe>>) (a, b) -> {a.addAll(b); return a;}));
        return ret;
    }

    @OnThread(Tag.FXPlatform)
    public List<AssistContentThreadSafe> getPrimitiveTypes()
    {
        return ProjectImportInformation.getPrims();
    }

    @OnThread(Tag.Any)
    public List<Future<List<AssistContentThreadSafe>>> getImportedTypesFutureList()
    {
       return importedTypes;
    }

    @OnThread(Tag.Worker)
    public Stream<AssistContentThreadSafe> getAllImportedTypes()
    {
        importedTypesLock.readLock().lock();
        ArrayList<Future<List<AssistContentThreadSafe>>> importedTypesCopy = new ArrayList<>(importedTypes);
        importedTypesLock.readLock().unlock();
        return Stream.concat(Stream.of(projectImportInformation.thenApply(i -> i.getJavaLangImports())), importedTypesCopy.stream()).map(Utility::getFutureList).flatMap(List::stream);
    }

    @OnThread(Tag.Worker)
    public List<AssistContentThreadSafe> getImportedTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds)
    {
        if (superType == null)
            return getAllImportedTypes()
                    .filter(ac -> kinds.contains(ac.getTypeKind()))
                    .collect(Collectors.toList());

        return getAllImportedTypes()
                .filter(ac -> kinds.contains(ac.getTypeKind()))
                .filter(ac -> ac.getSuperTypes().contains(superType.getName()) || (includeSelf && ac.getPackage() != null && (ac.getPackage() + "." + ac.getName()).equals(superType.getName())))
                .collect(Collectors.toList());
    }

    @OnThread(Tag.Any)
    public ReadWriteLock getImportedTypesLock()
    {
        return importedTypesLock;
    }

    @OnThread(Tag.Any)
    public List<AssistContentThreadSafe> getJavaLangImports()
    {
        try
        {
            return projectImportInformation.get().getJavaLangImports();
        }
        catch (InterruptedException | ExecutionException ex)
        {
            Debug.reportError(ex);
            return Collections.emptyList();
        }
    }

    /**
     * Base class for a quick fix suggestion.
     * The class contains a description (that will be indicated in the fix label)
     * and an executable to run when the fix is to be executed.
     */
    public static class FixSuggestionBase  extends FixSuggestion
    {
        private final String description;
        private final FXPlatformRunnable executeRunnable;

        @OnThread(Tag.Any)
        public FixSuggestionBase (String description, FXPlatformRunnable executeRunnable)
        {
            this.description = description;
            this.executeRunnable = executeRunnable;
        }

        @Override
        @OnThread(Tag.Any)
        public String getDescription()
        {
            return description;
        }

        @Override
        public void execute()
        {
            executeRunnable.run();
        }
    }
}
