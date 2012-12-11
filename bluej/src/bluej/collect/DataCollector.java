/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import bluej.Config;
import bluej.compiler.Diagnostic;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.extmgr.ExtensionWrapper;
import bluej.groupwork.Repository;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.Utility;

/**
 * DataCollector for sending off data.
 * 
 * You can call these methods under any setting. It is this class's responsibility to check:
 *  - That the user has actually opted in (TODO)
 *  - That we're not running Greenfoot
 *  - That we don't keep attempting to send when there's no net connection (TODO)
 */
public class DataCollector
{
    private static final String PROPERTY_UUID = "blackbox.uuid";
    
    private static final Charset utf8 = Charset.forName("UTF-8");

    //private List<BPackage> openPkgs = new ArrayList<BPackage>();
    
    /** Whether we've seen the first error in the current compilation yet */
    //private boolean seenFirstError;
    
    private static String uuid;
    private static String sessionUuid;
    private static int sequenceNum;
    
    /**
     * In BlueJ, the Project holds the list of inspectors, even though really
     * an inspector can be traced back to being per-package.  We hold this map
     * to record this association (which we know when an inspector is shown)
     * so that we can re-use it when the inspector is hidden. 
     */
    private static IdentityHashMap<Inspector, Package> inspectorPackages = new IdentityHashMap<Inspector, Package>();
    
    /** map from package directory to information on the sources contained within */
    //private Map<File,Set<SourceInfo>> srcInfoMap = new HashMap<File,Set<SourceInfo>>();

       
    
    private static String readFileAndAnonymise(Project proj, File f)
    {
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream inputStream = new FileInputStream(f);
            InputStreamReader reader = new InputStreamReader(inputStream, proj.getProjectCharset());
            char[] buf = new char[4096];
            
            int read = reader.read(buf);
            while (read != -1)
            {
                sb.append(buf, 0, read);
                read = reader.read(buf);
            }
            
            reader.close();
            inputStream.close();
            return CodeAnonymiser.anonymise(sb.toString());
        }
        catch (IOException ioe) {return null;}
    }

    private static String toPath(Project proj, File f)
    {
        return proj.getProjectDir().toURI().relativize(f.toURI()).getPath();
    }
    
    private static boolean dontSend()
    {
        return Config.isGreenfoot() || "optout".equals(uuid);
    }
    
    public static String getUserID()
    {
        return Config.getPropString(PROPERTY_UUID, null);
    }
    
    private static void initSessionId()
    {
        sessionUuid = UUID.randomUUID().toString();
    }

    private static void initUUidSequence()
    {
        DataSubmitter.initSequence();
        uuid = Config.getPropString(PROPERTY_UUID, null);
        if (uuid == null)
        {
            //TODO display opt-in dialog
            
            uuid = UUID.randomUUID().toString();
            Config.putPropString(PROPERTY_UUID, uuid);
            
        }
        /*
        else {
            try {
                int numFiles = Integer.parseInt(bluej.getExtensionPropertyString(PROPERTY_NUMFILES, "0"));
                // Read known file change times
                for (int i = 0; i < numFiles; i++) {
                    String fname = bluej.getExtensionPropertyString(PROPERTY_FILENAME + i, null);
                    if (fname == null) break;
                    String lastModString = bluej.getExtensionPropertyString(PROPERTY_LASTMOD + i, "0");
                    long lastMod = Long.parseLong(lastModString);
                    File file = new File(fname);
                    File parentFile = file.getParentFile();

                    Set<SourceInfo> infoSet = srcInfoMap.get(parentFile);
                    if (infoSet == null) {
                        infoSet = new HashSet<SourceInfo>();
                        srcInfoMap.put(parentFile, infoSet);
                    }

                    infoSet.add(new SourceInfo(file.getName(), lastMod));
                }
            }
            catch (NumberFormatException nfe) {
                // Shouldn't happen; just ignore it.
            }
        }
        */
    }
    
    /**
     * Save information which should be persistent across sessions.
     */
    private void saveInfo()
    {
        /*
        bluej.setExtensionPropertyString(PROPERTY_UUID, uuid);
        Set<Map.Entry<File,Set<SourceInfo>>> entrySet = srcInfoMap.entrySet();
        int fileCount = 0;
        for (Map.Entry<File,Set<SourceInfo>> entry : entrySet) {
            File baseDir = entry.getKey();
            Set<SourceInfo> infoSet = entry.getValue();
            int i = 0;
            for (SourceInfo info : infoSet) {
                File fullFile = new File(baseDir, info.getFilename());
                bluej.setExtensionPropertyString(PROPERTY_FILENAME + i, fullFile.getPath());
                bluej.setExtensionPropertyString(PROPERTY_LASTMOD + i, Long.toString(info.getLastModified()));
                i++;
                fileCount++;
            }
            
            // Hmm, this doesn't work - causes an exception, BlueJ 3.0.4:
            // bluej.setExtensionPropertyString(PROPERTY_FILENAME + i, null);
            // bluej.setExtensionPropertyString(PROPERTY_LASTMOD + i, null);
        }
        
        bluej.setExtensionPropertyString(PROPERTY_NUMFILES, "" + fileCount);
        */
    }
    
    /**
     * Handle a compilation event - collection information and submit it to the
     * data collection server in another thread.
     * 
     * @param files  the files being compiled (or with the error)
     * @param lineNum  the line number of the error (ignored if errMsg is null)
     * @param errMsg  the error message, or null if the compilation succeeded
     */
    private void handleCompilationEvent(File[] files, int lineNum, String errMsg)
    {
        /*
        if (files == null || files.length == 0) {
            return;
        }
        
        File errFile = files[0];
        File errDir = errFile.getParentFile();
        
        // Find what package the error was in:
        BPackage errPkg = getPackageForDir(errDir);
        if (errPkg == null) {
            return;
        }
        
        // Determine which classes have changed, get their content
        try {
            BClass[] bclasses = errPkg.getClasses();
            List<SourceContent> changedFiles = new ArrayList<SourceContent>();
            
            classLoop:
            for (BClass bclass : bclasses) {
                File sourceFile = bclass.getJavaFile();
                long lastMod = sourceFile.lastModified();
                Set<SourceInfo> infoSet = srcInfoMap.get(sourceFile.getParentFile());
                
                checkExistingInfo:
                if (infoSet != null) {
                    for (SourceInfo info : infoSet) {
                        if (info.getFilename().equals(sourceFile.getName())) {
                            if (info.getLastModified() == lastMod) {
                                continue classLoop;
                            }
                            try {
                                changedFiles.add(new SourceContent(sourceFile));
                                info.setLastModified(lastMod);
                                break checkExistingInfo;
                            }
                            catch (IOException ioe) {
                                // Don't really want to cause issues, so just ignore it.
                                break checkExistingInfo;
                            }
                        }
                    }
                    // If we get here, there's no existing info for the file:
                    try {
                        infoSet.add(new SourceInfo(sourceFile.getName(), lastMod));
                        changedFiles.add(new SourceContent(sourceFile));
                    }
                    catch (IOException ioe) {}
                }
                else {
                    try {
                        infoSet = new HashSet<SourceInfo>();
                        infoSet.add(new SourceInfo(sourceFile.getName(), lastMod));
                        changedFiles.add(new SourceContent(sourceFile));
                        srcInfoMap.put(sourceFile.getParentFile(), infoSet);
                    }
                    catch (IOException ioe) {
                        // Just ignore.
                    }
                }
            }
            String errPath = (errMsg == null) ? null : errFile.getPath();
            //DataSubmitter.submit(uuid, lineNum, errMsg, errPath, changedFiles);
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
        */
    }
    
    /**
     * Find the open package corresponding to the given directory.
     */
    /*
    private BPackage getPackageForDir(File dir)
    {
        BPackage thePkg = null;
        for (BPackage pkg : openPkgs) {
            try {
                if (pkg.getDir().equals(dir)) {
                    thePkg = pkg;
                    break;
                }
            }
            catch (PackageNotFoundException pnfe) { }
            catch (ProjectNotOpenException pnoe) { }
        }
        return thePkg;
    }
    */
    /*
    public static void packageOpened(Package pkg)
    {
        if (dontSend()) return;
        
        //TODO should this do something?
    }
    */
    /*
    @Override
    public void packageClosing(PackageEvent event)
    {
        if (Config.isGreenfoot()) return;
        
        openPkgs.remove(event.getPackage());
        saveInfo();
    }
    */

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
        
        mpe.addPart("event[source_file_name]", toBodyLocal(project, sourceFile));
        mpe.addPart("event[line_number]", toBody(lineNumber));
        
        submitEvent(project, pkg, eventName, new PlainEvent(mpe));
    }


    private static StringBody toBodyLocal(Project project, File sourceFile)
    {
        return toBody(toPath(project, sourceFile));
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
        
        addStackTrace(mpe, "event[stack]", stack);
        
        submitEvent(project, null, eventName, new PlainEvent(mpe));
    }


    private static void addStackTrace(MultipartEntity mpe, String listName, SourceLocation[] stack)
    {
        for (int i = 0; i < stack.length; i++)
        {
            mpe.addPart(listName + "[][entry]", toBody(i));
            mpe.addPart(listName + "[][class_source_name]", toBody(stack[i].getFileName()));
            mpe.addPart(listName + "[][line_number]", toBody(stack[i].getLineNumber()));
        }
    }
    
    private static String md5Hash(String src)
    {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(src.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e) {
            //Oracle comes with MD5, unlikely that any other JDK wouldn't:
            Debug.reportError(e);
            return "";
        }
        catch (UnsupportedEncodingException e) {
            //Shouldn't happen -- no UTF-8?!
            Debug.reportError(e);
            return "";
        }
        StringBuilder s = new StringBuilder();
        for (byte b : hash)
        {
            s.append(String.format("%02X", b));
        }
        return s.toString();
    }
    
    private static synchronized void submitEvent(final Project project, final Package pkg, final EventName eventName, final Event evt)
    {
        if (dontSend()) return;
        
        final String projectName = project == null ? null : project.getProjectName();
        final String projectPathHash = project == null ? null : md5Hash(project.getProjectDir().getAbsolutePath());
        final String packageName = pkg == null ? null : pkg.getQualifiedName();
        // Must take copy to avoid problems with later modification:
        final int thisSequenceNum = sequenceNum;  
        
        /**
         * Wrap the Event we've been given to add the other normal expected fields:
         */
        DataSubmitter.submitEvent(new Event() {
            
            @Override public void success(Map<FileKey, List<String>> fileVersions)
            {
                evt.success(fileVersions);
            }
            
            @Override
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                MultipartEntity mpe = evt.makeData(sequenceNum, fileVersions);
                
                if (mpe == null)
                    return null;
                
                mpe.addPart("user[uuid]", toBody(uuid));
                mpe.addPart("session[id]", toBody(sessionUuid));
                if (projectName != null)
                {
                    mpe.addPart("project[name]", toBody(projectName));
                    mpe.addPart("project[path_hash]", toBody(projectPathHash));
                    
                    if (packageName != null)
                    {
                        mpe.addPart("package[name]", toBody(packageName));
                    }
                }
                
                mpe.addPart("event[source_time]", toBody(DateFormat.getDateTimeInstance().format(new Date())));
                mpe.addPart("event[name]", toBody(eventName.getName()));
                mpe.addPart("event[sequence_id]", toBody(Integer.toString(sequenceNum)));
                
                return mpe;
            }
        });
        sequenceNum += 1;
    }

    public static void compiled(Project proj, Package pkg, File[] sources, List<DiagnosticWithShown> diagnostics, boolean success)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[compile_success]", toBody(success));
        
        for (File src : sources)
        {
            mpe.addPart("event[compile_input][][source_file_name]", toBody(toPath(proj, src)));
        }        
        
        for (DiagnosticWithShown dws : diagnostics)
        {
            final Diagnostic d = dws.getDiagnostic();
            
            mpe.addPart("event[compile_output][][is_error]", toBody(d.getType() == Diagnostic.ERROR));
            mpe.addPart("event[compile_output][][shown]", toBody(dws.wasShownToUser()));
            mpe.addPart("event[compile_output][][message]", toBody(d.getMessage()));
            if (d.getFileName() != null)
            {
                mpe.addPart("event[compile_output][][start_line]", toBody(d.getStartLine()));
                mpe.addPart("event[compile_output][][end_line]", toBody(d.getEndLine()));
                mpe.addPart("event[compile_output][][start_column]", toBody(d.getStartColumn()));
                mpe.addPart("event[compile_output][][end_column]", toBody(d.getEndColumn()));
                // Must make file name relative for anonymisation:
                String relative = toPath(proj, new File(d.getFileName()));
                mpe.addPart("event[compile_output][][source_file_name]", toBody(relative));
            }
        }
        submitEvent(proj, pkg, EventName.COMPILE, new PlainEvent(mpe));
    }
    
    private static StringBody toBody(String s)
    {
        try {
            return new StringBody(s == null ? "" : s, utf8);
        }
        catch (UnsupportedEncodingException e) {
            // Shouldn't happen, because UTF-8 is required to be supported
            return null;
        }
    }
    
    private static StringBody toBody(int i)
    {
        return toBody(Integer.toString(i));
    }
    
    private static StringBody toBody(long l)
    {
        return toBody(Long.toString(l));
    }
    
    private static StringBody toBody(boolean b)
    {
        return toBody(Boolean.toString(b));
    }
    

    public static void bluejOpened(String osVersion, String javaVersion, String bluejVersion, String interfaceLanguage, List<ExtensionWrapper> extensions)
    {
        if (Config.isGreenfoot()) return; //Don't even look for UUID
        initUUidSequence();
        initSessionId();
        
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("installation[operating_system]", toBody(osVersion));
        mpe.addPart("installation[java_version]", toBody(javaVersion));
        mpe.addPart("installation[bluej_version]", toBody(bluejVersion));
        mpe.addPart("installation[interface_language]", toBody(interfaceLanguage));
        
        addExtensions(mpe, extensions);
        
        submitEvent(null, null, EventName.BLUEJ_START, new PlainEvent(mpe));
    }


    private static void addExtensions(MultipartEntity mpe,
            List<ExtensionWrapper> extensions)
    {
        for (ExtensionWrapper ext : extensions)
        {
            mpe.addPart("extensions[][name]", toBody(ext.safeGetExtensionName()));
            mpe.addPart("extensions[][version]", toBody(ext.safeGetExtensionVersion()));
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
    
    public static void packageOpened(Package pkg)
    {
        final Project proj = pkg.getProject();
        
        final MultipartEntity mpe = new MultipartEntity();
        
        final Map<FileKey, List<String>> versions = new HashMap<FileKey, List<String>>();
        
        //TODO what about README, .class files with no source
        for (ClassTarget ct : pkg.getClassTargets())
        {

            String relative = toPath(proj, ct.getSourceFile());
            
            mpe.addPart("project[source_files][][name]", toBody(relative));
            
            String anonymisedContent = readFileAndAnonymise(proj, ct.getSourceFile());
            
            if (anonymisedContent != null)
            {
                mpe.addPart("source_histories[][source_history_type]", toBody("complete"));
                mpe.addPart("source_histories[][name]", toBody(relative));
                mpe.addPart("source_histories[][content]", toBody(anonymisedContent));
                versions.put(new FileKey(proj, relative), Arrays.asList(Utility.splitLines(anonymisedContent)));
            }
        }
        
        submitEvent(proj, pkg, EventName.PACKAGE_OPENING, new Event() {
            
            @Override
            public void success(Map<FileKey, List<String>> fileVersions)
            {
                fileVersions.putAll(versions);                
            }
            
            @Override
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                return mpe;
            }
        });
    }
    
    public static void packageClosed(Package pkg)
    {
        submitEventNoData(pkg.getProject(), pkg, EventName.PACKAGE_CLOSING);
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

    public static void edit(final Package pkg, final File path, final String source, final boolean includeOneLineEdits)
    {
        final Project proj = pkg.getProject();
        final FileKey key = new FileKey(proj, toPath(proj, path));
        final String anonSource = CodeAnonymiser.anonymise(source);
        final List<String> anonDoc = Arrays.asList(Utility.splitLines(anonSource));
                
        submitEvent(proj, pkg, EventName.EDIT, new Event() {

            private boolean dontReplace = false;
            
            //Edit solely within one line
            private boolean isOneLineDiff(Patch patch)
            {
                if (patch.getDeltas().size() > 1)
                    return false;
                Delta theDelta = patch.getDeltas().get(0);
                return theDelta.getOriginal().size() == 1 && theDelta.getRevised().size() == 1;
            }
            
            
            @Override
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                List<String> previousDoc = fileVersions.get(key);
                if (previousDoc == null)
                    previousDoc = new ArrayList<String>(); // Diff against empty file
                
                MultipartEntity mpe = new MultipartEntity();
                
                Patch patch = DiffUtils.diff(previousDoc, anonDoc);
                
                if (patch.getDeltas().isEmpty() || (isOneLineDiff(patch) && !includeOneLineEdits))
                {
                    dontReplace = true;
                    return null;
                }
                
                StringBuilder diff = new StringBuilder();
                
                for (Delta delta: patch.getDeltas()) {
                    diff.append("@@ -" + (delta.getOriginal().getPosition() + 1) + "," + delta.getOriginal().size() + " +" + (delta.getRevised().getPosition() + 1) + "," + delta.getRevised().size() + " @@\n");
                    for (String l : (List<String>)delta.getOriginal().getLines())
                    {
                        diff.append("-" + l + "\n");
                    }
                    for (String l : (List<String>)delta.getRevised().getLines())
                    {
                        diff.append("+" + l + "\n");
                    }
                }
                
                mpe.addPart("source_histories[][content]", toBody(diff.toString()));
                mpe.addPart("source_histories[][source_history_type]", toBody("diff"));
                mpe.addPart("source_histories[][name]", toBody(toPath(proj, path))); 
                
                return mpe;
            }


            @Override
            public void success(Map<FileKey, List<String>> fileVersions)
            {
                if (!dontReplace)
                {
                    fileVersions.put(key, anonDoc);
                }
            }
            
            
        });
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
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEvent(project, null, EventName.DEBUGGER_CONTINUE, new PlainEvent(mpe));        
    }

    public static void debuggerHalt(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_HALT, mpe, stack);
    }
    
    public static void debuggerStepInto(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_STEP_INTO, mpe, stack);
    }
    
    public static void debuggerStepOver(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_STEP_OVER, mpe, stack);
    }
    
    public static void debuggerHitBreakpoint(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitDebuggerEventWithLocation(project, EventName.DEBUGGER_HIT_BREAKPOINT, mpe, stack);
    }
    
    public static void codePadSuccess(Package pkg, String command, String output)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", toBody("success"));
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][result]", toBody(output));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
    }
    
    public static void codePadError(Package pkg, String command, String error)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", toBody("error"));
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][error]", toBody(error));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
    }
    
    public static void codePadException(Package pkg, String command, String exception)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][outcome]", toBody("exception"));
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][exception]", toBody(exception));
        submitEvent(pkg.getProject(), pkg, EventName.CODEPAD, new PlainEvent(mpe));
    }


    public static void renamedClass(Package pkg, File oldSourceFile, File newSourceFile)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("source_histories[][source_history_type]", toBody("rename"));
        mpe.addPart("source_histories[][content]", toBodyLocal(pkg.getProject(), oldSourceFile));
        mpe.addPart("source_histories[][name]", toBodyLocal(pkg.getProject(), newSourceFile));
        submitEvent(pkg.getProject(), pkg, EventName.RENAME, new PlainEvent(mpe));
    }
    
    public static void removeClass(Package pkg, File sourceFile)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("source_histories[][source_history_type]", toBody("file_delete"));
        mpe.addPart("source_histories[][name]", toBodyLocal(pkg.getProject(), sourceFile));
        submitEvent(pkg.getProject(), pkg, EventName.DELETE, new PlainEvent(mpe));
    }
    
    public static void addClass(Package pkg, File sourceFile)
    {
        final MultipartEntity mpe = new MultipartEntity();
        final Project project = pkg.getProject();
        
        final String contents = readFileAndAnonymise(project, sourceFile);
        
        mpe.addPart("project[source_files][][name]", toBodyLocal(project, sourceFile));
        mpe.addPart("source_histories[][source_history_type]", toBody("complete"));
        mpe.addPart("source_histories[][name]", toBodyLocal(project, sourceFile));
        mpe.addPart("source_histories[][text]", toBody(contents));
        final FileKey key = new FileKey(project, toPath(project, sourceFile));
        
        submitEvent(project, pkg, EventName.ADD, new Event() {
            
            @Override
            public void success(Map<FileKey, List<String>> fileVersions)
            {
                fileVersions.put(key, Arrays.asList(Utility.splitLines(contents)));
            }
            
            @Override
            public MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String>> fileVersions)
            {
                return mpe;
            }
        });
    }
    
    private static MultipartEntity getRepoMPE(Repository repo)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[vcs][vcs_type]", toBody(repo.getVCSType()));
        mpe.addPart("event[vcs][protocol]", toBody(repo.getVCSProtocol()));
        return mpe;
    }

    public static void teamShareProject(Project project, Repository repo)
    {
        submitEvent(project, null, EventName.VCS_SHARE, new PlainEvent(getRepoMPE(repo)));    
    }
    
    public static void teamCommitProject(Project project, Repository repo, Collection<File> committedFiles)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (File f : committedFiles)
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, f));
        }
        submitEvent(project, null, EventName.VCS_COMMIT, new PlainEvent(mpe));
    }
    
    public static void teamUpdateProject(Project project, Repository repo, Collection<File> updatedFiles)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (File f : updatedFiles)
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, f));
        }
        submitEvent(project, null, EventName.VCS_UPDATE, new PlainEvent(mpe));
    }
    
    public static void teamStatusProject(Project project, Repository repo, Map<File, String> status)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (Map.Entry<File, String> s : status.entrySet())
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, s.getKey()));
            mpe.addPart("vcs_files[][status]", toBody(s.getValue()));
        }
        submitEvent(project, null, EventName.VCS_STATUS, new PlainEvent(mpe));
    }
    
    public static void teamHistoryProject(Project project, Repository repo)
    {
        submitEvent(project, null, EventName.VCS_HISTORY, new PlainEvent(getRepoMPE(repo)));    
    }


    public static void showHideTerminal(Project project, boolean show)
    {
        submitEventNoData(project, null, show ? EventName.TERMINAL_OPEN : EventName.TERMINAL_CLOSE);      
    }

    
    public static void invokeCompileError(Package pkg, String code, String compilationError)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[invoke][code]", toBody(code));
        mpe.addPart("event[invoke][result]", toBody("compile_error"));
        mpe.addPart("event[invoke][compile_error]", toBody(compilationError));
        submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));        
    }
    
    public static void invokeMethodSuccess(Package pkg, String code, String objName, String typeName, int testIdentifier, int invocationIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[invoke][code]", toBody(code));
        mpe.addPart("event[invoke][type_name]", toBody(typeName));
        mpe.addPart("event[invoke][result]", toBody("success"));
        mpe.addPart("event[invoke][test_identifier]", toBody(testIdentifier));
        mpe.addPart("event[invoke][invoke_identifier]", toBody(invocationIdentifier));
        if (objName != null)
        {
            mpe.addPart("event[invoke][bench_object][class_name]", toBody(typeName));
            mpe.addPart("event[invoke][bench_object][name]", toBody(objName));
        }
        submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));        
    }
    
    public static void invokeMethodException(Package pkg, String code, ExceptionDescription ed)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[invoke][code]", toBody(code));
        mpe.addPart("event[invoke][result]", toBody("exception"));
        mpe.addPart("event[invoke][exception_class]", toBody(ed.getClassName()));
        mpe.addPart("event[invoke][exception_message]", toBody(ed.getText()));
        addStackTrace(mpe, "event[invoke][exception_stack]", ed.getStack().toArray(new SourceLocation[0]));
        submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));        
    }
    
    public static void invokeMethodTerminated(Package pkg, String code)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[invoke][code]", toBody(code));
        mpe.addPart("event[invoke][result]", toBody("terminated"));
        submitEvent(pkg.getProject(), pkg, EventName.INVOKE_METHOD, new PlainEvent(mpe));        
    }
    
    public static void removeObject(Package pkg, String name)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[object_name]", toBody(name));
        submitEvent(pkg.getProject(), pkg, EventName.REMOVE_OBJECT, new PlainEvent(mpe));
    }


    public static void inspectorClassShow(Package pkg, Inspector inspector, String className)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[inspect][unique]", toBody(inspector.getUniqueId()));
        mpe.addPart("event[inspect][static_class]", toBody(className));
        inspectorPackages.put(inspector, pkg);
        submitEvent(pkg.getProject(), pkg, EventName.INSPECTOR_SHOW, new PlainEvent(mpe));
    }
    
    public static void inspectorObjectShow(Package pkg, Inspector inspector, String benchName, String className, String displayName)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[inspect][unique]", toBody(inspector.getUniqueId()));
        mpe.addPart("event[inspect][display_name]", toBody(displayName));
        mpe.addPart("event[inspect][class_name]", toBody(className));
        if (benchName != null)
        {
            mpe.addPart("event[inspect][bench_object_name]", toBody(benchName));
        }
        inspectorPackages.put(inspector, pkg);
        submitEvent(pkg.getProject(), pkg, EventName.INSPECTOR_SHOW, new PlainEvent(mpe));
    }
    
    public static void inspectorHide(Project project, Inspector inspector)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[inspect][unique]", toBody(inspector.getUniqueId()));
        if (inspector instanceof ClassInspector || inspector instanceof ObjectInspector)
        {
            submitEvent(project, inspectorPackages.get(project), EventName.INSPECTOR_HIDE, new PlainEvent(mpe));
        }
    }


    public static void benchGet(Package pkg, String benchName, String typeName, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[bench_object][class_name]", toBody(typeName));
        mpe.addPart("event[bench_object][name]", toBody(benchName));
        mpe.addPart("event[test_identifier]", toBody(testIdentifier));
        submitEvent(pkg.getProject(), pkg, EventName.BENCH_GET, new PlainEvent(mpe));
        
    }

    public static void startTestMethod(Package pkg, int testIdentifier, File sourceFile, String testName)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", toBody(testIdentifier));
        mpe.addPart("event[test][source_file]", toBodyLocal(pkg.getProject(), sourceFile));
        mpe.addPart("event[test][method_name]", toBody(testName));
        
        submitEvent(pkg.getProject(), pkg, EventName.START_TEST, new PlainEvent(mpe));
        
    }

    public static void cancelTestMethod(Package pkg, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", toBody(testIdentifier));
        
        submitEvent(pkg.getProject(), pkg, EventName.CANCEL_TEST, new PlainEvent(mpe));
    }

    public static void endTestMethod(Package pkg, int testIdentifier)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[test][test_identifier]", toBody(testIdentifier));
        
        submitEvent(pkg.getProject(), pkg, EventName.END_TEST, new PlainEvent(mpe));
        
    }

    public static void assertTestMethod(Package pkg, int testIdentifier, int invocationIdentifier, 
            String assertion, String param1, String param2)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[assert][test_identifier]", toBody(testIdentifier));
        mpe.addPart("event[assert][invoke_identifier]", toBody(invocationIdentifier));
        mpe.addPart("event[assert][assertion]", toBody(assertion));
        mpe.addPart("event[assert][param1]", toBody(param1));
        mpe.addPart("event[assert][param2]", toBody(param2));
        
        submitEvent(pkg.getProject(), pkg, EventName.ASSERTION, new PlainEvent(mpe));
        
    }

    public static void objectBenchToFixture(Package pkg, File sourceFile, List<String> benchNames)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[source_file_name]", toBodyLocal(pkg.getProject(), sourceFile));
        for (String name : benchNames)
        {
            mpe.addPart("event[bench_objects][][name]", toBody(name));
        }        
        
        submitEvent(pkg.getProject(), pkg, EventName.BENCH_TO_FIXTURE, new PlainEvent(mpe));
        
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
        
        
    };
    
    public static void fixtureToObjectBench(Package pkg, File sourceFile, List<NamedTyped> objects)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("event[source_file_name]", toBodyLocal(pkg.getProject(), sourceFile));
        for (NamedTyped obj : objects)
        {
            mpe.addPart("event[bench_objects][][name]", toBody(obj.getName()));
            mpe.addPart("event[bench_objects][][type]", toBody(obj.getType()));
        }        
        
        submitEvent(pkg.getProject(), pkg, EventName.FIXTURE_TO_BENCH, new PlainEvent(mpe));
    }
}
