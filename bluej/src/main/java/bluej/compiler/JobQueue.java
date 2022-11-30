/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2016  Michael Kolling and John Rosenberg
 
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
import java.util.List;

import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 * Reasonably generic interface between the BlueJ IDE and the Java compiler.
 * 
 * @author Michael Cahill
 */
public class JobQueue
{
    private static JobQueue queue = null;

    public static synchronized JobQueue getJobQueue()
    {
        if (queue == null) {
            queue = new JobQueue();
        }
        return queue;
    }

    // ---- instance ----

    private CompilerThread thread = null;
    private Compiler compiler = null;

    /**
     * Construct the JobQueue. This is private; use getJobQueue() to get the job queue instance.
     */
    private JobQueue()
    {
        compiler = new CompilerAPICompiler();
        thread = new CompilerThread();

        // Lower priority to improve GUI response time during compilation
        int priority = Thread.currentThread().getPriority() - 1;
        priority = Math.max(priority, Thread.MIN_PRIORITY);
        thread.setPriority(priority);

        thread.start();
    }

    /**
     * Adds a job to the compile queue.
     * 
     * @param sources   The files to compile
     * @param observer  Observer to be notified when compilation begins,
     *                  errors/warnings, completes
     * @param classPath The classpath to use to locate objects/source code
     * @param destDir   Destination for class files?
     * @param suppressUnchecked    Suppress "unchecked" warning in java 1.5
     */
    public void addJob(CompileInputFile[] sources, CompileObserver observer, BPClassLoader bpClassLoader, File destDir,
            boolean suppressUnchecked, Charset fileCharset, CompileReason reason, CompileType type)
    {
        List<String> options = new ArrayList<String>();
        String optionString = Config.getPropString(Compiler.COMPILER_OPTIONS, "");
        options.addAll(Utility.dequoteCommandLine(optionString));
        
        thread.addJob(new Job(sources, compiler, observer, bpClassLoader,
                destDir, suppressUnchecked, options, fileCharset, type, reason));
    }

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
