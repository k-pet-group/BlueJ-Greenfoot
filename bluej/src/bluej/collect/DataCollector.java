/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.extmgr.ExtensionWrapper;
import bluej.groupwork.Repository;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;

/**
 * DataCollector for sending off data.
 * 
 * You can call these methods under any setting. It is this class's responsibility to check:
 *  - That the user has actually opted in
 *  - That we're not running Greenfoot
 *  - That we don't keep attempting to send when there's no net connection (actually, DataSubmitter checks this for us)
 *  
 * This class mainly acts as a proxy for the DataCollectorImpl class, which implements the actual
 * collection logic.
 */
public class DataCollector
{
    private static final String PROPERTY_UUID = "blackbox.uuid";
    private static final String PROPERTY_EXPERIMENT = "blackbox.experiment";
    private static final String PROPERTY_PARTICIPANT = "blackbox.participant";
    private static final String OPT_OUT = "optout";
    
    /**
     * We decide at the very beginning of the session whether we are recording, based
     * on whether the user was opted in.  Starting to record mid-session is fairly
     * useless, so even if the user opts in during the session, we won't record
     * their data until the next session begins.  Thus, this variable
     * will never become true after startSession() has been called, althoug
     * it may become false if the user opts out mid-session
     */
    private static boolean recordingThisSession;

    /**
     * Session identifier.  Never changes after startSession() has been called:
     */
    private static String sessionUuid;
    
    /**
     * These three variables can change during the execution:
     */
    private static String uuid;
    private static String experimentIdentifier;
    private static String participantIdentifier;
    
    
    /**
     * Checks whether we should send data.  This takes into account whether we
     * are in Greenfoot, and opt-in status.  It doesn't check whether we have stopped
     * sending due to connection problems -- DataSubmitter keeps track of that.
     */
    private static synchronized boolean dontSend()
    {
        return Config.isGreenfoot() || !recordingThisSession;
    }

    private static synchronized void startSession()
    {
        // Look for an existing UUID:
        uuid = Config.getPropString(PROPERTY_UUID, null);
        
        // If there is no UUID in the file, or it's invalid, ask them if they want to opt in or opt out:
        if (!(OPT_OUT.equals(uuid)) && !uuidValidForRecording() )
        {
            changeOptInOut();
        }
        
        recordingThisSession = uuidValidForRecording();
        
        if (recordingThisSession)
        {
            // Initialise the session:
            sessionUuid = UUID.randomUUID().toString();
        }
        
        // We fetch these regardless, so that everything is consistent
        // if the user opts in and edits them mid-session:
        experimentIdentifier = Config.getPropString(PROPERTY_EXPERIMENT, null);
        participantIdentifier = Config.getPropString(PROPERTY_PARTICIPANT, null);
    }

    /**
     * Checks if the user's UUID would be valid for recording, which is another way
     * of asking if the user has opted in.  However, this is not the same as "are we currently
     * sending data for this user", because if they opt-in mid-session, this method
     * will return true, but dontSend() will also return true (because recordingThisSession
     * will be false; it is set once at the very beginning of the session).
     */
    private static synchronized boolean uuidValidForRecording()
    {
        return uuid != null && !(OPT_OUT.equals(uuid)) && uuid.length() >= 32;
    }

    /**
     * Show a dialog to ask the user for their opt-in/opt-out preference,
     * and then update the UUID accordingly
     */
    public static synchronized void changeOptInOut()
    {
        DataCollectionDialog dlg = new DataCollectionDialog();
        dlg.setLocationRelativeTo(null); // Centre on screen
        dlg.setVisible(true);
        
        if (dlg.optedIn())
        {
            // Only generate new UUID if didn't have one already:
            if (!uuidValidForRecording())
            {
                uuid = UUID.randomUUID().toString();
            }
        }
        else
        {
            uuid = OPT_OUT;
            recordingThisSession = false;
        }
        Config.putPropString(PROPERTY_UUID, uuid);
    }
    
    /**
     * Gets the user's UUID
     */
    public static synchronized String getUserID()
    {
        return uuid;
    }
    
    /**
     * Get the experiment identifier.
     */
    public static String getExperimentIdentifier()
    {
        return experimentIdentifier;
    }
    
    /**
     * Get the participant identifier.
     */
    public static String getParticipantIdentifier()
    {
        return participantIdentifier;
    };
    
    /**
     * Get the session identifier.
     */
    public static String getSessionUuid()
    {
        return sessionUuid;
    }
    
    /**
     * Gets a String to display to the user in the preferences, explaining their
     * current opt-in/recording status
     */
    public static synchronized String getOptInOutStatus()
    {
        if (recordingThisSession)
        {
            return Config.getString("collect.status.optedin");
        }
        else if (uuidValidForRecording())
        {
            return Config.getString("collect.status.nextsession");
        }
        else
        {
            return Config.getString("collect.status.optedout");
        }
    }

    public static synchronized void setExperimentIdentifier(String experimentIdentifier)
    {
        DataCollector.experimentIdentifier = experimentIdentifier;
        Config.putPropString(PROPERTY_EXPERIMENT, experimentIdentifier);
    }

    public static synchronized void setParticipantIdentifier(String participantIdentifier)
    {
        DataCollector.participantIdentifier = participantIdentifier;
        Config.putPropString(PROPERTY_PARTICIPANT, participantIdentifier);
    }
    
    public static void bluejOpened(String osVersion, String javaVersion, String bluejVersion, String interfaceLanguage, List<ExtensionWrapper> extensions)
    {
        if (Config.isGreenfoot()) return;
        startSession();
        if (dontSend()) return;
        DataCollectorImpl.bluejOpened(osVersion, javaVersion, bluejVersion, interfaceLanguage, extensions);
    }
    
    public static void bluejClosed()
    {
        if (dontSend()) return;
        DataCollectorImpl.bluejClosed();
    }
    
    public static void compiled(Project proj, Package pkg, File[] sources, List<DiagnosticWithShown> diagnostics, boolean success)
    {
        if (dontSend()) return;
        DataCollectorImpl.compiled(proj, pkg, sources, diagnostics, success);
    }

    public static void debuggerTerminate(Project project)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerTerminate(project);
    }
    
    public static void debuggerChangeVisible(Project project, boolean newVis)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerChangeVisible(project, newVis);
    }
    
    public static void debuggerContinue(Project project, String threadName)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerContinue(project, threadName);
    }

    public static void debuggerHalt(Project project, String threadName, SourceLocation[] stack)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerHalt(project, threadName, stack);
    }
    
    public static void debuggerStepInto(Project project, String threadName, SourceLocation[] stack)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerStepInto(project, threadName, stack);
    }
    
    public static void debuggerStepOver(Project project, String threadName, SourceLocation[] stack)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerStepOver(project, threadName, stack);
    }
    
    public static void debuggerHitBreakpoint(Project project, String threadName, SourceLocation[] stack)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerHitBreakpoint(project, threadName, stack);
    }

    public static void invokeCompileError(Package pkg, String code, String compilationError)
    {
        if (dontSend()) return;
        DataCollectorImpl.invokeCompileError(pkg, code, compilationError);
    }
    
    public static void invokeMethodSuccess(Package pkg, String code, String objName, String typeName, int testIdentifier, int invocationIdentifier)
    {
        if (dontSend()) return;
        DataCollectorImpl.invokeMethodSuccess(pkg, code, objName, typeName, testIdentifier, invocationIdentifier);
    }
    
    public static void invokeMethodException(Package pkg, String code, ExceptionDescription ed)
    {
        if (dontSend()) return;
        DataCollectorImpl.invokeMethodException(pkg, code, ed);
    }
    
    public static void invokeMethodTerminated(Package pkg, String code)
    {
        if (dontSend()) return;
        DataCollectorImpl.invokeMethodTerminated(pkg, code);
    }

    public static void assertTestMethod(Package pkg, int testIdentifier, int invocationIdentifier, 
            String assertion, String param1, String param2)
    {
        if (dontSend()) return;
        DataCollectorImpl.assertTestMethod(pkg, testIdentifier, invocationIdentifier, assertion, param1, param2);
    }

    public static void removeObject(Package pkg, String name)
    {
        if (dontSend()) return;
        DataCollectorImpl.removeObject(pkg, name);
    }

    public static void codePadSuccess(Package pkg, String command, String output)
    {
        if (dontSend()) return;
        DataCollectorImpl.codePadSuccess(pkg, command, output);
    }

    public static void codePadError(Package pkg, String command, String error)
    {
        if (dontSend()) return;
        DataCollectorImpl.codePadError(pkg, command, error);
    }

    public static void codePadException(Package pkg, String command, String exception)
    {
        if (dontSend()) return;
        DataCollectorImpl.codePadException(pkg, command, exception);
    }

    public static void teamCommitProject(Project project, Repository repo, Set<File> committedFiles)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamCommitProject(project, repo, committedFiles);
    }

    public static void teamShareProject(Project project, Repository repo)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamShareProject(project, repo);
    }

    public static void addClass(Package pkg, File sourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.addClass(pkg, sourceFile);
    }

    public static void teamUpdateProject(Project project, Repository repo, Set<File> updatedFiles)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamUpdateProject(project, repo, updatedFiles);
    }

    public static void teamHistoryProject(Project project, Repository repo)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamHistoryProject(project, repo);
    }

    public static void teamStatusProject(Project project, Repository repo, Map<File, String> status)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamStatusProject(project, repo, status);
    }

    public static void debuggerBreakpointToggle(Package pkg, File sourceFile, int lineNumber, boolean newState)
    {
        if (dontSend()) return;
        DataCollectorImpl.debuggerBreakpointToggle(pkg, sourceFile, lineNumber, newState);
    }

    public static void renamedClass(Package pkg, File oldSourceFile, File newSourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.renamedClass(pkg, oldSourceFile, newSourceFile);
    }

    public static void removeClass(Package pkg, File sourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.removeClass(pkg, sourceFile);
    }

    public static void edit(Package pkg, File path, String source, boolean includeOneLineEdits)
    {
        if (dontSend()) return;
        DataCollectorImpl.edit(pkg, path, source, includeOneLineEdits);
    }

    public static void packageOpened(Package pkg)
    {
        if (dontSend()) return;
        DataCollectorImpl.packageOpened(pkg);
    }

    public static void packageClosed(Package pkg)
    {
        if (dontSend()) return;
        DataCollectorImpl.packageClosed(pkg);
    }

    public static void benchGet(Package pkg, String benchName, String typeName, int testIdentifier)
    {
        if (dontSend()) return;
        DataCollectorImpl.benchGet(pkg, benchName, typeName, testIdentifier);        
    }

    public static void endTestMethod(Package pkg, int testIdentifier)
    {
        if (dontSend()) return;
        DataCollectorImpl.endTestMethod(pkg, testIdentifier);        
    }

    public static void cancelTestMethod(Package pkg, int testIdentifier)
    {
        if (dontSend()) return;
        DataCollectorImpl.cancelTestMethod(pkg, testIdentifier);        
    }

    public static void startTestMethod(Package pkg, int testIdentifier,
            File sourceFile, String testName)
    {
        if (dontSend()) return;
        DataCollectorImpl.startTestMethod(pkg, testIdentifier, sourceFile, testName);        
    }

    public static void restartVM(Project project)
    {
        if (dontSend()) return;
        DataCollectorImpl.restartVM(project);        
    }

    public static void testResult(Package pkg, DebuggerTestResult lastResult)
    {
        if (dontSend()) return;
        DataCollectorImpl.testResult(pkg, lastResult);        
    }

    public static void projectOpened(Project proj, List<ExtensionWrapper> projectExtensions)
    {
        if (dontSend()) return;
        DataCollectorImpl.projectOpened(proj, projectExtensions);        
    }

    public static void projectClosed(Project proj)
    {
        if (dontSend()) return;
        DataCollectorImpl.projectClosed(proj);        
    }

    public static void inspectorObjectShow(Package pkg,
            ObjectInspector inspector, String benchName, String className,
            String displayName)
    {
        if (dontSend()) return;
        DataCollectorImpl.inspectorObjectShow(pkg, inspector, benchName, className, displayName);        
    }

    public static void inspectorHide(Project project, Inspector inspector)
    {
        if (dontSend()) return;
        DataCollectorImpl.inspectorHide(project, inspector);        
    }

    public static void inspectorClassShow(Package pkg, ClassInspector inspector, String className)
    {
        if (dontSend()) return;
        DataCollectorImpl.inspectorClassShow(pkg, inspector, className);        
    }

    public static class NamedTyped
    {
        private  String name;
        private  String type;
        
        public NamedTyped(String name, String type)
        {
            this.name = name;
            this.type = type;
        }
        public String getName()
        {
            return name;
        }
        public String getType()
        {
            return type;
        }
    }

    public static void fixtureToObjectBench(Package pkg, File sourceFile,
            List<NamedTyped> objects)
    {
        if (dontSend()) return;
        DataCollectorImpl.fixtureToObjectBench(pkg, sourceFile, objects);        
    }

    public static void objectBenchToFixture(Package pkg, File sourceFile,
            List<String> benchNames)
    {
        if (dontSend()) return;
        DataCollectorImpl.objectBenchToFixture(pkg, sourceFile, benchNames);        
    }

    public static void showHideTerminal(Project project, boolean show)
    {
        if (dontSend()) return;
        DataCollectorImpl.showHideTerminal(project, show);        
    }
}
