/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2014,2015,2016,2017,2019  Michael Kolling and John Rosenberg
 
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
import bluej.collect.CollectUtility.ProjectDetails;
import bluej.collect.DataCollector.NamedTyped;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.editor.stride.FrameCatalogue;
import bluej.extensions2.SourceType;
import bluej.extmgr.ExtensionWrapper;
import bluej.groupwork.Repository;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.ClassTarget.SourceFileInfo;
import bluej.stride.generic.Frame;
import bluej.utility.Utility;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.http.entity.mime.MultipartEntity;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * DataCollector for sending off data.
 * <p>
 * This is the actual implementation. Methods in this class are only called when data collection
 * is active.
 * <p>
 * The implementation here is separate from the interface to BlueJ, which is in the DataCollector
 * class, to avoid creating a run-time dependency on the commonds HTTP libraries when running without
 * data collection enabled (eg in Greenfoot).
 */
@OnThread(Tag.FXPlatform)
public class DataCollectorImpl
{
    /**
     * In BlueJ, the Project holds the list of inspectors, even though really
     * an inspector can be traced back to being per-package.  We hold this map
     * to record this association (which we know when an inspector is shown)
     * so that we can re-use it when the inspector is hidden. 
     */
    private static IdentityHashMap<Inspector, Package> inspectorPackages = new IdentityHashMap<Inspector, Package>();

    /**
     * Submits an event with no extra data.  A useful short-hand for calling submitEvent
     * with no content in the event.
     */
    private static void submitEventNoData(Project project, Package pkg, EventName eventName)
    {
        submitEvent(project, pkg, eventName, new PlainEvent(new MultipartEntity()));
    }
    
    /**
     * Submits an event and adds a source location.  This should be used if the file
     * is within the project (otherwise see submitEventWithClassLocation)
     * 
     * @param project
     * @param eventName
     * @param mpe You can pass null if you have no other data to give
     * @param sourceFile
     * @param lineNumber
     */
    private static void submitEventWithLocalLocation(Project project, Package pkg, EventName eventName, MultipartEntity mpe, File sourceFile, int lineNumber)
    {
        if (mpe == null)
        {
            mpe = new MultipartEntity();
        }
        
        mpe.addPart("event[source_file_name]", CollectUtility.toBodyLocal(new ProjectDetails(project), sourceFile));
        mpe.addPart("event[line_number]", CollectUtility.toBody(lineNumber));
        
        submitEvent(project, pkg, eventName, new PlainEvent(mpe));
    }


    /**
     * Submits an event and adds a source location.  This should be used if it is not
     * known whether the class is in the project, or whether it might be from a library
     * (e.g. java.io.*) or wherever.
     * 
     * @param project
     * @param eventName
     * @param mpe You can pass null if you have no other data to give
     * @param classSourceName
     * @param lineNumber
     */
    private static void submitDebuggerEventWithLocation(Project project, EventName eventName, MultipartEntity mpe, SourceLocation[] stack)
    {
        if (mpe == null)
        {
            mpe = new MultipartEntity();
        }
        
        DataCollectorImpl.addStackTrace(mpe, "event[stack]", stack);
        
        submitEvent(project, null, eventName, new PlainEvent(mpe));
    }

    /**
     * Submit an event.
     * 
     * @param project   the associated project (may be null)
     * @param pkg       the associated package (may be null)
     * @param eventName  the name of the event type
     * @param evt       the event to be submitted
     */
    private static synchronized void submitEvent(final Project project, final Package pkg, final EventName eventName, final Event evt)
    {
        final String projectName = project == null ? null : project.getProjectName();
        final String projectPathHash = project == null ? null : CollectUtility.md5Hash(project.getProjectDir().getAbsolutePath());
        final String packageName = pkg == null ? null : pkg.getQualifiedName();
        
        // We take a copy of these internal variables, so that we don't have a race hazard
        // if the variable changes between now and the event being sent:
        final String uuidCopy = DataCollector.getUserID();
        final String experimentCopy = DataCollector.getExperimentIdentifier();
        final String participantCopy = DataCollector.getParticipantIdentifier();
        
        /**
         * Wrap the Event we've been given to add the other normal expected fields:
         */
        DataSubmitter.submitEvent(new Event() {
            
            @Override public void success(Map<FileKey, List<String>> fileVersions)
            {
                evt.success(fileVersions);
            }
            
            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                MultipartEntity mpe = evt.makeData(sequenceNum, fileVersions);
                
                if (mpe == null)
                    return null;
                
                mpe.addPart("user[uuid]", CollectUtility.toBody(uuidCopy));
                mpe.addPart("session[id]", CollectUtility.toBody(DataCollector.getSessionUuid()));
                mpe.addPart("participant[experiment]", CollectUtility.toBody(experimentCopy));
                mpe.addPart("participant[participant]", CollectUtility.toBody(participantCopy));
                
                if (projectName != null)
                {
                    mpe.addPart("project[name]", CollectUtility.toBody(projectName));
                    mpe.addPart("project[path_hash]", CollectUtility.toBody(projectPathHash));
                    
                    if (packageName != null)
                    {
                        mpe.addPart("package[name]", CollectUtility.toBody(packageName));
                    }
                }
                
                mpe.addPart("event[source_time]", CollectUtility.toBody(DateFormat.getDateTimeInstance().format(new Date())));
                mpe.addPart("event[name]", CollectUtility.toBody(eventName.getName()));
                mpe.addPart("event[sequence_id]", CollectUtility.toBody(Integer.toString(sequenceNum)));
                
                return mpe;
            }
        });
    }

    public static void compiled(Project proj, Package pkg, CompileInputFile[] sources, List<DiagnosticWithShown> diagnostics, boolean success, CompileReason compileReason, int compileSequence)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[compile_success]", CollectUtility.toBody(success));
        mpe.addPart("event[compile_reason]", CollectUtility.toBody(compileReason.getServerString()));
        if (compileSequence != -1)
        {
            mpe.addPart("event[compile_sequence]", CollectUtility.toBody(compileSequence));
        }
        
        ProjectDetails projDetails = new ProjectDetails(proj);
        for (CompileInputFile src : sources)
        {
            mpe.addPart("event[compile_input][][source_file_name]", CollectUtility.toBody(CollectUtility.toPath(projDetails, src.getUserSourceFile())));
        }        
        
        for (DiagnosticWithShown dws : diagnostics)
        {
            final Diagnostic d = dws.getDiagnostic();

            mpe.addPart("event[compile_output][][is_error]", CollectUtility.toBody(d.getType() == Diagnostic.ERROR));
            mpe.addPart("event[compile_output][][message]", CollectUtility.toBody(d.getMessage()));
            mpe.addPart("event[compile_output][][session_sequence]", CollectUtility.toBody(d.getIdentifier()));
            mpe.addPart("event[compile_output][][origin]", CollectUtility.toBody(d.getOrigin()));
            if (d.getFileName() != null)
            {
                if (d.getStartLine() >= 1)
                    mpe.addPart("event[compile_output][][start_line]", CollectUtility.toBody(d.getStartLine()));
                if (d.getEndLine() >= 1)
                    mpe.addPart("event[compile_output][][end_line]", CollectUtility.toBody(d.getEndLine()));
                if (d.getStartColumn() >= 1)
                    mpe.addPart("event[compile_output][][start_column]", CollectUtility.toBody(d.getStartColumn()));
                if (d.getEndColumn() >= 1)
                    mpe.addPart("event[compile_output][][end_column]", CollectUtility.toBody(d.getEndColumn()));
                if (d.getXPath() != null)
                {
                    mpe.addPart("event[compile_output][][xpath]", CollectUtility.toBody(d.getXPath()));
                    if (d.getXmlStart() != -1)
                        mpe.addPart("event[compile_output][][xml_start]", CollectUtility.toBody(d.getXmlStart()));
                    if (d.getXmlEnd() != -1)
                        mpe.addPart("event[compile_output][][xml_end]", CollectUtility.toBody(d.getXmlEnd()));
                }
                // Must make file name relative for anonymisation:
                String relative = CollectUtility.toPath(projDetails, dws.getUserFileName());
                mpe.addPart("event[compile_output][][source_file_name]", CollectUtility.toBody(relative));
            }
        }
        submitEvent(proj, pkg, EventName.COMPILE, new PlainEvent(mpe));
    }
    
    public static void bluejOpened(String osVersion, String javaVersion, String bluejVersion, String interfaceLanguage, List<ExtensionWrapper> extensions)
    {
        if (Config.isGreenfoot() && !Boot.isTrialRecording()) return; //Don't even look for UUID
        DataSubmitter.initSequence();
        
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("installation[operating_system]", CollectUtility.toBody(osVersion));
        mpe.addPart("installation[java_version]", CollectUtility.toBody(javaVersion));
        mpe.addPart("installation[bluej_version]", CollectUtility.toBody(bluejVersion));
        mpe.addPart("installation[interface_language]", CollectUtility.toBody(interfaceLanguage));
        
        addExtensions(mpe, extensions);
        
        submitEvent(null, null, EventName.BLUEJ_START, new PlainEvent(mpe));
    }


    private static void addExtensions(MultipartEntity mpe,
            List<ExtensionWrapper> extensions)
    {
        for (ExtensionWrapper ext : extensions)
        {
            mpe.addPart("extensions[][name]", CollectUtility.toBody(ext.safeGetExtensionName()));
            mpe.addPart("extensions[][version]", CollectUtility.toBody(ext.safeGetExtensionVersion()));
        }
    }
    
    public static void projectOpened(Project proj, List<ExtensionWrapper> projectExtensions)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        addExtensions(mpe, projectExtensions);
        
        submitEventNoData(proj, null, EventName.PROJECT_OPENING);
    }
    
    public static void projectClosed(Project proj)
    {
        submitEventNoData(proj, null, EventName.PROJECT_CLOSING);
    }

    /**
     * Records the EventName.PACKAGE_OPENING event for the given package, including sending relevant source histories.
     */
    public static void packageOpened(Package pkg)
    {
        addCompleteFiles(pkg, EventName.PACKAGE_OPENING, pkg.getClassTargets(), null);
    }

    /**
     * Sends an event with a set of complete files.
     *
     * @param pkg The package that the set of files all belong to.
     * @param eventName The event name to send with the event
     * @param classTargets The class targets of the files to send.
     *                     If there are Stride files, both Stride and Java
     *                     will be sent.
     * @param extra The extra action to run on the event before sending,
     *              e.g. add an extra field to send.  May be null (meaning no
     *              extra action to run).
     */
    private static void addCompleteFiles(Package pkg, EventName eventName, List<ClassTarget> classTargets, Consumer<MultipartEntity> extra)
    {
        final MultipartEntity mpe = new MultipartEntity();

        if (extra != null)
        {
            extra.accept(mpe);
        }

        final ProjectDetails proj = new ProjectDetails(pkg.getProject());

        final Map<FileKey, List<String>> versions = new HashMap<>();

        for (ClassTarget ct : classTargets)
        {
            // It is important we add Stride file first, then Java, because Java will note it is generated from the Stride
            // file, so server needs to process the Stride file first:
            for (SourceFileInfo fileInfo : ct.getAllSourceFilesJavaLast())
            {
                String relative = CollectUtility.toPath(proj, fileInfo.file);
                mpe.addPart("project[source_files][][name]", CollectUtility.toBody(relative));
                switch (fileInfo.sourceType)
                {
                    case Java:
                        mpe.addPart("project[source_files][][source_type]", CollectUtility.toBody("java"));
                        break;
                    case Stride:
                        mpe.addPart("project[source_files][][source_type]", CollectUtility.toBody("stride"));
                        break;
                }
                String anonymisedContent = CollectUtility.readFileAndAnonymise(proj, fileInfo.file);

                String generatedFrom = null;

                // If this is the Java file and there was a Stride file, note the relation:
                if (fileInfo.sourceType == SourceType.Java && ct.getSourceType() == SourceType.Stride)
                {
                    generatedFrom = CollectUtility.toPath(proj, ct.getSourceFile());
                    // Java file won't have been saved yet, but that's ok, just treat it as
                    // empty but existing for now:
                    if (anonymisedContent == null)
                        anonymisedContent = "";
                }

                if (anonymisedContent != null)
                {
                    addSourceHistoryItem(mpe, relative, "complete", anonymisedContent, generatedFrom);
                    versions.put(new FileKey(proj, relative), Arrays.asList(Utility.splitLines(anonymisedContent)));
                }
            }
        }

        submitEvent(pkg.getProject(), pkg, eventName, new Event() {

            @Override
            public void success(Map<FileKey, List<String>> fileVersions)
            {
                fileVersions.putAll(versions);
            }

            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                return mpe;
            }
        });
    }

    /**
     * Adds a source history item to the MPE
     * @param mpe The MPE we're sending to the server
     * @param relativeName The name of the file (relative to project root)
     * @param anonymisedContent The anonymised content of the file or diff.  May null for some types (e.g. file deletion)
     * @param generatedFrom The Stride file this Java file was generated from.  May be null if this is a Stride file, or a Java file that has no Stride.
     */
    @OnThread(Tag.Any)
    private static void addSourceHistoryItem(MultipartEntity mpe, String relativeName, String type, String anonymisedContent, String generatedFrom)
    {
        mpe.addPart("source_histories[][source_history_type]", CollectUtility.toBody(type));
        mpe.addPart("source_histories[][name]", CollectUtility.toBody(relativeName));
        if (generatedFrom != null)
        {
            mpe.addPart("source_histories[][generated_from]", CollectUtility.toBody(generatedFrom));
        }
        if (anonymisedContent != null)
        {
            mpe.addPart("source_histories[][content]", CollectUtility.toBody(anonymisedContent));
        }
    }

    /**
     * Records the EventName.PACKAGE_CLOSING event for the given package, including sending relevant source histories.
     */
    public static void packageClosed(Package pkg)
    {
        addCompleteFiles(pkg, EventName.PACKAGE_CLOSING, pkg.getClassTargets(), mpe -> {
            mpe.addPart("event[has_hash]", CollectUtility.toBody(true));
        });
    }

    public static void bluejClosed()
    {
        submitEventNoData(null, null, EventName.BLUEJ_FINISH);
        
        // Give the queue 1 second to be flushed, so that the finish event gets time to be sent,
        // but otherwise, return anyway (can't wait forever):
        DataSubmitter.waitForQueueFlush(1000);
    }

    public static void restartVM(Project project)
    {
        submitEventNoData(project, null, EventName.RESETTING_VM);        
    }

    static class EditedFileInfo
    {
        // e.g. "diff" or "diff_generated"
        private final String editType;
        // Path to the relevant file
        private final File path;
        // The complete original (unanonymised) source
        private final String source;
        // Should we send the edit if it's only one line?
        private final boolean includeOneLineEdits;
        // The file which this one was generated from (or null if N/A)
        private final File generatedFrom;
        // The reason for the Stride edit being generated (null if unknown or N/A)
        private final StrideEditReason strideEditReason;
        // These get set after constructor:
        private FileKey fileKey;
        private List<String> anonSource;
        // Keep track of whether we actually sent the edit or not:
        public boolean dontSend = false;

        EditedFileInfo(String editType, File path, String source, boolean includeOneLineEdits, File generatedFrom, StrideEditReason strideEditReason)
        {
            this.editType = editType;
            this.path = path;
            this.source = source;
            this.includeOneLineEdits = includeOneLineEdits;
            this.generatedFrom = generatedFrom;
            this.strideEditReason = strideEditReason;
        }
    }


    static void edit(final Package pkg, List<EditedFileInfo> editedFiles)
    {
        final Project proj = pkg.getProject();
        final ProjectDetails projDetails = new ProjectDetails(proj);
        // Generate FileKeys and anonymous source for all the files:
        for (EditedFileInfo editedFile : editedFiles)
        {
            editedFile.fileKey = new FileKey(projDetails, CollectUtility.toPath(projDetails, editedFile.path));
            editedFile.anonSource = Arrays.asList(Utility.splitLines(CodeAnonymiser.anonymise(editedFile.source)));
        }
                
        submitEvent(proj, pkg, EventName.EDIT, new Event() {
            
            //Edit solely within one line
            private boolean isOneLineDiff(Patch patch)
            {
                if (patch.getDeltas().size() > 1)
                    return false;
                Delta theDelta = patch.getDeltas().get(0);
                return theDelta.getOriginal().size() == 1 && theDelta.getRevised().size() == 1;
            }
            
            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                MultipartEntity mpe = new MultipartEntity();
                for (EditedFileInfo editedFile : editedFiles)
                {

                    List<String> previousDoc = fileVersions.get(editedFile.fileKey);
                    if (previousDoc == null)
                        previousDoc = new ArrayList<String>(); // Diff against empty file


                    Patch patch = DiffUtils.diff(previousDoc, editedFile.anonSource);

                    if (patch.getDeltas().isEmpty() || (isOneLineDiff(patch) && !editedFile.includeOneLineEdits))
                    {
                        editedFile.dontSend = true;
                        continue;
                    }

                    String diff = makeDiff(patch);

                    addSourceHistoryItem(mpe, CollectUtility.toPath(projDetails, editedFile.path), editedFile.editType, diff, editedFile.generatedFrom == null ? null : CollectUtility.toPath(projDetails, editedFile.generatedFrom));

                    if (editedFile.strideEditReason != null && editedFile.strideEditReason.getText() != null)
                    {
                        mpe.addPart("source_histories[][reason]", CollectUtility.toBody(editedFile.strideEditReason.getText()));
                    }
                }
                // If no files to send, cancel sending the whole edit event:
                if (editedFiles.stream().allMatch(f -> f.dontSend))
                {
                    return null;
                }
                else
                {
                    return mpe;
                }
            }

            @Override
            public void success(Map<FileKey, List<String>> fileVersions)
            {
                for (EditedFileInfo editedFile : editedFiles)
                {
                    if (!editedFile.dontSend)
                    {
                        fileVersions.put(editedFile.fileKey, editedFile.anonSource);
                    }
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    @OnThread(Tag.Any)
    // protected for testing purposes
    protected static String makeDiff(Patch patch)
    {
        StringBuilder diff = new StringBuilder();
        // There is a DiffUtils.generateUnifiedDiff function, but don't use it
        // It's broken in 1.2.1 for patches that purely add lines
        
        for (Delta delta: patch.getDeltas()) {
            int srcLine, srcSize, destLine, destSize;
            srcSize = delta.getOriginal().size();
            destSize = delta.getRevised().size();
            // It seems that the line numbers given back when the patch is a pure-insert
            // (i.e. zero lines are modified in the original file) are off by one, so
            // correct for this:
            if (srcSize > 0)
            {
                srcLine = delta.getOriginal().getPosition() + 1;
                destLine = delta.getRevised().getPosition() + 1;
            }
            else
            {
                srcLine = delta.getOriginal().getPosition();
                destLine = delta.getRevised().getPosition();
            }
            diff.append("@@ -" + srcLine + "," + srcSize + " +" + destLine + "," + destSize + " @@\n");
            for (String l : (List<String>)delta.getOriginal().getLines())
            {
                diff.append("-" + l + "\n");
            }
            for (String l : (List<String>)delta.getRevised().getLines())
            {
                diff.append("+" + l + "\n");
            }
        }
        return diff.toString();
    }
    
    
    public static void debuggerTerminate(Project project)
    {
        submitEventNoData(project, null, EventName.DEBUGGER_TERMINATE);        
    }
    
    public static void debuggerChangeVisible(Project project, boolean newVis)
    {
        submitEventNoData(project, null, newVis ? EventName.DEBUGGER_OPEN : EventName.DEBUGGER_CLOSE);        
    }
    
    public static void debuggerBreakpointToggle(Package pkg, File sourceFile, int lineNumber, boolean newState)
    {
        submitEventWithLocalLocation(pkg.getProject(), pkg, newState ? EventName.DEBUGGER_BREAKPOINT_ADD : EventName.DEBUGGER_BREAKPOINT_REMOVE, null, sourceFile, lineNumber);
    }
    
    public static void debuggerContinue(Project project, String threadName)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", CollectUtility.toBody(threadName));
        submitEvent(project, null, EventName.DEBUGGER_CONTINUE, new PlainEvent(mpe));        
    }

    public static void debuggerHalt(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", CollectUtility.toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_HALT, mpe, stack);
    }
    
    public static void debuggerStepInto(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", CollectUtility.toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_STEP_INTO, mpe, stack);
    }
    
    public static void debuggerStepOver(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", CollectUtility.toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_STEP_OVER, mpe, stack);
    }
    
    public static void debuggerHitBreakpoint(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", CollectUtility.toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_HIT_BREAKPOINT, mpe, stack);
    }
    
    public static void codePadSuccess(Package pkg, String command, String output)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", CollectUtility.toBody("success"));
        mpe.addPart("event[codepad][command]", CollectUtility.toBody(command));
        mpe.addPart("event[codepad][result]", CollectUtility.toBody(output));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
    }
    
    public static void codePadError(Package pkg, String command, String error)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", CollectUtility.toBody("error"));
        mpe.addPart("event[codepad][command]", CollectUtility.toBody(command));
        mpe.addPart("event[codepad][error]", CollectUtility.toBody(error));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
    }
    
    public static void codePadException(Package pkg, String command, String exception)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", CollectUtility.toBody("exception"));
        mpe.addPart("event[codepad][command]", CollectUtility.toBody(command));
        mpe.addPart("event[codepad][exception]", CollectUtility.toBody(exception));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
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
    public static void renamedClass(Package pkg, final File oldFrameSourceFile, final File newFrameSourceFile,
                                    final File oldJavaSourceFile, final File newJavaSourceFile)
    {
        final String eventType = "rename";
        final ProjectDetails projDetails = new ProjectDetails(pkg.getProject());

        final boolean isFrameFile = newFrameSourceFile != null;
        final String oldFrameFilePath = isFrameFile ? CollectUtility.toPath(projDetails, oldFrameSourceFile) : null;
        final String newFrameFilePath = isFrameFile ? CollectUtility.toPath(projDetails, newFrameSourceFile) : null;

        final String oldJavaFilePath = CollectUtility.toPath(projDetails, oldJavaSourceFile);
        final String newJavaFilePath = CollectUtility.toPath(projDetails, newJavaSourceFile);

        MultipartEntity mpe = new MultipartEntity();
        if (isFrameFile) {
            addSourceHistoryItem(mpe, newFrameFilePath, eventType, oldFrameFilePath, null);
        }
        addSourceHistoryItem(mpe, newJavaFilePath, eventType, oldJavaFilePath, newFrameFilePath);

        submitEvent(pkg.getProject(), pkg, EventName.RENAME, new PlainEvent(mpe) {

            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                // We need to change the fileVersions hash to move the content across
                // from the old file to the new file:
                if (isFrameFile) {
                    FileKey oldFrameKey = new FileKey(projDetails, oldFrameFilePath);
                    FileKey newFrameKey = new FileKey(projDetails, newFrameFilePath);
                    fileVersions.put(newFrameKey, fileVersions.get(oldFrameKey));
                    fileVersions.remove(oldFrameKey);
                }

                FileKey oldJavaKey  = new FileKey(projDetails, oldJavaFilePath);
                FileKey newJavaKey  = new FileKey(projDetails, newJavaFilePath);
                fileVersions.put(newJavaKey, fileVersions.get(oldJavaKey));
                fileVersions.remove(oldJavaKey);

                return super.makeData(sequenceNum, fileVersions);
            }
            
        });
    }

    /**
     * Records removing class files (Java, or Stride and its generated Java).
     *
     * @param pkg              The package in which the files live.
     * @param frameSourceFile  The Stride source file, or <code>null</code> in case the source type is Java.
     * @param javaSourceFile   The Java source file.
     */
    public static void removeClass(Package pkg, File frameSourceFile, File javaSourceFile)
    {
        final String eventType = "file_delete";
        final ProjectDetails projDetails = new ProjectDetails(pkg.getProject());

        final boolean isStrideFile = frameSourceFile != null;
        final String strideFilePath = isStrideFile ? CollectUtility.toPath(projDetails, frameSourceFile) : null;
        final String javaFilePath = CollectUtility.toPath(projDetails, javaSourceFile);

        MultipartEntity mpe = new MultipartEntity();
        if (isStrideFile) {
            addSourceHistoryItem(mpe, strideFilePath, eventType, null, null);
        }
        addSourceHistoryItem(mpe, javaFilePath, eventType, null, strideFilePath);

        submitEvent(pkg.getProject(), pkg, EventName.DELETE, new PlainEvent(mpe) {

            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                // We should remove the old source from the fileVersions hash:
                if (isStrideFile) {
                    fileVersions.remove(new FileKey(projDetails, strideFilePath));
                }
                fileVersions.remove(new FileKey(projDetails, javaFilePath));
                return super.makeData(sequenceNum, fileVersions);
            }

        });
    }

    /**
     * Send a conversion event (Stride to Java, or Java to Stride) to the server.
     * @param pkg The package that the files live in
     * @param javaSourceFile The Java file involved in the conversion (may be source or destination depending on conversino direction)
     * @param strideSourceFile The Stride file involved in the conversion (ditto: may be source or destination)
     * @param strideToJava If true, conversion is Stride->Java.  If false, conversion is Java->Stride.
     */
    static void conversion(Package pkg, File javaSourceFile, File strideSourceFile, boolean strideToJava)
    {
        final ProjectDetails projDetails = new ProjectDetails(pkg.getProject());
        MultipartEntity mpe = new MultipartEntity();
        // The Java file will always be a diff against previous content, because no matter which direction
        // the conversion is in, the Java file will exist before and after.
        // The Stride file will either be deleted (Stride->Java), or added and thus complete (Java->Stride).

        // First deal with Stride file:
        String anonStride;
        if (strideToJava)
        {
            addSourceHistoryItem(mpe, CollectUtility.toPath(projDetails, strideSourceFile), "file_delete", null, null);
            anonStride = null;
        }
        else
        {
            anonStride = CollectUtility.readFileAndAnonymise(projDetails, strideSourceFile);
            addSourceHistoryItem(mpe, CollectUtility.toPath(projDetails, strideSourceFile),  "java_to_stride",
                    anonStride, null);
            mpe.addPart("source_histories[][converted_from]", CollectUtility.toBodyLocal(projDetails, javaSourceFile));
        }


        // Then deal with the Java file:
        mpe.addPart("source_histories[][source_history_type]", CollectUtility.toBody(strideToJava ? "stride_to_java" : "diff_generated"));
        mpe.addPart("source_histories[][name]", CollectUtility.toBodyLocal(projDetails, javaSourceFile));
        final List<String> anonJava = Arrays.asList(Utility.splitLines(CollectUtility.readFileAndAnonymise(projDetails, javaSourceFile)));
        // We do not put the content in yet, that's done below because it's a diff...

        if (strideToJava)
        {
            mpe.addPart("source_histories[][converted_from]", CollectUtility.toBodyLocal(projDetails, strideSourceFile));
        }
        else
        {
            // We converted Java to Stride, so now the Java file is generated from the Stride:
            mpe.addPart("source_histories[][generated_from]", CollectUtility.toBodyLocal(projDetails, strideSourceFile));
        }

        final FileKey strideFileKey = new FileKey(projDetails, CollectUtility.toPath(projDetails, strideSourceFile));
        final FileKey javaFileKey = new FileKey(projDetails, CollectUtility.toPath(projDetails, javaSourceFile));

        submitEvent(pkg.getProject(), pkg, strideToJava ? EventName.CONVERT_STRIDE_TO_JAVA : EventName.CONVERT_JAVA_TO_STRIDE, new PlainEvent(mpe) {

            @Override
            @OnThread(Tag.Worker)
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                List<String> previousDoc = fileVersions.get(javaFileKey);
                if (previousDoc == null)
                    previousDoc = new ArrayList<String>(); // Diff against empty file

                MultipartEntity mpe = new MultipartEntity();

                Patch patch = DiffUtils.diff(previousDoc, anonJava);
                String diff = makeDiff(patch);
                mpe.addPart("source_histories[][content]", CollectUtility.toBody(diff));

                // We need to change the fileVersions hash to remove/add Stride and alter Java:
                fileVersions.put(javaFileKey, anonJava);
                if (strideToJava)
                {
                    fileVersions.remove(strideFileKey);
                }
                else
                {
                    fileVersions.put(strideFileKey, Arrays.asList(Utility.splitLines(anonStride)));
                }

                return super.makeData(sequenceNum, fileVersions);
            }
        });
    }

    /**
     * Records the EventName.ADD event, indicating that the given class was added.
     * @param pkg The package containing the class.
     * @param ct The class involved.  Relevant source history (i.e. complete file) will be
     *           sent, either just .java (for Java classes) or .stride and .java (for Stride classes)
     */
    public static void addClass(Package pkg, ClassTarget ct)
    {
        addCompleteFiles(pkg, EventName.ADD, Collections.singletonList(ct), null);
    }

    public static void openClass(Package pkg, File sourceFile)
    {
        classEvent(pkg, sourceFile, EventName.FILE_OPEN);
    }

    public static void closeClass(Package pkg, File sourceFile)
    {
        classEvent(pkg, sourceFile, EventName.FILE_CLOSE);
    }

    public static void selectClass(Package pkg, File sourceFile)
    {
        classEvent(pkg, sourceFile, EventName.FILE_SELECT);
    }

    private static void classEvent(Package pkg, File sourceFile, EventName eventName)
    {
        final MultipartEntity mpe = new MultipartEntity();
        final ProjectDetails projDetails = new ProjectDetails(pkg.getProject());
        mpe.addPart("event[source_file_name]", CollectUtility.toBodyLocal(projDetails, sourceFile));
        submitEvent(pkg.getProject(), pkg, eventName, new PlainEvent(mpe));
    }

    public static void teamShareProject(Project project, Repository repo)
    {
        submitEvent(project, null, EventName.VCS_SHARE, new PlainEvent(DataCollectorImpl.getRepoMPE(repo)));    
    }

    /**
     * Records a VCS push event
     * @param project The project which is in VCS
     * @param repo The repository object for VCS
     * @param pushedFiles The files involved in the push
     */
    public static void teamPushProject(Project project, Repository repo, Collection<File> pushedFiles)
    {
        final ProjectDetails projDetails = new ProjectDetails(project);
        MultipartEntity mpe = DataCollectorImpl.getRepoMPE(repo);
        for (File f : pushedFiles)
        {
            mpe.addPart("vcs_files[][file]", CollectUtility.toBodyLocal(projDetails, f));
        }
        submitEvent(project, null, EventName.VCS_PUSH, new PlainEvent(mpe));
    }
    
    public static void teamCommitProject(Project project, Repository repo, Collection<File> committedFiles)
    {
        final ProjectDetails projDetails = new ProjectDetails(project);
        MultipartEntity mpe = DataCollectorImpl.getRepoMPE(repo);
        for (File f : committedFiles)
        {
            mpe.addPart("vcs_files[][file]", CollectUtility.toBodyLocal(projDetails, f));
        }
        submitEvent(project, null, EventName.VCS_COMMIT, new PlainEvent(mpe));
    }
    
    public static void teamUpdateProject(Project project, Repository repo, Collection<File> updatedFiles)
    {
        final ProjectDetails projDetails = new ProjectDetails(project);
        MultipartEntity mpe = DataCollectorImpl.getRepoMPE(repo);
        for (File f : updatedFiles)
        {
            mpe.addPart("vcs_files[][file]", CollectUtility.toBodyLocal(projDetails, f));
        }
        submitEvent(project, null, EventName.VCS_UPDATE, new PlainEvent(mpe));
    }
    
    public static void teamStatusProject(Project project, Repository repo, Map<File, String> status)
    {
        final ProjectDetails projDetails = new ProjectDetails(project);
        MultipartEntity mpe = DataCollectorImpl.getRepoMPE(repo);
        for (Map.Entry<File, String> s : status.entrySet())
        {
            mpe.addPart("vcs_files[][file]", CollectUtility.toBodyLocal(projDetails, s.getKey()));
            mpe.addPart("vcs_files[][status]", CollectUtility.toBody(s.getValue()));
        }
        submitEvent(project, null, EventName.VCS_STATUS, new PlainEvent(mpe));
    }
    
    public static void teamHistoryProject(Project project, Repository repo)
    {
        submitEvent(project, null, EventName.VCS_HISTORY, new PlainEvent(DataCollectorImpl.getRepoMPE(repo)));    
    }


    public static void showHideTerminal(Project project, boolean show)
    {
        submitEventNoData(project, null, show ? EventName.TERMINAL_OPEN : EventName.TERMINAL_CLOSE);      
    }

    
    public static void invokeCompileError(Package pkg, String code, String compilationError)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();

            mpe.addPart("event[invoke][code]", CollectUtility.toBody(code));
            mpe.addPart("event[invoke][result]", CollectUtility.toBody("compile_error"));
            mpe.addPart("event[invoke][compile_error]", CollectUtility.toBody(compilationError));
            submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));
        }
    }
    
    public static void invokeMethodSuccess(Package pkg, String code, String objName, String typeName, int testIdentifier, int invocationIdentifier)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();

            mpe.addPart("event[invoke][code]", CollectUtility.toBody(code));
            mpe.addPart("event[invoke][type_name]", CollectUtility.toBody(typeName));
            mpe.addPart("event[invoke][result]", CollectUtility.toBody("success"));
            mpe.addPart("event[invoke][test_identifier]", CollectUtility.toBody(testIdentifier));
            mpe.addPart("event[invoke][invoke_identifier]", CollectUtility.toBody(invocationIdentifier));
            if (objName != null)
            {
                mpe.addPart("event[invoke][bench_object][class_name]", CollectUtility.toBody(typeName));
                mpe.addPart("event[invoke][bench_object][name]", CollectUtility.toBody(objName));
            }
            submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));
        }
    }
    
    public static void invokeMethodException(Package pkg, String code, ExceptionDescription ed)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();

            mpe.addPart("event[invoke][code]", CollectUtility.toBody(code));
            mpe.addPart("event[invoke][result]", CollectUtility.toBody("exception"));
            mpe.addPart("event[invoke][exception_class]", CollectUtility.toBody(ed.getClassName()));
            mpe.addPart("event[invoke][exception_message]", CollectUtility.toBody(ed.getText()));
            DataCollectorImpl.addStackTrace(mpe, "event[invoke][exception_stack]", ed.getStack().toArray(new SourceLocation[0]));
            submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));
        }
    }
    
    public static void invokeMethodTerminated(Package pkg, String code)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();
            mpe.addPart("event[invoke][code]", CollectUtility.toBody(code));
            mpe.addPart("event[invoke][result]", CollectUtility.toBody("terminated"));
            submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));
        }
    }
    
    public static void removeObject(Package pkg, String name)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();
            mpe.addPart("event[object_name]", CollectUtility.toBody(name));
            submitEvent(pkg.getProject(), pkg, EventName.REMOVE_OBJECT, new PlainEvent(mpe));
        }
    }

    /**
     * A class inspector was shown.
     * 
     * @param proj  The project associated with the action
     * @param pkg   The package associated with the action; may be null
     * @param inspector  The inspector shown
     * @param className  The name of the class associated with the inspector
     */
    public static void inspectorClassShow(Project proj, Package pkg, Inspector inspector, String className)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();
            mpe.addPart("event[inspect][unique]", CollectUtility.toBody(inspector.getUniqueId()));
            mpe.addPart("event[inspect][static_class]", CollectUtility.toBody(className));
            inspectorPackages.put(inspector, pkg);
            submitEvent(proj, pkg, EventName.INSPECTOR_SHOW, new PlainEvent(mpe));
        }
    }
    
    public static void inspectorObjectShow(Package pkg, Inspector inspector, String benchName, String className, String displayName)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();
            mpe.addPart("event[inspect][unique]", CollectUtility.toBody(inspector.getUniqueId()));
            mpe.addPart("event[inspect][display_name]", CollectUtility.toBody(displayName));
            mpe.addPart("event[inspect][class_name]", CollectUtility.toBody(className));
            if (benchName != null)
            {
                mpe.addPart("event[inspect][bench_object_name]", CollectUtility.toBody(benchName));
            }
            inspectorPackages.put(inspector, pkg);
            submitEvent(pkg.getProject(), pkg, EventName.INSPECTOR_SHOW, new PlainEvent(mpe));
        }
    }
    
    public static void inspectorHide(Project project, Inspector inspector)
    {
        if (! false) {
            MultipartEntity mpe = new MultipartEntity();
            mpe.addPart("event[inspect][unique]", CollectUtility.toBody(inspector.getUniqueId()));
            if (inspector instanceof ClassInspector || inspector instanceof ObjectInspector)
            {
                submitEvent(project, inspectorPackages.get(inspector), EventName.INSPECTOR_HIDE, new PlainEvent(mpe));
            }
        }
    }


    public static void benchGet(Package pkg, String benchName, String typeName, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[bench_object][class_name]", CollectUtility.toBody(typeName));
        mpe.addPart("event[bench_object][name]", CollectUtility.toBody(benchName));
        mpe.addPart("event[test_identifier]", CollectUtility.toBody(testIdentifier));
        submitEvent(pkg.getProject(), pkg, EventName.BENCH_GET, new PlainEvent(mpe));
        
    }

    public static void startTestMethod(Package pkg, int testIdentifier, File sourceFile, String testName)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", CollectUtility.toBody(testIdentifier));
        mpe.addPart("event[test][source_file]", CollectUtility.toBodyLocal(new ProjectDetails(pkg.getProject()), sourceFile));
        mpe.addPart("event[test][method_name]", CollectUtility.toBody(testName));
        
        submitEvent(pkg.getProject(), pkg, EventName.START_TEST, new PlainEvent(mpe));
        
    }

    public static void cancelTestMethod(Package pkg, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", CollectUtility.toBody(testIdentifier));
        
        submitEvent(pkg.getProject(), pkg, EventName.CANCEL_TEST, new PlainEvent(mpe));
    }

    public static void endTestMethod(Package pkg, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", CollectUtility.toBody(testIdentifier));
        
        submitEvent(pkg.getProject(), pkg, EventName.END_TEST, new PlainEvent(mpe));
        
    }

    public static void assertTestMethod(Package pkg, int testIdentifier, int invocationIdentifier, 
            String assertion, String param1, String param2)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[assert][test_identifier]", CollectUtility.toBody(testIdentifier));
        mpe.addPart("event[assert][invoke_identifier]", CollectUtility.toBody(invocationIdentifier));
        mpe.addPart("event[assert][assertion]", CollectUtility.toBody(assertion));
        mpe.addPart("event[assert][param1]", CollectUtility.toBody(param1));
        mpe.addPart("event[assert][param2]", CollectUtility.toBody(param2));
        
        submitEvent(pkg.getProject(), pkg, EventName.ASSERTION, new PlainEvent(mpe));
        
    }

    public static void objectBenchToFixture(Package pkg, File sourceFile, List<String> benchNames)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[source_file_name]", CollectUtility.toBodyLocal(new ProjectDetails(pkg.getProject()), sourceFile));
        for (String name : benchNames)
        {
            mpe.addPart("event[bench_objects][][name]", CollectUtility.toBody(name));
        }        
        
        submitEvent(pkg.getProject(), pkg, EventName.BENCH_TO_FIXTURE, new PlainEvent(mpe));
        
    }
        
    public static void fixtureToObjectBench(Package pkg, File sourceFile, List<NamedTyped> objects)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[source_file_name]", CollectUtility.toBodyLocal(new ProjectDetails(pkg.getProject()), sourceFile));
        for (NamedTyped obj : objects)
        {
            mpe.addPart("event[bench_objects][][name]", CollectUtility.toBody(obj.getName()));
            mpe.addPart("event[bench_objects][][class_name]", CollectUtility.toBody(obj.getType()));
        }        
        
        submitEvent(pkg.getProject(), pkg, EventName.FIXTURE_TO_BENCH, new PlainEvent(mpe));
    }

    public static void testResult(Package pkg, DebuggerTestResult lastResult)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[class_name]", CollectUtility.toBody(lastResult.getQualifiedClassName()));
        mpe.addPart("event[method_name]", CollectUtility.toBody(lastResult.getMethodName()));
        mpe.addPart("event[run_time]", CollectUtility.toBody(lastResult.getRunTimeMs()));
        String status = "unknown";
        if (lastResult.isSuccess())
            status = "success";
        else if (lastResult.isFailure())
            status = "failure";
        else if (lastResult.isError())
            status = "error";
        mpe.addPart("event[result]", CollectUtility.toBody(status));
        
        if (!lastResult.isSuccess())
        {
            mpe.addPart("event[exception_message]", CollectUtility.toBody(lastResult.getExceptionMessage()));
            mpe.addPart("event[exception_trace]", CollectUtility.toBody(lastResult.getTrace()));
        }
        
        submitEvent(pkg.getProject(), pkg, EventName.RUN_TEST, new PlainEvent(mpe));
    }

    /**
     * Adds the given stack trace to the MPE, using the given list name.
     */
    private static void addStackTrace(MultipartEntity mpe, String listName, SourceLocation[] stack)
    {
        for (int i = 0; i < stack.length; i++)
        {
            mpe.addPart(listName + "[][entry]", CollectUtility.toBody(i));
            mpe.addPart(listName + "[][class_name]", CollectUtility.toBody(stack[i].getClassName()));
            mpe.addPart(listName + "[][class_source_name]", CollectUtility.toBody(stack[i].getFileName()));
            mpe.addPart(listName + "[][line_number]", CollectUtility.toBody(stack[i].getLineNumber()));
        }
    }

    /**
     * Turns the Repository into an MPE with appropriate information
     */
    private static MultipartEntity getRepoMPE(Repository repo)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[vcs][vcs_type]", CollectUtility.toBody(repo.getVCSType()));
        mpe.addPart("event[vcs][protocol]", CollectUtility.toBody(repo.getVCSProtocol()));
        return mpe;
    }

    public static void showErrorIndicators(Package pkg, Collection<Integer> errorIdentifiers)
    {
        MultipartEntity mpe = new MultipartEntity();
        // Sanity check -- don't send event if there's no sequences to send:
        if (errorIdentifiers.isEmpty())
        {
            return;
        }

        for (Integer errorIdentifier : errorIdentifiers)
        {
            mpe.addPart("event[error_sequences][]", CollectUtility.toBody(errorIdentifier));
        }
        submitEvent(pkg.getProject(), pkg, EventName.SHOWN_ERROR_INDICATOR, new PlainEvent(mpe));
    }

    public static void showErrorMessage(Package pkg, int errorIdentifier, List<String> quickFixes)
    {
        MultipartEntity mpe = new MultipartEntity();

        mpe.addPart("event[error_sequence]", CollectUtility.toBody(errorIdentifier));

        if (quickFixes != null && !quickFixes.isEmpty())
        {
            quickFixes.forEach(fix ->
                mpe.addPart("event[quick_fixes][][text]", CollectUtility.toBody(fix))
            );
        }

        submitEvent(pkg.getProject(), pkg, EventName.SHOWN_ERROR_MESSAGE, new PlainEvent(mpe));
    }

    public static void fixExecuted(Package pkg, int errorIdentifier, int fixIndex)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[error_sequence]", CollectUtility.toBody(errorIdentifier));
        mpe.addPart("event[fix_order]", CollectUtility.toBody(fixIndex));
        submitEvent(pkg.getProject(), pkg, EventName.FIX_EXECUTED, new PlainEvent(mpe));
    }

    public static void greenfootEvent(Project project, Package pkg, GreenfootInterfaceEvent greenfootEvent)
    {
        MultipartEntity mpe = new MultipartEntity();
        EventName event = null;
        switch (greenfootEvent)
        {
            case WINDOW_ACTIVATED:
                event = EventName.GREENFOOT_WINDOW_ACTIVATED;
                break;
            case WORLD_RESET:
                event = EventName.GREENFOOT_WORLD_RESET;
                break;
            case WORLD_ACT:
                event = EventName.GREENFOOT_WORLD_ACT;
                break;
            case WORLD_RUN:
                event = EventName.GREENFOOT_WORLD_RUN;
                break;
            case WORLD_PAUSE:
                event = EventName.GREENFOOT_WORLD_PAUSE;
                break;
        }
        if (event != null)
            submitEvent(project, pkg, event, new PlainEvent(mpe));
    }

    public static void codeCompletionStarted(Project project, Package pkg, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, int codeCompletionId)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[code_completion][trigger]", CollectUtility.toBody("start"));
        mpe.addPart("event[code_completion][completion_sequence]", CollectUtility.toBody(codeCompletionId));
        addCodeCompletionLocation(mpe, lineNumber, columnNumber, xpath, subIndex);
        if (stem != null)
            mpe.addPart("event[code_completion][stem]", CollectUtility.toBody(stem));
        submitEvent(project, pkg, EventName.CODE_COMPLETION_STARTED, new PlainEvent(mpe));
    }

    public static void codeCompletionEnded(Project project, Package pkg, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, String replacement, int codeCompletionId)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[code_completion][trigger]", CollectUtility.toBody("selected"));
        mpe.addPart("event[code_completion][completion_sequence]", CollectUtility.toBody(codeCompletionId));
        addCodeCompletionLocation(mpe, lineNumber, columnNumber, xpath, subIndex);
        if (stem != null)
            mpe.addPart("event[code_completion][stem]", CollectUtility.toBody(stem));
        if (replacement != null)
            mpe.addPart("event[code_completion][replacement]", CollectUtility.toBody(replacement));
        submitEvent(project, pkg, EventName.CODE_COMPLETION_ENDED, new PlainEvent(mpe));
    }

    private static void addCodeCompletionLocation(MultipartEntity mpe, Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex)
    {
        if (lineNumber != null)
            mpe.addPart("event[code_completion][line_number]", CollectUtility.toBody(lineNumber));
        if (columnNumber != null)
            mpe.addPart("event[code_completion][column_number]", CollectUtility.toBody(columnNumber));
        if (xpath != null)
            mpe.addPart("event[code_completion][xpath]", CollectUtility.toBody(xpath));
        if (subIndex != null)
            mpe.addPart("event[code_completion][xml_index]", CollectUtility.toBody(subIndex));
    }

    public static void unknownFrameCommandKey(Project project, Package pkg, String enclosingFrameXpath, int cursorIndex, char key)
    {
        MultipartEntity mpe = new MultipartEntity();
        if (enclosingFrameXpath != null)
        {
            mpe.addPart("event[unknown_frame_command][enclosing_xpath]", CollectUtility.toBody(enclosingFrameXpath));
            mpe.addPart("event[unknown_frame_command][enclosing_index]", CollectUtility.toBody(cursorIndex));
        }
        mpe.addPart("event[unknown_frame_command][command]", CollectUtility.toBody(Character.toString(key)));
        submitEvent(project, pkg, EventName.UNKNOWN_FRAME_COMMAND, new PlainEvent(mpe));
    }

    /**
     * Records the Frame Catalogue's showing/hiding.
     *
     * @param project              the current project
     * @param pkg                  the current package
     * @param enclosingFrameXpath  the path for the frame that include the focused cursor, if any.
     * @param cursorIndex          the focused cursor's index (if any) within the enclosing frame.
     * @param show                 true for showing and false for hiding
     * @param reason               the user interaction which triggered the change.
     */
    public static void showHideFrameCatalogue(Project project, Package pkg, String enclosingFrameXpath,
                                              int cursorIndex, boolean show, FrameCatalogue.ShowReason reason)
    {
        MultipartEntity mpe = new MultipartEntity();
        if (enclosingFrameXpath != null)
        {
            mpe.addPart("event[frame_catalogue_showing][enclosing_xpath]", CollectUtility.toBody(enclosingFrameXpath));
            mpe.addPart("event[frame_catalogue_showing][enclosing_index]", CollectUtility.toBody(cursorIndex));
        }
        mpe.addPart("event[frame_catalogue_showing][show]", CollectUtility.toBody(show));
        mpe.addPart("event[frame_catalogue_showing][reason]", CollectUtility.toBody(reason.getText()));
        submitEvent(project, pkg, EventName.FRAME_CATALOGUE_SHOWING, new PlainEvent(mpe));
    }

    /**
     * Records a view mode change.
     *
     * @param project              The current project
     * @param pkg                  The current package. May be <code>null</code>.
     * @param sourceFile           The Stride file that its view mode has changed.
     * @param enclosingFrameXpath  The path for the frame that include the focused cursor, if any. May be <code>null</code>.
     * @param cursorIndex          The focused cursor's index (if any) within the enclosing frame.
     * @param oldView              The old view mode that been switch from.
     * @param newView              The new view mode that been switch to.
     * @param reason               The user interaction which triggered the change.
     */
    public static void viewModeChange(Project project, Package pkg, File sourceFile, String enclosingFrameXpath,
                                      int cursorIndex, Frame.View oldView, Frame.View newView, Frame.ViewChangeReason reason)
    {
        MultipartEntity mpe = new MultipartEntity();
        final ProjectDetails projDetails = new ProjectDetails(pkg.getProject());
        mpe.addPart("event[view_mode_change][source_file_name]", CollectUtility.toBodyLocal(projDetails, sourceFile));
        if (enclosingFrameXpath != null) {
            mpe.addPart("event[view_mode_change][enclosing_xpath]", CollectUtility.toBody(enclosingFrameXpath));
            mpe.addPart("event[view_mode_change][enclosing_index]", CollectUtility.toBody(cursorIndex));
        }
        mpe.addPart("event[view_mode_change][old_view]", CollectUtility.toBody(oldView.getText()));
        mpe.addPart("event[view_mode_change][new_view]", CollectUtility.toBody(newView.getText()));
        mpe.addPart("event[view_mode_change][reason]", CollectUtility.toBody(reason.getText()));
        submitEvent(project, pkg, EventName.VIEW_MODE_CHANGE, new PlainEvent(mpe));
    }
}
