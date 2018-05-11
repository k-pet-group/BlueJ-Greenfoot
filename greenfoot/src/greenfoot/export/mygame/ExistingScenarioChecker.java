/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.export.mygame;

import bluej.utility.FXWorker;
import java.io.IOException;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Abstract class that can be used to check (asynchronously) whether a scenario already exists on the
 * publish site.
 * 
 * @author Poul Henriksen
 */
public abstract class ExistingScenarioChecker
{
    /** Worker to handle asynchronously checking of existing scenario */
    private FXWorker worker;

    /** Are we in the process of checking? */
    private boolean checking = false;

    /** Indicates that the user wants to abort checking for this scenario. */
    private boolean abort = false;

    /** Indicates that the checking has finished. */
    private boolean finished = false;

    private volatile String hostName;
    private volatile String userName;
    private volatile String scenarioName;

    class ScenarioWorker extends FXWorker
    {
        @Override
        @OnThread(Tag.Worker)
        public Object construct()
        {
            return checkExistence(hostName, userName, scenarioName);
        }

        @Override
        public void finished()
        {
            workerFinished(getValue());
        }

        @Override
        public void abort()
        {
            ExistingScenarioChecker.this.abort();
        }
    }

    /**
     * Will start a thread that checks whether a scenario with the given name
     * exists for the given user. When a result is ready the method
     * scenarioExistenceChecked will be called. The result for the given host / user / scenario
     * combination may be cached.
     */
    public void startScenarioExistenceCheck(final String hostName, final String userName,
            final String scenarioName)
    {
        synchronized (this)
        {
            boolean sameScenario = hostName.equals(this.hostName) && userName.equals(this.userName)
                    && scenarioName.equals(this.scenarioName);
            if (sameScenario)
            {
                return;
            }
            if (checking)
            {
                // Abort the current check in preparation for the new one.
                abort();
            }

            this.hostName = hostName;
            this.userName = userName;
            this.scenarioName = scenarioName;

            checking = true;
            abort = false;
            finished = false;
            worker = new ScenarioWorker();
            worker.start();
        }
    }

    /**
     * Will abort the checking.
     * 
     * @return True if successful abort, false if we didn't manage to abort
     *         (because it already finished the check)
     *         
     * @throws IllegalStateException If the check has not started yet.
     */
    public synchronized boolean abort()
    {
        // pre: is checking
        if (finished)
        {
            return false;
        }

        if (!checking)
        {
            throw new IllegalStateException("Check not started yet. Nothing to abort.");
        }

        abort = true;
        worker.interrupt();
        return true;
    }

    /**
     * Method that will be called when the check has finished.
     * 
     * This will execute on the FX event thread.
     * 
     * @param info If the scenario exists info about it will be returned. If it
     *            does not exist it will return null.
     */
    @OnThread(Tag.FXPlatform)
    public abstract void scenarioExistenceChecked(ScenarioInfo info);

    /**
     * Method that will be called if a check fails. This can be because if a
     * network error or other things. This will execute on the FX event
     * thread.
     * 
     * @param reason The exception fired when the check is failed.
     */
    @OnThread(Tag.FXPlatform)
    public abstract void scenarioExistenceCheckFailed(Exception reason);

    /**
     * Checks the existence of the given scenario.
     */
    @OnThread(Tag.Worker)
    private Object checkExistence(final String hostName, final String userName, final String scenarioName)
    {
        MyGameClient client = new MyGameClient(null);
        ScenarioInfo info = new ScenarioInfo();
        try
        {
            if (client.checkExistingScenario(hostName, userName, scenarioName, info))
            {
                return info;
            }
            return null;
        }
        catch (IOException e)
        {
            return e;
        }
    }

    /**
     * Called when the worker has finished and the result is ready to be
     * processed.
     */
    @OnThread(Tag.FXPlatform)
    private synchronized void workerFinished(Object value)
    {
        finished = true;
        checking = false;

        if (!abort)
        {
            if (value instanceof Exception)
            {
                scenarioExistenceCheckFailed((Exception) value);
            }
            else
            {
                scenarioExistenceChecked((ScenarioInfo) value);
            }
        }
    }
}
