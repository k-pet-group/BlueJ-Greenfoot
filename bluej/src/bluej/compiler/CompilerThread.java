/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.utility.Queue;

/**
 * The compiler thread. BlueJ uses exactly one thread for compilation. Jobs are
 * queued, and this thread processes tham one by one. If there is no job, this
 * thread just sleeps.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
class CompilerThread extends Thread
{
    private Queue jobs;
    private boolean busy = true;

    /**
     * Create a new compiler thread that holds its own job queue.
     */
    public CompilerThread()
    {
        super(Config.getString("compiler.thread.title"));
        jobs = new Queue();
    }

    /**
     * Start running this thread. The compiler thread will run infinitely in a
     * loop. It will compile jobs as long as there are any jobs pending, and
     * then wait for new jobs to be scheduled. New jobs are scheduled using the
     * addJob method.
     */
    public void run()
    {
        Job job;
        while (true) {
            synchronized (this) {
                while ((job = (Job) jobs.dequeue()) == null) {
                    busy = false;
                    notifyAll();
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {}
                }
            }

            job.compile();
        }
    }

    /**
     * Add a new job to this thread's job queue. The job will be processed by
     * this thread some tim ein the near future. This method returns
     * immediately.
     */
    @OnThread(Tag.Any)
    public synchronized void addJob(Job job)
    {
        jobs.enqueue(job);
        busy = true;
        notifyAll();
    }

    @OnThread(Tag.Any)
    public boolean isBusy()
    {
        return busy;
    }
}