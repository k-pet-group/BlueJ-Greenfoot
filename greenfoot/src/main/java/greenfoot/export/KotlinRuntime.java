/*
 This file is part of the Greenfoot program.
 Copyright (C) 2026  Poul Henriksen and Michael Kolling

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
package greenfoot.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;

import bluej.Boot;
import bluej.Config;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Required installed runtime inputs for a Kotlin publishing request. */
@OnThread(Tag.Any)
final class KotlinRuntime
{
    private static final KotlinRuntime NONE = new KotlinRuntime(null, List.of());
    private final File stdlib;
    private final List<File> licenceFiles;

    private KotlinRuntime(File stdlib, List<File> licenceFiles)
    {
        this.stdlib = stdlib;
        this.licenceFiles = licenceFiles;
    }

    static KotlinRuntime prepare(boolean hasKotlinSources) throws IOException
    {
        return prepare(hasKotlinSources, () -> Boot.getInstance().getRuntimeUserClassPath(), Config::getGreenfootLibDir);
    }

    // Suppliers keep Java/Stride publishing independent of both Kotlin runtime and licence lookup.
    static KotlinRuntime prepare(boolean hasKotlinSources, Supplier<URL[]> studentClassPath,
                                 Supplier<File> greenfootLibDir) throws IOException
    {
        if (!hasKotlinSources) {
            return NONE;
        }

        File stdlib = findStdlib(studentClassPath.get());
        validateStdlib(stdlib);
        File licenceDir = new File(greenfootLibDir.get(), "export/kotlin");
        List<File> licenceFiles = List.of(new File(licenceDir, "LICENSE.txt"), new File(licenceDir, "NOTICE.txt"),
                new File(licenceDir, "README.txt"));
        for (File licence : licenceFiles) {
            requireReadableFile(licence);
            try (InputStream input = Files.newInputStream(licence.toPath())) {
                if (input.transferTo(OutputStream.nullOutputStream()) == 0) {
                    throw new IOException("Empty Kotlin runtime licence file: " + licence);
                }
            }
        }
        return new KotlinRuntime(stdlib, licenceFiles);
    }

    private static File findStdlib(URL[] studentClassPath) throws IOException
    {
        if (studentClassPath != null) {
            for (URL url : studentClassPath) {
                // Boot supplies local installed JARs. Do not fetch remote libraries or search lib/+libs.
                if (!url.getProtocol().equals("file")) {
                    continue;
                }
                try {
                    File file = new File(url.toURI());
                    // Select the first main stdlib in actual classpath order, not jdk7/jdk8 adapters or a newer sibling.
                    if (file.getName().matches("kotlin-stdlib(?:-[0-9].*)?\\.jar")) {
                        return file;
                    }
                }
                catch (URISyntaxException | IllegalArgumentException e) {
                    throw new IOException("Invalid installed student runtime path: " + url, e);
                }
            }
        }
        throw new IOException("Kotlin stdlib is missing from the installed student runtime classpath");
    }

    private static void requireReadableFile(File file) throws IOException
    {
        if (!file.isFile() || !Files.isReadable(file.toPath())) {
            throw new IOException("Missing or unreadable Kotlin runtime file: " + file);
        }
    }

    private static void validateStdlib(File stdlib) throws IOException
    {
        requireReadableFile(stdlib);
        try (JarFile jar = new JarFile(stdlib)) {
            if (jar.getJarEntry("kotlin/Unit.class") == null || jar.getJarEntry("kotlin/jvm/internal/Intrinsics.class") == null) {
                throw new IOException("Installed Kotlin stdlib is missing runtime classes: " + stdlib);
            }
            // Opening a ZIP alone does not detect damaged entry data. Read/check it before any artifact is created.
            byte[] buffer = new byte[8192];
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                CRC32 crc = new CRC32();
                try (InputStream input = jar.getInputStream(entry)) {
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        crc.update(buffer, 0, count);
                    }
                }
                if (crc.getValue() != entry.getCrc()) {
                    throw new IOException("Corrupt Kotlin stdlib entry " + entry.getName() + " in " + stdlib);
                }
            }
        }
        catch (SecurityException e) {
            throw new IOException("Cannot verify installed Kotlin stdlib: " + stdlib, e);
        }
    }

    void addTo(JarCreator creator)
    {
        if (stdlib != null) {
            creator.addJarToJar(stdlib);
            for (File licence : licenceFiles) {
                creator.addFile("META-INF/kotlin/", licence);
            }
        }
    }
}
