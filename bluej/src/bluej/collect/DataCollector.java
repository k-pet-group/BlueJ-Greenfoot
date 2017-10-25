/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
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

import bluej.Boot;
import bluej.Config;
import bluej.collect.DataCollectorImpl.EditedFileInfo;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.editor.stride.FrameCatalogue;
import bluej.extmgr.ExtensionWrapper;
import bluej.groupwork.Repository;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.stride.generic.Frame;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.*;

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
@OnThread(Tag.FXPlatform)
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
        DataCollectorImpl.bluejOpened(osVersion, javaVersion, bluejVersion, interfaceLanguage, extensions);
    }
    
    public static void bluejClosed()
    {
        if (dontSend()) return;
        DataCollectorImpl.bluejClosed();
    }

    /**
     * Record a compile event.  This may be a javac compile, but it may also be a Stride early or late error check.
     *
     *
     * @param proj The project involved in the compilation
     * @param pkg The package involved in the compilation.  Due to BlueJ's design, we only ever compile one package at a time.
     *            May be null if it cannot be determined for some reason
     * @param sources The collection of files fed to the compilation as input.
     * @param diagnostics The diagnostics (i.e. errors and warnings) which were generated
     *                    as a result of the compile.  May be empty, especially if compile was successful.
     * @param success Was the compile a success?
     * @param reason The reason for performing the compilation.  This is recorded on the server
     * @param compilationSequence A sequence identifier (unique within this session) of the original trigger of the compilation event.
     *                            If we compile Stride, we may get an early, normal and late compilation result passed to three
     *                            separate calls of this method, but they will all have the same compilationSequence
     *                            value, to allow them to be reassembled by a later researcher.  The special value -1
     *                            means it is non-applicable or unknown, and in this case will not be sent to the server.
     */
    public static void compiled(Project proj, Package pkg, CompileInputFile[] sources, List<DiagnosticWithShown> diagnostics, boolean success, CompileReason reason, int compilationSequence)
    {
        if (dontSend()) return;

        // This bitset will be filled with all errors which we've already been told are shown,
        // but which we couldn't send to the server because we hadn't yet sent the compilation
        // event which notifies the server of their creation:
        Set<Integer> pendingShow = new HashSet<>();

        for (DiagnosticWithShown dws : diagnostics)
        {
            // If we have been told already that the error has been shown to the user,
            // we want to send a new event afterwards with the shown:
            // Or, if it's just been shown now:
            if (shownErrorIndicators.get(dws.getDiagnostic().getIdentifier()) || dws.wasShownToUser())
            {
                pendingShow.add(dws.getDiagnostic().getIdentifier());
                shownErrorIndicators.set(dws.getDiagnostic().getIdentifier());
            }

            createdErrors.set(dws.getDiagnostic().getIdentifier());
        }
        DataCollectorImpl.compiled(proj, pkg, sources, diagnostics, success, reason, compilationSequence);
        if (!pendingShow.isEmpty())
        {
            DataCollectorImpl.showErrorIndicators(pkg, pendingShow);
        }
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

    /**
     * Records a VCS push event
     * @param project The project which is in VCS
     * @param repo The repository object for VCS
     * @param pushedFiles The files involved in the push
     */
    public static void teamPushProject(Project project, Repository repo, Set<File> pushedFiles)
    {
        if (dontSend()) return;
        DataCollectorImpl.teamPushProject(project, repo, pushedFiles);
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

    /**
     * Records renaming a class (Java, or Stride and its generated Java).
     *
     * @param pkg                The package in which the files live.
     * @param oldFrameSourceFile The original Stride source file that has been deleted,
     *                           or <code>null</code> in case the source type is Java.
     * @param newFrameSourceFile The new created Stride source file, or <code>null</code> in case the source type is Java.
     * @param oldJavaSourceFile  The original Java source file that has been deleted.
     * @param newJavaSourceFile  The new created Java source file.
     */
    public static void renamedClass(Package pkg, File oldFrameSourceFile, File newFrameSourceFile, File oldJavaSourceFile, File newJavaSourceFile)
    {
        if (dontSend()) {
            return;
        }
        DataCollectorImpl.renamedClass(pkg, oldFrameSourceFile, newFrameSourceFile, oldJavaSourceFile, newJavaSourceFile);
    }

    /**
     * Records removing class files.
     *
     * @param pkg              The package in which the files live.
     * @param frameSourceFile  The Stride source file, or <code>null</code> in case the source type is Java.
     * @param javaSourceFile   The Java source file.
     */
    public static void removeClass(Package pkg, File frameSourceFile, File javaSourceFile)
    {
        if (dontSend()) {
            return;
        }
        DataCollectorImpl.removeClass(pkg, frameSourceFile, javaSourceFile);
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

    /**
     * Record conversion from Stride to Java.
     * @param pkg The package in which the files live.
     * @param oldSourceFile The old Stride source file (which will be deleted by the caller)
     * @param newSourceFile The Java source file (which will be retained by the caller)
     */
    public static void convertStrideToJava(Package pkg, File oldSourceFile, File newSourceFile)
    {
        if (dontSend()) return;
        // Note the function takes Java, Stride rather than old or new.  Here, new is Java:
        DataCollectorImpl.conversion(pkg, newSourceFile, oldSourceFile, true);
    }

    /**
     * Record conversion from Java to Stride.
     * @param pkg The package in which the files live.
     * @param oldSourceFile The old Java source file (which will now be generated, rather than edited directly)
     * @param newSourceFile The new Stride source file (which will now be edited and used to generate the Java)
     */
    public static void convertJavaToStride(Package pkg, File oldSourceFile, File newSourceFile)
    {
        if (dontSend()) return;
        // Note the function takes Java, Stride rather than old or new.  Here, old is Java:
        DataCollectorImpl.conversion(pkg, oldSourceFile, newSourceFile, false);
    }


    public static void editJava(Package pkg, File path, String source, boolean includeOneLineEdits)
    {
        if (dontSend()) return;
        DataCollectorImpl.edit(pkg, Collections.singletonList(new EditedFileInfo("diff", path, source, includeOneLineEdits, null, null)));
    }

    public static void editStride(Package pkg, File javaPath, String javaSource, File stridePath, String strideSource, StrideEditReason reason)
    {
        if (dontSend()) return;
        DataCollectorImpl.edit(pkg, Arrays.asList(
            new EditedFileInfo("diff_generated", javaPath, javaSource, true, stridePath, null),
            new EditedFileInfo("diff", stridePath, strideSource, true, null, reason)
        ));
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

    /**
     * A class inspector was shown.
     * @param proj  The project associated with the action
     * @param pkg   The package associated with the action; may be null
     * @param inspector  The inspector shown
     * @param className  The name of the class associated with the inspector
     */
    public static void inspectorClassShow(Project proj, Package pkg, ClassInspector inspector, String className)
    {
        if (dontSend()) return;
        DataCollectorImpl.inspectorClassShow(proj, pkg, inspector, className);        
    }

    public static void showErrorIndicators(Package pkg, Collection<Integer> errorIdentifiers)
    {
        if (dontSend()) return;

        HashSet<Integer> previouslyUnshown = new HashSet<>(errorIdentifiers);
        previouslyUnshown.removeAll(asCollection(shownErrorIndicators));

        if (previouslyUnshown.isEmpty())
        {
            // Already know about all of them:
            return;
        }
        else
        {
            // We only send those that are  previously unshown, now shown, but have been sent as created:
            HashSet<Integer> toSend = new HashSet<>(previouslyUnshown);
            toSend.retainAll(asCollection(createdErrors));
            if (!toSend.isEmpty())
            {
                // Creation has already been sent for the these events, so fine to follow it up with a shown event:
                DataCollectorImpl.showErrorIndicators(pkg, toSend);
            }
            // Otherwise, we haven't sent the creation yet, so do nothing (will get sent later)


            // Either way, flag it in the bitset as shown:
            for (Integer id : previouslyUnshown)
            {
                shownErrorIndicators.set(id);
            }
        }
    }

    /**
     * Mirrors a BitSet as a collection of integers (the indexes of the set bits).
     * Surprisingly, there's no method in BitSet itself to support this.
     *
     * Rather than make a copy of the collection, we create a fake collection
     * which is based on the original bitset.  Thus, if the original bitset changes,
     * the returned collection will also change.  Hence why it's named asCollection
     * rather than toCollection.
     */
    private static Collection<Integer> asCollection(BitSet bitSet)
    {
        return new AbstractCollection<Integer>()
        {
            @Override
            public Iterator<Integer> iterator()
            {
                return new Iterator<Integer>()
                {
                    // -1 when there's no more bits
                    int nextBit = bitSet.nextSetBit(0);

                    @Override
                    public boolean hasNext()
                    {
                        return nextBit != -1;
                    }

                    @Override
                    public Integer next()
                    {
                        int retBit = nextBit;
                        nextBit = bitSet.nextSetBit(nextBit + 1);
                        return retBit;
                    }
                };
            }

            @Override
            public boolean contains(Object o)
            {
                if (o instanceof Integer)
                    return bitSet.get((Integer)o);
                else
                    return false;
            }

            @Override
            public int size()
            {
                return bitSet.cardinality();
            }
        };
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

    /**
     * Record that code completion has been triggered.
     *
     * @param ct The class target in which code completion was triggered.
     * @param lineNumber The Java line number, or null if Stride is being used
     * @param columnNumber The Java column number, or null if Stride is being used
     * @param xpath The XPath to the Stride element, or null if Java is being used
     * @param subIndex The sub-index within the Stride element, or null if Java is being used
     * @param stem The initial String stem used to decide initially eligible items
     * @param codeCompletionId The ID of the code completion, unique to this session.  Used to match with later ending event.
     */
    public static void codeCompletionStarted(ClassTarget ct, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, int codeCompletionId)
    {
        if (dontSend()) return;
        DataCollectorImpl.codeCompletionStarted(ct.getPackage().getProject(), ct.getPackage(), lineNumber, columnNumber, xpath, subIndex, stem, codeCompletionId);
    }

    /**
     * Record that code completion has ended.
     *
     * @param ct The class target in which code completion was ended.
     * @param lineNumber The Java line number, or null if Stride is being used
     * @param columnNumber The Java column number, or null if Stride is being used
     * @param xpath The XPath to the Stride element, or null if Java is being used
     * @param subIndex The sub-index within the Stride element, or null if Java is being used
     * @param stem The current String stem at the point where the code completion was ended.
     * @param replacement The replacement which was chosen from the code completion list.
     * @param codeCompletionId The ID of the code completion, unique to this session.  Used to match with later ending event.
     */
    public static void codeCompletionEnded(ClassTarget ct, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, String replacement, int codeCompletionId)
    {
        if (dontSend()) return;
        DataCollectorImpl.codeCompletionEnded(ct.getPackage().getProject(), ct.getPackage(), lineNumber, columnNumber, xpath, subIndex, stem, replacement, codeCompletionId);
    }

    public static void unknownFrameCommandKey(ClassTarget ct, String enclosingFrameXpath, int cursorIndex, char key)
    {
        if (dontSend()) return;
        DataCollectorImpl.unknownFrameCommandKey(ct.getPackage().getProject(), ct.getPackage(), enclosingFrameXpath, cursorIndex, key);
    }

    /**
     * Records the Frame Catalogue's showing/hiding.
     *
     * @param project              The current project
     * @param pkg                  The current package. May be <code>null</code>.
     * @param enclosingFrameXpath  The path for the frame that include the focused cursor, if any. May be <code>null</code>.
     * @param cursorIndex          The focused cursor's index (if any) within the enclosing frame.
     * @param show                 true for showing and false for hiding
     * @param reason               The user interaction which triggered the change.
     */
    public static void showHideFrameCatalogue(Project project, Package pkg, String enclosingFrameXpath, int cursorIndex,
                                              boolean show, FrameCatalogue.ShowReason reason)
    {
        if (dontSend()) {
            return;
        }
        DataCollectorImpl.showHideFrameCatalogue(project, pkg, enclosingFrameXpath, cursorIndex, show, reason);
    }

    /**
     * Records a view mode change.
     *
     * @param pkg                  The current package. May be <code>null</code>.
     * @param sourceFile           The Stride file that its view mode has changed.
     * @param enclosingFrameXpath  The path for the frame that include the focused cursor, if any. May be <code>null</code>.
     * @param cursorIndex          The focused cursor's index (if any) within the enclosing frame.
     * @param oldView              The old view mode that been switch from.
     * @param newView              The new view mode that been switch to.
     * @param reason               The user interaction which triggered the change.
     */
    public static void viewModeChange(Package pkg, File sourceFile, String enclosingFrameXpath, int cursorIndex,
                                      Frame.View oldView, Frame.View newView, Frame.ViewChangeReason reason)
    {
        if (dontSend()) {
            return;
        }
        DataCollectorImpl.viewModeChange(pkg.getProject(), pkg, sourceFile, enclosingFrameXpath, cursorIndex, oldView, newView, reason);
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
