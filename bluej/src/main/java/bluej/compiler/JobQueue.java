/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2016,2026  Michael Kolling and John Rosenberg
 
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.utility.Utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Reasonably generic interface between the BlueJ IDE and the compiler(s).
 * Manages a single CompilerThread and dispatches compilation jobs to the
 * appropriate compiler (Java or Kotlin) based on source file extension.
 */
public class JobQueue
{
    private static JobQueue queue = null;

    /**
     * Returns the shared compilation queue, starting its worker if necessary.
     *
     * @return the application compilation queue
     */
    @OnThread(Tag.Any)
    public static synchronized JobQueue getJobQueue()
    {
        if (queue == null) {
            queue = new JobQueue();
        }
        return queue;
    }

    // ---- instance ----

    private final CompilerThread thread;
    private final CompilerAPICompiler javaCompiler;
    private final KotlinCompiler kotlinCompiler;

    /**
     * Construct the JobQueue. This is private; use getJobQueue() to get the job queue instance.
     */
    private JobQueue()
    {
        javaCompiler = new CompilerAPICompiler();
        kotlinCompiler = new KotlinCompiler();
        thread = new CompilerThread();

        // Lower priority to improve GUI response time during compilation
        int priority = Thread.currentThread().getPriority() - 1;
        priority = Math.max(priority, Thread.MIN_PRIORITY);
        thread.setPriority(priority);

        thread.start();
    }

    /**
     * Adds one request to the compile queue. Sources are partitioned by extension:
     * {@code .kt} files go to {@link KotlinCompiler}, the rest to {@link CompilerAPICompiler}.
     * When both languages are present, Kotlin runs first, then Java only if Kotlin succeeds.
     * Inputs and configuration are copied; source file contents are read when compilation runs.
     *
     * @param sources       The files to compile
     * @param observer      Observer notified at start, errors/warnings, and completion
     * @param bpClassLoader The classpath to use to locate objects/source code
     * @param destDir       Project source root and destination for retained class files
     * @param suppressUnchecked   Suppress "unchecked" warning
     * @param fileCharset   Source file character set
     * @param reason        Reason for compilation
     * @param type          Compilation and output-retention policy
     */
    @OnThread(Tag.Any)
    public void addJob(CompileInputFile[] sources, CompileObserver observer, BPClassLoader bpClassLoader, File destDir,
            boolean suppressUnchecked, Charset fileCharset, CompileReason reason, CompileType type)
    {
        List<String> options = new ArrayList<String>();
        String optionString = Config.getPropString(Compiler.COMPILER_OPTIONS, "");
        options.addAll(Utility.dequoteCommandLine(optionString));

        if (sources != null) {
            List<CompileInputFile> inputs = Arrays.stream(sources).filter(Objects::nonNull).toList();
            if (!inputs.isEmpty()) {
                thread.addJob(new Job(inputs, javaCompiler, kotlinCompiler, observer, bpClassLoader.getClassPathAsFiles(),
                        destDir, suppressUnchecked, options, fileCharset, type, reason, type.keepClasses()));
            }
        }
    }

    /**
     * Tests whether a request contains both Kotlin and Java-compiler inputs, using the queue's extension rules.
     *
     * @param sources the selected inputs; null arrays and null entries are ignored
     * @return true if both language partitions are non-empty
     */
    @OnThread(Tag.Any)
    public static boolean isMixedLanguage(CompileInputFile[] sources)
    {
        LanguagePartition partition = partitionByLanguage(sources, true);
        return !partition.kotlinSources().isEmpty() && !partition.javaSources().isEmpty();
    }

    /**
     * Partition source files by language so each can be sent to its matching compiler.
     * <p>
     * Files whose name ends with {@code .kt} (case-insensitive) are treated as Kotlin
     * sources when a Kotlin compiler is available; everything else (including files
     * with unrecognised extensions and any null/empty input file paths) is treated as
     * Java for the Java compiler. Both BlueJ and Greenfoot provide the Kotlin compiler;
     * the unavailable-backend fallback routes all sources to Java.
     *
     * @param sources              the input files to partition
     * @param kotlinCompilerAvailable  whether a Kotlin compiler exists in this JobQueue
     * @return a {@link LanguagePartition} with the two source groups
     */
    @OnThread(Tag.Any)
    static LanguagePartition partitionByLanguage(CompileInputFile[] sources, boolean kotlinCompilerAvailable)
    {
        List<CompileInputFile> kotlinSources = new ArrayList<>();
        List<CompileInputFile> javaSources = new ArrayList<>();
        if (sources == null) {
            return new LanguagePartition(kotlinSources, javaSources);
        }
        for (CompileInputFile source : sources) {
            if (source == null) {
                continue;
            }
            File sourceFile = source.getJavaCompileInputFile();
            boolean isKotlin = kotlinCompilerAvailable
                    && sourceFile != null
                    && sourceFile.getName().toLowerCase().endsWith(".kt");
            if (isKotlin) {
                kotlinSources.add(source);
            } else {
                javaSources.add(source);
            }
        }
        return new LanguagePartition(kotlinSources, javaSources);
    }

    /**
     * Result of {@link #partitionByLanguage}: two disjoint lists of source files,
     * one for each compiler. Either list may be empty.
     */
    record LanguagePartition(List<CompileInputFile> kotlinSources, List<CompileInputFile> javaSources) { }

    /**
     * Wait until the compiler job queue is empty, then return.
     */
    public void waitForEmptyQueue()
    {
        synchronized (thread) {
            while (thread.isBusy()) {
                try {
                    thread.wait();
                }
                catch (InterruptedException ex) {}
            }
        }
    }
}
