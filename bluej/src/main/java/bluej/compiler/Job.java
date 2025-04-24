/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016,2020,2022  Michael Kolling and John Rosenberg
 
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
package bluej.compiler;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.utility.Utility;

/**
 * A compiler "job". A list of filenames to compile + parameters.
 * Jobs are held in a queue by the CompilerThread, which compiles them
 * by running the job's "compile" method.
 *
 * @author  Michael Cahill
 */
record Job(List<CompileInputFile> sources, Compiler javaCompiler, Compiler kotlinCompiler, CompileObserver observer, BPClassLoader bpClassLoader, File destDir,
           boolean internal, // true for compiling shell files,
                             // or user files if we want to suppress
                             // "unchecked" warnings, false otherwise
           List<String> userCompileOptions, Charset fileCharset, CompileType type, CompileReason reason)
{
    /**
     * Generator for unique ascending compilation identifiers.  It doesn't matter if it's shared between
     * packages or between projects, it just needs to be unique and ascending.  An individual user won't manage
     * 2 billion compilations in a single session, so integer is fine:
     */
    private static final AtomicInteger nextCompilationSequence = new AtomicInteger(1);

    private void configureCompiler(Compiler compiler, File destDir, BPClassLoader bpClassLoader)
    {
        if(destDir != null) {
            compiler.setDestDir(destDir);
        }
        compiler.setClasspath(bpClassLoader.getClassPathAsFiles());
        compiler.setBootClassPath(null);
    }

    /**
     * Compile this job
     */
    public void compile()
    {
        int compilationSequence = nextCompilationSequence.getAndIncrement();

        configureCompiler(javaCompiler, destDir, bpClassLoader);

        CompileInputFile[] sourcesArray = sources.toArray(new CompileInputFile[0]);
        try {
            if(observer != null) {
                observer.startCompile(sourcesArray, reason, type, compilationSequence);
            }

            // We could make a new file manager that memory-mapped the output files
            // and discarded them... but creating a temporary dir is much more
            // straightforward:
            File outputDir = type.keepClasses() ? destDir : Files.createTempDirectory("bluej").toFile();

            List<File> srcFiles = Utility.mapList(sources, CompileInputFile::getCompileInputFile);
            List<File> javaSourceFiles = Utility.mapList(
                    Utility.filterList(sources, cif -> cif.getCompileFileExtension().equals("java")),
                    CompileInputFile::getCompileInputFile);

            boolean successful = true;
            // We compile Kotlin files first to make sure that class files are later available for Java compilation.
            if (kotlinCompiler != null)
            {
                configureCompiler(kotlinCompiler, outputDir, bpClassLoader);
                // Kotlin compiler needs all the source files (Kotlin and Java) to be passed to it at once.
                successful &= kotlinCompiler.compile(srcFiles.toArray(new File[0]),
                        observer, internal, userCompileOptions, fileCharset, type, outputDir);
            }

            if (!javaSourceFiles.isEmpty())
            {
                successful &= javaCompiler.compile(javaSourceFiles.toArray(new File[0]),
                        observer, internal, userCompileOptions, fileCharset, type, outputDir);
            }

            if (!type.keepClasses() && outputDir != null)
            {
                outputDir.delete();
            }

            if(observer != null)
            {
                observer.endCompile(sourcesArray, successful, type, compilationSequence);
            }
        } catch(Exception e) {
            System.err.println(Config.getString("compileException") + ": " + e);
            e.printStackTrace();
            if (observer != null) {
                observer.endCompile(sourcesArray, false, type, compilationSequence);
            }
        }
    }
}
