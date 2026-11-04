/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016,2020,2022,2026  Michael Kolling and John Rosenberg

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.FileUtility;

import threadchecker.OnThread;
import threadchecker.Tag;

/** One selected source set and its configuration, executed as a single compiler request. */
record Job(List<CompileInputFile> sources, CompilerAPICompiler javaCompiler, KotlinCompiler kotlinCompiler,
           CompileObserver observer, List<File> classPath, File destDir,
           boolean internal, // true for compiling shell files,
                             // or user files if we want to suppress
                             // "unchecked" warnings, false otherwise
           List<String> userCompileOptions, Charset fileCharset, CompileType type, CompileReason reason,
           boolean keepClasses)
{
    /**
     * Generator for unique ascending compilation identifiers.  It doesn't matter if it's shared between
     * packages or between projects, it just needs to be unique and ascending.  An individual user won't manage
     * 2 billion compilations in a single session, so integer is fine:
     */
    private static final AtomicInteger nextCompilationSequence = new AtomicInteger(1);

    Job
    {
        sources = List.copyOf(sources);
        classPath = List.copyOf(classPath);
        userCompileOptions = List.copyOf(userCompileOptions);
    }

    // Only the compiler thread configures and invokes the shared backends.
    @OnThread(Tag.Worker)
    void compile()
    {
        int compilationSequence = nextCompilationSequence.getAndIncrement();
        boolean successful = false;
        File tempDir = null;
        try {
            if (observer != null) {
                observer.startCompile(sources.toArray(CompileInputFile[]::new), reason, type, compilationSequence);
            }

            File outputDir = destDir;
            if (!keepClasses) {
                tempDir = Files.createTempDirectory("bluej-compile").toFile();
                outputDir = tempDir;
            }
            List<File> phaseClassPath = new ArrayList<>(classPath);
            if (tempDir != null) {
                // Prefer this request's Kotlin output over any previously retained classes.
                phaseClassPath.add(0, tempDir);
            }

            JobQueue.LanguagePartition partition = JobQueue.partitionByLanguage(
                    sources.toArray(CompileInputFile[]::new), kotlinCompiler != null);
            successful = true;
            // Kotlin first so its .class output is on javac's classpath; .java sources are
            // also handed to K2 for Kotlin-to-Java symbol resolution.
            if (!partition.kotlinSources().isEmpty()) {
                configure(kotlinCompiler, phaseClassPath);
                successful = kotlinCompiler.compile(actualFiles(partition.kotlinSources()), partition.javaSources(),
                        outputDir, observer, type);
            }
            if (successful && !partition.javaSources().isEmpty()) {
                configure(javaCompiler, phaseClassPath);
                successful = javaCompiler.compile(actualFiles(partition.javaSources()), observer, internal,
                        userCompileOptions, fileCharset, type, outputDir);
            }
        }
        catch (Exception e) {
            successful = false;
            Debug.reportError(Config.getString("compileException"), e);
        }
        finally {
            if (tempDir != null) {
                FileUtility.deleteDir(tempDir);
            }
        }

        // Completion belongs to the request, not either backend, and must never be retried.
        if (observer != null) {
            try {
                observer.endCompile(sources.toArray(CompileInputFile[]::new), successful, type, compilationSequence);
            }
            catch (Exception e) {
                Debug.reportError(Config.getString("compileException"), e);
            }
        }
    }

    @OnThread(Tag.Worker)
    private void configure(Compiler compiler, List<File> phaseClassPath)
    {
        compiler.setDestDir(destDir);
        compiler.setClasspath(phaseClassPath);
        compiler.setBootClassPath(null);
    }

    private static File[] actualFiles(List<CompileInputFile> inputs)
    {
        return inputs.stream().map(CompileInputFile::getJavaCompileInputFile).toArray(File[]::new);
    }
}
