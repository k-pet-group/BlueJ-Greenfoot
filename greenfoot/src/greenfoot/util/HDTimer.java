/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2012,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Timer to do high precision sleeps and waits.
 * 
 * @author Poul Henriksen
 */
public class HDTimer
{
    private static Long sleepPrecision;
    private static long worstYieldTime;
    private static boolean inited;
    private static Long waitPrecision;

    static {
        init();
    }

    public synchronized static void init()
    {
        if (!inited) {
            measureSleepPrecision();
            measureWaitPrecision();
            inited = true;
        }
    }

    private static void measureSleepPrecision()
    {
        int testSize = 11;
        List<Long> tests = new ArrayList<Long>();

        try {
            for (int i = 0; i < testSize; i++) {
                long t1 = System.nanoTime();
                Thread.sleep(0, 1);
                long t2 = System.nanoTime();
                tests.add((t2 - t1));
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        Collections.sort(tests);
        sleepPrecision = tests.get(testSize / 2);
    }

    private static void measureWaitPrecision()
    {
        int testSize = 11;
        List<Long> tests = new ArrayList<Long>();
        Object lock = new Object();
        try {
            synchronized (lock) {
                for (int i = 0; i < testSize; i++) {
                    long t1 = System.nanoTime();
                    lock.wait(0, 1);
                    long t2 = System.nanoTime();
                    tests.add((t2 - t1));
                }
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        Collections.sort(tests);
        waitPrecision = tests.get(testSize / 2);
    }

    /**
     * Sleep for the specified amount of time.
     * 
     * @param nanos
     *            Time to wait in nanoseconds.
     * @throws InterruptedException
     *             if another thread has interrupted the current thread
     */
    public static void sleep(long nanos)
        throws InterruptedException
    {
        long tStart = System.nanoTime();
        sleepFromTime(nanos, tStart);
    }

    /**
     * Sleep for the specified amount of time.
     * 
     * @param nanos
     *            Time to wait in nanoseconds.
     * @param tStart The tiem from which the wainting should start.
     * 
     * @throws InterruptedException
     *             if another thread has interrupted the current thread
     */
    private static void sleepFromTime(long nanos, long tStart)
        throws InterruptedException
    {
        long sleepNanos = nanos - sleepPrecision;

        // First, use Java's Thread.sleep() if it is precise enough
        if (nanos / sleepPrecision >= 2) {
            long actualDelayMillis = (sleepNanos) / 1000000L;
            int nanoRest = (int) (sleepNanos % 1000000L);
            if(Thread.interrupted()) {
                throw new InterruptedException("HDTimer.sleepFromTime interrupted in sleep.");
            }
            Thread.sleep(actualDelayMillis, nanoRest);
        }

        // Second, yield in a busy loop if precise enough
        while ((System.nanoTime() - tStart + worstYieldTime) < nanos) {
            long t1 = System.nanoTime();
            if(Thread.interrupted()) {
                throw new InterruptedException("HDTimer.sleepFromTime interrupted in yield.");
            }
            Thread.yield();
            long yieldTime = System.nanoTime() - t1;
            if (yieldTime > worstYieldTime) {
                worstYieldTime = yieldTime;
            }
        }

        // Third, run a busy loop for the rest of the time
        while ((System.nanoTime() - tStart) < nanos) {
            if(Thread.interrupted()) {
                throw new InterruptedException("HDTimer.sleepFromTime interrupted in busy loop.");
            }
        }
    }
}
