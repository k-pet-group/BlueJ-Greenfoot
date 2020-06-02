/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020  Michael Kolling and John Rosenberg 
 
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
import bluej.parser.AssistContentThreadSafe;
import bluej.parser.PrimitiveTypeCompletion;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A per-project collection of completions for commonly imported items.
 * For example, it contains an import for java.util.List and in Greenfoot for greenfoot.Greenfoot.
 */
public class ProjectImportInformation
{
    @OnThread(Tag.Any)
    private final List<AssistContentThreadSafe> popularImports;
    @OnThread(Tag.Any)
    private final List<AssistContentThreadSafe> rarerImports;
    // This static field can be accessed by any thread from an instance, but is initialised once
    // in an Editor constructor by the first constructed instance, so is thread safe:
    @OnThread(Tag.Any)
    private final List<AssistContentThreadSafe> javaLangImports;
    @OnThread(Tag.FX)
    private final static List<AssistContentThreadSafe> prims = PrimitiveTypeCompletion.allPrimitiveTypes().stream().map(AssistContentThreadSafe::copy).collect(Collectors.toList());
    private final Project project;
    
    @OnThread(Tag.Worker)
    public ProjectImportInformation(Project project)
    {
        // This must be set first before calling scanImports:
        this.project = project;
        javaLangImports = scanImports("java.lang.*");
        // This should perhaps be in an external config file:
        popularImports = Arrays.asList(
                "java.io.*",
                "java.math.*",
                "java.time.*",
                "java.util.*",
                "java.util.function.*",
                "java.util.stream.*",
                Config.isGreenfoot() ? "greenfoot.*" : null
        ).stream().filter(i -> i != null).flatMap(i -> scanImports(i).stream()).collect(Collectors.toList());

        rarerImports = Arrays.asList(
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
        ).stream().filter(i -> i != null).flatMap(i -> scanImports(i).stream()).collect(Collectors.toList());

    }

    /**
     * Scans for imports that match the given import string.
     * @param importString A Java import like "java.util.List" or "java.awt.*"
     */
    @OnThread(Tag.Worker)
    public List<AssistContentThreadSafe> scanImports(final String importString)
    {
        try
        {
            return project.getImportScanner().getImportedTypes(importString);
        }
        catch (Throwable t)
        {
            Debug.reportError("Exception while scanning for import " + importString, t);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the most popular imports, e.g. java.util.List
     */
    @OnThread(Tag.Any)
    public List<AssistContentThreadSafe> getPopularImports()
    {
        return popularImports;
    }

    /**
     * Gets slightly rarer imports, like JavaFX.
     */
    @OnThread(Tag.Any)
    public List<AssistContentThreadSafe> getRarerImports()
    {
        return rarerImports;
    }

    /**
     * Gets java.lang items which shouldn't need an import, like java.lang.String.
     */
    @OnThread(Tag.Any)
    public List<AssistContentThreadSafe> getJavaLangImports()
    {
        return javaLangImports;
    }

    /**
     * Gets the primitive types (e.g. int, char) as completions
     */
    public static List<AssistContentThreadSafe> getPrims()
    {
        return prims;
    }
}
