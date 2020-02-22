/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016,2020  Michael Kolling and John Rosenberg
 
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

/**
 * A compiler "job". A list of filenames to compile + parameters.
 * Jobs are held in a queue by the CompilerThread, which compiles them
 * by running the job's "compile" method.
 *
 * @author  Michael Cahill
 */
class Job
{
    Compiler compiler;  // The compiler for this job
    CompileObserver observer;
    File destDir;
    BPClassLoader bpClassLoader;
    CompileInputFile sources[];
    boolean internal; // true for compiling shell files, 
                      // or user files if we want to suppress 
                      // "unchecked" warnings, false otherwise
    private List<String> userCompileOptions;
    private Charset fileCharset;
    private CompileType type;
    private CompileReason reason;

    /**
     * Generator for unique ascending compilation identifiers.  It doesn't matter if it's shared between
     * packages or between projects, it just needs to be unique and ascending.  An individual user won't manage
     * 2 billion compilations in a single session, so integer is fine:
     */
    private static final AtomicInteger nextCompilationSequence = new AtomicInteger(1);
    
    /**
     * Create a job with a set of sources.
     */
    public Job(CompileInputFile[] sourceFiles, Compiler compiler, CompileObserver observer,
                        BPClassLoader bpClassLoader, File destDir, boolean internal,
                        List<String> userCompileOptions, Charset fileCharset, CompileType type, CompileReason reason)
    {
        this.sources = sourceFiles;
        this.compiler = compiler;
        this.observer = observer;
        this.bpClassLoader = bpClassLoader;
        this.destDir = destDir;
        this.internal = internal;
        this.userCompileOptions = userCompileOptions;
        this.fileCharset = fileCharset;
        this.type = type;
        this.reason = reason;
    }
    
    /**
     * Compile this job
     */
    public void compile()
    {
        int compilationSequence = nextCompilationSequence.getAndIncrement();

        try {
            if(observer != null) {
                observer.startCompile(sources, reason, type, compilationSequence);
            }

            if(destDir != null) {
                compiler.setDestDir(destDir);
            }

            compiler.setClasspath(bpClassLoader.getClassPathAsFiles());

            compiler.setBootClassPath(null);
            String majorVersion = System.getProperty("java.specification.version");
            userCompileOptions.add(0, "-source");
            userCompileOptions.add(1, majorVersion);

            File[] actualSourceFiles = new File[sources.length];
            for (int i = 0; i < sources.length; i++)
            {
                actualSourceFiles[i] = sources[i].getJavaCompileInputFile();
            }

            boolean successful = compiler.compile(actualSourceFiles, observer, internal, userCompileOptions, fileCharset, type);

            if(observer != null) {
                observer.endCompile(sources, successful, type, compilationSequence);
            }
        } catch(Exception e) {
            System.err.println(Config.getString("compileException") + ": " + e);
            e.printStackTrace();
            if (observer != null) {
                observer.endCompile(sources, false, type, compilationSequence);
            }
        }
    }
}
