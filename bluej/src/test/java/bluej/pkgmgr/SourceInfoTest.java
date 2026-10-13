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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import bluej.parser.kotlin.KotlinHierarchyResolver;
import bluej.parser.kotlin.KotlinHierarchyResolver.TypeKind;
import bluej.parser.kotlin.KotlinHierarchyResolver.TypeLookup;
import bluej.parser.symtab.ClassInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Regression tests for Kotlin source metadata caching and cross-file hierarchy refresh. */
public class SourceInfoTest
{
    /** Automatically removed source fixtures. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private File source(String name, String text) throws Exception
    {
        File file = temporary.newFile(name);
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        return file;
    }

    private ClassInfo info(SourceInfo cache, File file, TypeLookup lookup)
    {
        return cache.getKotlinInfo(file, "demo", StandardCharsets.UTF_8, lookup);
    }

    /**
     * Editing another declaration must refresh hierarchy even when the child source is unchanged.
     *
     * @throws Exception if source fixture I/O fails
     */
    @Test
    public void testExternalDeclarationChanges() throws Exception
    {
        SourceInfo childCache = new SourceInfo();
        SourceInfo baseCache = new SourceInfo();
        File child = source("Child.kt", "package demo\nimport library.*\n"
            + "class Child : Base { constructor() : super() }");
        File base = source("Base.kt", "package demo\nopen class Base");
        TypeLookup lookup = name -> {
            if (name.equals("demo.Base") && base.exists()) {
                ClassInfo declaration = info(baseCache, base, n -> null);
                return declaration == null ? TypeKind.UNKNOWN
                    : declaration.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
            }
            return name.equals("library.Base") ? TypeKind.CLASS : null;
        };
        assertEquals("demo.Base", info(childCache, child, lookup).getSuperclass());
        Files.writeString(base.toPath(), "package demo\ninterface Base");
        ClassInfo changed = info(childCache, child, lookup);
        assertNull(changed.getSuperclass());
        assertEquals(List.of("demo.Base"), changed.getImplements());
        Files.delete(base.toPath());
        assertEquals("library.Base", info(childCache, child, lookup).getSuperclass());
        Files.writeString(base.toPath(), "package demo\nopen class Base");
        assertEquals("demo.Base", info(childCache, child, lookup).getSuperclass());
    }

    /**
     * External source edits are detected without depending on timestamp resolution or file size.
     *
     * @throws Exception if source fixture I/O fails
     */
    @Test
    public void testSameSizeSourceEdit() throws Exception
    {
        SourceInfo cache = new SourceInfo();
        File child = source("Child.kt", "package demo\nclass Child : first.Base()");
        long modified = child.lastModified();
        assertEquals("first.Base", info(cache, child, n -> null).getSuperclass());
        Files.writeString(child.toPath(), "package demo\nclass Child : other.Base()");
        assertTrue(child.setLastModified(modified));
        assertEquals("other.Base", info(cache, child, n -> null).getSuperclass());
    }

    /**
     * Failed source must replace, rather than expose, the previous successful metadata.
     *
     * @throws Exception if source fixture I/O fails
     */
    @Test
    public void testUnreadableAndUnsupportedSourceClearsMetadata() throws Exception
    {
        SourceInfo cache = new SourceInfo();
        File child = source("Child.kt", "package demo\nclass Child : library.Base()");
        assertNotNull(info(cache, child, n -> null));
        Files.writeString(child.toPath(), "package demo\n");
        assertNull(info(cache, child, n -> null));
        assertNull(cache.getInfoIfAvailable());
        Files.delete(child.toPath());
        assertNull(info(cache, child, n -> null));
        assertNull(cache.getInfoIfAvailable());
    }

    /**
     * Library metadata and explicit cache invalidation retain the existing source-info contract.
     *
     * @throws Exception if the source fixture cannot be written
     */
    @Test
    public void testInvalidationAndLibraryLookup() throws Exception
    {
        SourceInfo cache = new SourceInfo();
        File child = source("Child.kt", "package demo\nimport java.util.*\nclass Child : ArrayList<String>()");
        ClassInfo result = info(cache, child,
            name -> KotlinHierarchyResolver.libraryType(name, getClass().getClassLoader()));
        assertEquals("java.util.ArrayList", result.getSuperclass());
        assertSame(result, cache.getInfoIfAvailable());
        cache.setSourceModified();
        assertNull(cache.getInfoIfAvailable());
        assertNotNull(info(cache, child, n -> null));
    }

    /**
     * Cache reuse across filenames must still update function-file identity.
     *
     * @throws Exception if the source fixtures cannot be written
     */
    @Test
    public void testFilenameIsPartOfSnapshotIdentity() throws Exception
    {
        SourceInfo cache = new SourceInfo();
        String text = "package demo\nfun run() {}";
        File first = source("First.kt", text);
        File second = source("Second.kt", text);
        assertEquals("First", info(cache, first, n -> null).getName());
        assertEquals("Second", info(cache, second, n -> null).getName());
    }

    /**
     * Untracked project bytecode must be rejected before the loader can cache it, including nested types.
     *
     * @throws Exception if fixture I/O or classloader cleanup fails
     */
    @Test
    public void testProjectOutputIsNotALibrary() throws Exception
    {
        File project = temporary.newFolder("project");
        File pkg = new File(project, "demo");
        assertTrue(pkg.mkdir());
        Files.write(new File(pkg, "Base.class").toPath(), new byte[0]);
        Files.write(new File(pkg, "Outer$Inner.class").toPath(), new byte[0]);
        AtomicInteger loads = new AtomicInteger();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {project.toURI().toURL()},
            ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("demo.")) {
                    loads.incrementAndGet();
                }
                return super.loadClass(name, resolve);
            }
        }) {
            assertNull(SourceInfo.lookupLibraryType("demo.Base", loader, project));
            assertNull(SourceInfo.lookupLibraryType("demo.Outer.Inner", loader, project));
            assertEquals("No stale output may enter the loader", 0, loads.get());
            assertEquals(TypeKind.INTERFACE, SourceInfo.lookupLibraryType("java.lang.Runnable", loader, project));
        }
    }
}
