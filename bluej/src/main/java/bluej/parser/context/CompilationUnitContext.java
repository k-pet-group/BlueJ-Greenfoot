/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.parser.context;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Encapsulates the context information for a compilation unit.
 * This includes method/field documentation, parameter names, and other metadata
 * stored in .ctxt files.
 */
@OnThread(value = Tag.Any, ignoreParent = true)
public class CompilationUnitContext {
    
    /** A list of comments contained in this compilation unit */
    private final List<CommentEntry> comments;
    /** The class contained in this compilation unit (NOTE: this will have to be changed for Kotlin) */
    private final String className;
    /** The context file backing this information (if any) */
    private final File ctxtFile;
    /** Stores whether this context is read only */
    private final boolean readOnly;


    CompilationUnitContext(@NotNull String className, File ctxtFile, boolean readOnly) {
        this.className = className;
        this.ctxtFile = ctxtFile;
        this.readOnly = readOnly;
        this.comments = new ArrayList<>();
    }


    /**
     * Creates a new context for a class.
     *
     * @param className The name of the class
     * @param ctxtFile The .ctxt file backing this context class
     */
    static CompilationUnitContext writable(@NotNull String className, @NotNull File ctxtFile) {
        return new CompilationUnitContext(className, ctxtFile, false);
    }


    /**
     * Creates a new read-only context for a class.
     * 
     * @param className The name of the class
     */
    static CompilationUnitContext readOnly(@NotNull String className) {
        return new CompilationUnitContext(className, null, true);
    }


    /**
     * Gets the class name.
     *
     * @return The class name
     */
    @NotNull public String getClassName() {
        return className;
    }


    /**
     * Gets all comments.
     *
     * @return An unmodifiable list of comments
     */
    @NotNull public List<CommentEntry> getComments() {
        return Collections.unmodifiableList(comments);
    }


    /**
     * Adds a comment entry.
     *
     * @param comment The comment to add
     */
    public void addComment(@NotNull CommentEntry comment) {
        comments.add(comment);
    }


    /**
     * Replace all comments with the given list.
     *
     * @param entries The comments to replace with
     */
    void setComments(List<CommentEntry> entries) {
        comments.clear();
        comments.addAll(entries);
    }


    /**
     * Clears all comments.
     */
    public void clear() {
        comments.clear();
    }


    /**
     * Checks if the context is empty.
     *
     * @return true if there are no comments
     */
    public boolean isEmpty() {
        return comments.isEmpty();
    }


    /**
     * Finds a comment by target signature.
     * 
     * @param targetSignature The target signature to search for
     *
     * @return The matching comment entry, or null if not found
     */
    public CommentEntry findComment(@NotNull String targetSignature) {
        return comments.stream()
            .filter(c -> c.getTarget().equals(targetSignature))
            .findFirst()
            .orElse(null);
    }


    /**
     * Gets comments matching a regular expression pattern.
     * 
     * @param regex The regular expression pattern
     *
     * @return A list of matching comments
     */
    @NotNull public List<CommentEntry> findCommentsMatching(@NotNull String regex) {
        try {
            return comments.stream()
                .filter(c -> c.getTarget().matches(regex))
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Invalid regex
            return Collections.emptyList();
        }
    }
    

    /**
     * Deletes the .ctxt file.
     * 
     * @return true if the file was deleted, false otherwise
     */
    public boolean delete() {
        if (!readOnly && ctxtFile != null && ctxtFile.exists()) {
            boolean deleted = ctxtFile.delete();
            if (deleted) {
                comments.clear();
            }
            return deleted;
        }
        return false;
    }


    @Override
    @NotNull public String toString() {
        return "CompilationUnitContext[class=" + className + 
               ", comments=" + comments.size() + 
               ", file=" + (ctxtFile != null ? ctxtFile.getName() : "<none>") + "]";
    }
}