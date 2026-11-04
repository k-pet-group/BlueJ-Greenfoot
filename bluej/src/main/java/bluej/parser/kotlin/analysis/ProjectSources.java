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
package bluej.parser.kotlin.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The Kotlin and Java source files found on disk beneath a project directory.
 *
 * @param kotlinFiles      the Kotlin source files (normalised paths), in sorted order
 * @param javaSourceStamps one entry per Java source file combining its relative path, modification
 *                         time and size, in sorted order; equal stamps mean the Java sources are unchanged
 */
@OnThread(Tag.Worker)
record ProjectSources(List<File> kotlinFiles, List<String> javaSourceStamps)
{
    /**
     * Scan a project directory. Hidden directories (such as {@code .git}) are skipped.
     *
     * @param projectDir the project directory
     * @return the sources found
     * @throws IOException if the directory cannot be walked
     */
    static ProjectSources scan(File projectDir) throws IOException
    {
        Path root = KotlinAnalysisRequest.normalize(projectDir).toPath();
        List<File> kotlinFiles = new ArrayList<>();
        List<String> javaSourceStamps = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                boolean hidden = !dir.equals(root) && dir.getFileName().toString().startsWith(".");
                return hidden ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                String name = file.getFileName().toString();
                if (name.endsWith(".kt")) {
                    kotlinFiles.add(file.toFile());
                }
                else if (name.endsWith(".java")) {
                    javaSourceStamps.add(root.relativize(file) + "|" + attrs.lastModifiedTime().toMillis() + "|" + attrs.size());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
            {
                // An unreadable file is simply not part of the analysis.
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(kotlinFiles);
        Collections.sort(javaSourceStamps);
        return new ProjectSources(List.copyOf(kotlinFiles), List.copyOf(javaSourceStamps));
    }
}
