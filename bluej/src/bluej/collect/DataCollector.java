/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016  Michael Kolling and John Rosenberg
 
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

import javax.swing.SwingUtilities;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.io.File;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javafx.application.Platform;

import bluej.Boot;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.extensions.SourceType;
import bluej.pkgmgr.target.ClassTarget;
import threadchecker.OnThread;
import threadchecker.Tag;
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
public @OnThread(Tag.Swing) class DataCollector
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
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static boolean recordingThisSession;

    /**
     * Session identifier.  Never changes after startSession() has been called:
     */
    @OnThread(value = Tag.Any, requireSynchronized = true) private static String sessionUuid;
    
    /**
     * These three variables can change during the execution:
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static String uuid;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static String experimentIdentifier;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static String participantIdentifier;

    /**
     * Keep track of which error (per-session compile error sequence ids) *messages* we have shown,
     * and thus already sent an event about.
     */
    private static final BitSet shownErrorMessages = new BitSet();

    /**
     * Keep track of which error (per-session compile error sequence ids) *indicators* we have shown,
     * and thus already told the server about, either in a compiled event, or a shown_error_indicator event.
     */
    private static final BitSet shownErrorIndicators = new BitSet();

    /**
     * Keep track of which errors (per-session compile error sequence ids) we have told the server
     * have been created.  Due to threading back and forths, it is possible for us to be told that
     * error indicators have been shown before we've been told about the compile event that generated them.
     */
    private static final BitSet createdErrors = new BitSet();
    
    
    /**
     * Checks whether we should send data.  This takes into account whether we
     * are in Greenfoot, and opt-in status.  It doesn't check whether we have stopped
     * sending due to connection problems -- DataSubmitter keeps track of that.
     */
    @OnThread(Tag.Any)
    private static synchronized boolean dontSend()
    {
        return (Config.isGreenfoot() && !Boot.isTrialRecording()) || !recordingThisSession;
    }

    @OnThread(Tag.FXPlatform)
    private static synchronized void startSession()
    {
        // Look for an existing UUID:
        uuid = Config.getPropString(PROPERTY_UUID, null);
        
        // If there is no UUID in the file, or it's invalid, ask them if they want to opt in or opt out:
        if (!(OPT_OUT.equals(uuid)) && !uuidValidForRecording() )
        {
            changeOptInOut(Boot.isTrialRecording());
        }

        // Temporarily for 4.0.0-preview, do not send to Blackbox:
        recordingThisSession = false; //uuidValidForRecording();
        
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
    @OnThread(Tag.Any)
    private static synchronized boolean uuidValidForRecording()
    {
        return uuid != null && !(OPT_OUT.equals(uuid)) && uuid.length() >= 32;
    }

    /**
     * Show a dialog to ask the user for their opt-in/opt-out preference,
     * and then update the UUID accordingly
     */
    @OnThread(Tag.FXPlatform)
    public static synchronized void changeOptInOut(boolean forceOptIn)
    {
        boolean optedIn;
        if (forceOptIn)
        {
            optedIn = true;
        }
        else
        {
            DataCollectionDialog dlg = new DataCollectionDialog();
            optedIn = dlg.showAndWait().orElse(false);
        }

        if (optedIn)
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
    @OnThread(Tag.Any)
    public static synchronized String getExperimentIdentifier()
    {
        return experimentIdentifier;
    }
    
    /**
     * Get the participant identifier.
     */
    @OnThread(Tag.Any)
    public static synchronized String getParticipantIdentifier()
    {
        return participantIdentifier;
    };
    
    /**
     * Get the session identifier.
     */
    @OnThread(Tag.Any)
    public static synchronized String getSessionUuid()
    {
        return sessionUuid;
    }
    
    /**
     * Gets a String to display to the user in the preferences, explaining their
     * current opt-in/recording status
     */
    @OnThread(Tag.Any)
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

    @OnThread(Tag.FXPlatform)
    public static void bluejOpened(String osVersion, String javaVersion, String bluejVersion, String interfaceLanguage, List<ExtensionWrapper> extensions)
    {
        if (Config.isGreenfoot() && !Boot.isTrialRecording()) return;
        startSession();
        if (dontSend()) return;
        SwingUtilities.invokeLater(() -> DataCollectorImpl.bluejOpened(osVersion, javaVersion, bluejVersion, interfaceLanguage, extensions));
    }
    
    public static void bluejClosed()
    {
        if (dontSend()) return;
        DataCollectorImpl.bluejClosed();
    }
    
    public static void compiled(Project proj, Package pkg, CompileInputFile[] sources, List<DiagnosticWithShown> diagnostics, boolean success, CompileReason reason, SourceType inputType)
    {
        if (dontSend()) return;
        diagnostics.forEach(dws -> {
            // If the error was shown to the user, store that in our set.  Conversely,
            // if we have been told already that the error has been shown to the user,
            // we want to reflect that in the event
            if (dws.wasShownToUser())
                shownErrorIndicators.set(dws.getDiagnostic().getIdentifier());
            else if (shownErrorIndicators.get(dws.getDiagnostic().getIdentifier()))
                dws.markShownToUser();

            createdErrors.set(dws.getDiagnostic().getIdentifier());
        });
        DataCollectorImpl.compiled(proj, pkg, sources, diagnostics, success, reason, inputType);
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

    public static void addClass(Package pkg, ClassTarget ct)
    {
        if (dontSend()) return;
        DataCollectorImpl.addClass(pkg, ct);
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

    public static void openClass(Package pkg, File sourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.openClass(pkg, sourceFile);
    }

    public static void closeClass(Package pkg, File sourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.closeClass(pkg, sourceFile);
    }

    public static void selectClass(Package pkg, File sourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.selectClass(pkg, sourceFile);
    }
    
    public static void convertStrideToJava(Package pkg, File oldSourceFile, File newSourceFile)
    {
        if (dontSend()) return;
        DataCollectorImpl.convertStrideToJava(pkg, oldSourceFile, newSourceFile);
    }

    public static void edit(Package pkg, File path, String source, boolean includeOneLineEdits, StrideEditReason reason)
    {
        if (dontSend()) return;
        DataCollectorImpl.edit(pkg, path, source, includeOneLineEdits, reason);
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

    public static void showErrorIndicator(Package pkg, int errorIdentifier)
    {
        if (dontSend()) return;
        // Already know about this:
        if (shownErrorIndicators.get(errorIdentifier))
            return;

        if (createdErrors.get(errorIdentifier))
        {
            // Creation has already been sent, so fine to follow it up with a shown event:
            DataCollectorImpl.showErrorIndicator(pkg, errorIdentifier);
        }
        // Otherwise, we haven't sent the creation yet, so do nothing but flag it in the bitset
        shownErrorIndicators.set(errorIdentifier);
    }

    public static void showErrorMessage(Package pkg, int errorIdentifier, List<String> quickFixes)
    {
        if (dontSend()) return;
        // Only send an event for each error the first time it is shown:
        if (shownErrorMessages.get(errorIdentifier))
            return;
        shownErrorMessages.set(errorIdentifier);
        DataCollectorImpl.showErrorMessage(pkg, errorIdentifier, quickFixes);
    }

    public static void fixExecuted(Package aPackage, int errorIdentifier, int fixIndex)
    {
        if (dontSend()) return;
        DataCollectorImpl.fixExecuted(aPackage, errorIdentifier, fixIndex);
    }

    public static void recordGreenfootEvent(Project project, GreenfootInterfaceEvent event)
    {
        if (dontSend()) return;
        DataCollectorImpl.greenfootEvent(project, project.getPackage(""), event);
    }

    public static void codeCompletionStarted(ClassTarget ct, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem)
    {
        if (dontSend()) return;
        DataCollectorImpl.codeCompletionStarted(ct.getPackage().getProject(), ct.getPackage(), lineNumber, columnNumber, xpath, subIndex, stem);
    }

    public static void codeCompletionEnded(ClassTarget ct, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, String replacement)
    {
        if (dontSend()) return;
        DataCollectorImpl.codeCompletionEnded(ct.getPackage().getProject(), ct.getPackage(), lineNumber, columnNumber, xpath, subIndex, stem, replacement);
    }

    public static void unknownFrameCommandKey(ClassTarget ct, String enclosingFrameXpath, int cursorIndex, char key)
    {
        if (dontSend()) return;
        DataCollectorImpl.unknownFrameCommandKey(ct.getPackage().getProject(), ct.getPackage(), enclosingFrameXpath, cursorIndex, key);
    }

    public static boolean hasGivenUp()
    {
        return DataSubmitter.hasGivenUp();
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
