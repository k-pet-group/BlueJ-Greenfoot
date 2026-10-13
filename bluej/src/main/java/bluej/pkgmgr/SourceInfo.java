/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2026  Michael Kolling and John Rosenberg

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import bluej.parser.InfoParser;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.kotlin.KotlinHierarchyResolver;
import bluej.parser.kotlin.KotlinHierarchyResolver.TypeKind;
import bluej.parser.kotlin.KotlinHierarchyResolver.TypeLookup;
import bluej.parser.kotlin.KotlinInfoParser;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Cached source metadata for automatic editing and hierarchy display.
 * Kotlin PSI is cached separately from hierarchy resolution, which can change when another file changes.
 */
@OnThread(Tag.FXPlatform)
public final class SourceInfo
{
    private ClassInfo info;
    private KotlinInfoParser.ParsedSource kotlinSource;
    private String kotlinText;
    private File kotlinFile;

    /** Create an initially empty source metadata cache. */
    public SourceInfo()
    {
    }

    /** Discard metadata after a source edit. */
    public void setSourceModified()
    {
        info = null;
        kotlinSource = null;
        kotlinText = null;
        kotlinFile = null;
    }

    /**
     * @param sourceFile source file to read
     * @param pkg containing project package
     * @return current metadata, or null if the file cannot be read/parsed
     */
    public ClassInfo getInfo(File sourceFile, Package pkg)
    {
        if (sourceFile != null && sourceFile.getName().toLowerCase().endsWith(".kt")) {
            return getKotlinInfo(sourceFile, pkg.getQualifiedName(), pkg.getProject().getProjectCharset(),
                name -> lookupKotlinType(name, pkg.getProject()));
        }
        if (info == null) {
            try {
                info = InfoParser.parseWithPkg(sourceFile, pkg);
            }
            catch (FileNotFoundException fnfe) {
                // info remains null
            }
        }
        return info;
    }

    // Kept separate from project lookup so syntax caching and cross-file refresh can be tested independently.
    ClassInfo getKotlinInfo(File sourceFile, String packageName, Charset charset, TypeLookup lookup)
    {
        try {
            info = getKotlinSource(sourceFile, charset).getInfo(packageName, lookup);
        }
        catch (IOException e) {
            setSourceModified();
        }
        return info;
    }

    private KotlinInfoParser.ParsedSource getKotlinSource(File file, Charset charset) throws IOException
    {
        String text = Files.readString(file.toPath(), charset);
        if (kotlinSource == null || !file.equals(kotlinFile) || !text.equals(kotlinText)) {
            kotlinSource = new KotlinInfoParser.ParsedSource(text, file.getName());
            kotlinText = text;
            kotlinFile = file;
        }
        return kotlinSource;
    }

    private TypeKind lookupKotlinType(String name, Project project)
    {
        // Check enclosing source targets too: a nested class must not escape to stale bytecode.
        String targetName = name;
        while (!targetName.isEmpty()) {
            Target target = project.getTarget(targetName);
            if (target instanceof ClassTarget ct) {
                TypeKind sourceKind = ct.getSourceInfo().getSourceTypeKind(ct, name);
                if (sourceKind != null) {
                    return sourceKind;
                }
                if (ct.hasSourceCode()) {
                    return TypeKind.UNKNOWN;
                }
                return ct.isCompiled() ? KotlinHierarchyResolver.libraryType(name, project.getClassLoader())
                    : TypeKind.UNKNOWN;
            }
            int dot = targetName.lastIndexOf('.');
            targetName = dot < 0 ? "" : targetName.substring(0, dot);
        }

        return lookupLibraryType(name, project.getClassLoader(), project.getProjectDir());
    }

    // Only libraries may bypass source targets. Untracked project .class files can be stale.
    static TypeKind lookupLibraryType(String name, ClassLoader loader, File projectDir)
    {
        return KotlinHierarchyResolver.libraryType(name, loader, resource -> {
            if (resource.getProtocol().equals("file")) {
                try {
                    return !new File(resource.toURI()).toPath().toRealPath()
                        .startsWith(projectDir.toPath().toRealPath());
                }
                catch (IOException | URISyntaxException e) {
                    return false;
                }
            }
            return true;
        });
    }

    private TypeKind getSourceTypeKind(ClassTarget target, String name)
    {
        try {
            Charset charset = target.getPackage().getProject().getProjectCharset();
            // Check actual source files too: another target may not yet have refreshed its source type.
            boolean stride = target.getFrameSourceFile().exists();
            if (!stride && !target.getJavaSourceFile().exists()) {
                if (!target.getKotlinSourceFile().exists()) {
                    return null;
                }
                TypeKind kind = getKotlinSource(target.getKotlinSourceFile(), charset).getDeclaredType(name);
                return kind == null ? TypeKind.UNKNOWN : kind;
            }
            // Stride's generated Java is current only after compilation. Do not reflect old output.
            if (stride && !target.isCompiled()) {
                return TypeKind.UNKNOWN;
            }
            try (Reader reader = Files.newBufferedReader(target.getJavaSourceFile().toPath(), charset)) {
                ClassInfo declaration = InfoParser.parse(reader,
                    new ClassLoaderResolver(ClassLoader.getPlatformClassLoader()), null);
                if (declaration != null && target.getQualifiedName().equals(name)
                    && target.getBaseName().equals(declaration.getName())
                    && target.getPackage().getQualifiedName().equals(declaration.getPackage())) {
                    return declaration.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
                }
            }
        }
        catch (IOException e) {
            // Reserve the source target's name, even when temporarily unreadable or malformed.
        }
        return TypeKind.UNKNOWN;
    }

    /**
     * Return the most recently computed metadata without parsing or refreshing hierarchy lookup.
     *
     * @return cached metadata, or null if none is available
     */
    public ClassInfo getInfoIfAvailable()
    {
        return info;
    }
}
