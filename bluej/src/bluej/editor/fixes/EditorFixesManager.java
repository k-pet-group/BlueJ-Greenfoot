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

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.fixes.SuggestionList.SuggestionShown;
import bluej.parser.AssistContentThreadSafe;
import bluej.parser.PrimitiveTypeCompletion;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.role.Kind;
import bluej.utility.Debug;
import bluej.utility.Utility;
import javafx.application.Platform;
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
    private final static List<Future<List<AssistContentThreadSafe>>> popularImports = new ArrayList<>();
    @OnThread(Tag.Any)
    private final static List<Future<List<AssistContentThreadSafe>>> rarerImports = new ArrayList<>();
    // This static field can be accessed by any thread from an instance, but is initialised once
    // in an Editor constructor by the first constructed instance, so is thread safe:
    @OnThread(Tag.Any)
    private static Future<List<AssistContentThreadSafe>> javaLangImports;
    @OnThread(Tag.FX)
    private static List<AssistContentThreadSafe> prims;
    // The following variables are to be used on a *per editor* basis
    @OnThread(Tag.Any)
    private final ReadWriteLock importedTypesLock;
    @OnThread(Tag.Any) // but only when the lock is acquired!
    private final List<Future<List<AssistContentThreadSafe>>> importedTypes;
    @OnThread(Tag.Any)
    private Project project;
    @OnThread(Tag.Any)
    private boolean areImportsready = false;

    /**
     * The constructor of EditorFixesManager is called by an Editor,
     * so this is where *per editor* variables are initialised.
     */
    @OnThread(Tag.Any)
    public EditorFixesManager()
    {
        importedTypesLock = new ReentrantReadWriteLock();
        importedTypes = new ArrayList<>();
    }

    public void prepareImports(Project project){
        this.project = project;

        if (getJavaLangImports() == null)
        {
            setJavaLangImports(importsUpdated("java.lang.*"));
        }
        // This should perhaps be in an external config file:
        if (getPopularImports().isEmpty())
        {
            getPopularImports().addAll(Arrays.asList(
                    "java.io.*",
                    "java.math.*",
                    "java.time.*",
                    "java.util.*",
                    "java.util.function.*",
                    "java.util.stream.*",
                    Config.isGreenfoot() ? "greenfoot.*" : null
            ).stream().filter(i -> i != null).map(this::importsUpdated).collect(Collectors.toList()));

            getRarerImports().addAll(Arrays.asList(
                    Config.isGreenfoot() ? null : "java.awt.*",
                    Config.isGreenfoot() ? null : "java.awt.event.*",
                    "java.net.*",
                    "java.text.*",
                    "java.util.concurrent.*",
                    Config.isGreenfoot() ? null : "javafx.application.*",
                    Config.isGreenfoot() ? null : "javafx.beans.*",
                    Config.isGreenfoot() ? null : "javafx.beans.property.*",
                    Config.isGreenfoot() ? null : "javafx.collections.*",
                    Config.isGreenfoot() ? null : "javafx.event.*",
                    Config.isGreenfoot() ? null : "javafx.scene.*",
                    Config.isGreenfoot() ? null : "javafx.scene.control.*",
                    Config.isGreenfoot() ? null : "javafx.scene.input.*",
                    Config.isGreenfoot() ? null : "javafx.scene.layout.*",
                    Config.isGreenfoot() ? null : "javafx.stage.*",
                    Config.isGreenfoot() ? null : "javax.swing.*",
                    Config.isGreenfoot() ? null : "javax.swing.event.*"
            ).stream().filter(i -> i != null).map(this::importsUpdated).collect(Collectors.toList()));
        }
    }


    public Future<List<AssistContentThreadSafe>> importsUpdated(final String x)
    {
        CompletableFuture<List<AssistContentThreadSafe>> f = new CompletableFuture<>();
        Utility.runBackground(() -> {
            try
            {
                f.complete(project.getImportScanner().getImportedTypes(x));
            }
            catch (Throwable t)
            {
                Debug.reportError("Exception while scanning for import " + x, t);
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
        if (popularImports.size() > 0 && rarerImports.size() > 0)
        {
            // Add popular:
            Stream.concat(
                popularImports.stream().flatMap(imps -> Utility.getFutureList(imps).stream().map(ac -> new Pair<>(SuggestionList.SuggestionShown.COMMON, ac))),
                rarerImports.stream().flatMap(imps -> Utility.getFutureList(imps).stream().map(ac -> new Pair<>(SuggestionList.SuggestionShown.RARE, ac)))
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

        // And return the result:
        HashMap<SuggestionList.SuggestionShown, Collection<AssistContentThreadSafe>> ret = new HashMap<>();
        imports.values().forEach(p -> ret.merge(p.getKey(), new ArrayList<AssistContentThreadSafe>(Arrays.asList(p.getValue())), (BiFunction<Collection<AssistContentThreadSafe>, Collection<AssistContentThreadSafe>, Collection<AssistContentThreadSafe>>) (a, b) -> {a.addAll(b); return a;}));
        areImportsready = (ret.size() > 0);
        return ret;
    }

    @OnThread(Tag.Any)
    public boolean areImportsready(){
        return areImportsready;
    }

    @OnThread(Tag.FXPlatform)
    public List<AssistContentThreadSafe> getPrimitiveTypes()
    {
        if (prims == null)
            prims = PrimitiveTypeCompletion.allPrimitiveTypes().stream().map(AssistContentThreadSafe::copy).collect(Collectors.toList());
        return prims;
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
        return Stream.concat(Stream.of(javaLangImports), importedTypesCopy.stream()).map(Utility::getFutureList).flatMap(List::stream);
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
    public List<Future<List<AssistContentThreadSafe>>> getPopularImports (){
        return popularImports;
    }

    @OnThread(Tag.Any)
    public List<Future<List<AssistContentThreadSafe>>> getRarerImports(){
        return rarerImports;
    }

    @OnThread(Tag.Any)
    public Future<List<AssistContentThreadSafe>> getJavaLangImports(){
        return javaLangImports;
    }

    @OnThread(Tag.Any)
    public void setJavaLangImports(Future<List<AssistContentThreadSafe>> javaLangImports){
        this.javaLangImports = javaLangImports;
    }

    @OnThread(Tag.FX)
    public List<AssistContentThreadSafe> getPrims(){
        return prims;
    }

    public static class ImportSingleFix extends FixSuggestion
    {
        private final AssistContentThreadSafe classInfo;
        private final Editor editor;

        @OnThread(Tag.Any)
        public ImportSingleFix(Editor editor, AssistContentThreadSafe ac)
        {
            this.editor = editor;
            this.classInfo = ac;
        }

        @Override
        @OnThread(Tag.Any)
        public String getDescription()
        {
            return "Import class " + classInfo.getPackage() + "." + classInfo.getName();
        }

        @Override
        public void execute()
        {
            editor.addImportFromQuickFix(classInfo.getPackage() + "." + classInfo.getName());
        }
    }

    public static class ImportPackageFix extends FixSuggestion
    {
        private final AssistContentThreadSafe classInfo;
        private final Editor editor;

        @OnThread(Tag.Any)
        public ImportPackageFix(Editor editor, AssistContentThreadSafe ac)
        {
            this.editor = editor;
            this.classInfo = ac;
        }

        @Override
        @OnThread(Tag.Any)
        public String getDescription()
        {
            return "Import package " + classInfo.getPackage() + " (for " + classInfo.getName() + " class)";
        }

        @Override
        public void execute()
        {
            editor.addImportFromQuickFix(classInfo.getPackage() + ".*");
        }
    }
}
