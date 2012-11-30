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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import bluej.Config;
import bluej.collect.DataSubmitter.FileKey;
import bluej.compiler.Diagnostic;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.groupwork.Repository;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
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
    private static final String VERSION = "1.0";
    
    private static final String PROPERTY_UUID = "blackbox.uuid";
    
    private static final Charset utf8 = Charset.forName("UTF-8");

    //private List<BPackage> openPkgs = new ArrayList<BPackage>();
    
    /** Whether we've seen the first error in the current compilation yet */
    //private boolean seenFirstError;
    
    private static String uuid;
    private static int sequenceNum;
    
    /** map from package directory to information on the sources contained within */
    //private Map<File,Set<SourceInfo>> srcInfoMap = new HashMap<File,Set<SourceInfo>>();

    private static ArrayList<String> splitLines(String s)
    {
        ArrayList<String> r = new ArrayList<String>();
             
        int i, prev = 0;
        for (i = s.indexOf('\n'); i != -1; i = s.indexOf('\n', i+1))
        {
            r.add(s.substring(prev, i + 1));
            prev = i + 1;
        }
        
        if (prev < s.length() - 1)
        {
            r.add(s.substring(prev, s.length()));
        }
        return r;
    }
       
    
    private static String readFile(Project proj, File f)
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
            return sb.toString();
        }
        catch (IOException ioe) {return null;}
    }

    private static String toPath(Project proj, File f)
    {
        return proj.getProjectDir().toURI().relativize(f.toURI()).getPath();
    }
    
    private static boolean dontSend()
    {
        return Config.isGreenfoot(); //TODO or opted out or send failed
    }

    private static void initUUidSequence()
    {
        sequenceNum = 1;
        uuid = Config.getPropString(PROPERTY_UUID, null);
        if (uuid == null)
        {
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
    private static void submitEventNoData(Project project, EventName eventName)
    {
        submitEvent(project, eventName, new PlainEvent(new MultipartEntity()));
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
    private static void submitEventWithLocalLocation(Project project, EventName eventName, MultipartEntity mpe, File sourceFile, int lineNumber)
    {
        if (mpe == null)
        {
            mpe = new MultipartEntity();
        }
        
        mpe.addPart("event[source_file_name]", toBodyLocal(project, sourceFile));
        mpe.addPart("event[line_number]", toBody(lineNumber));
        
        submitEvent(project, eventName, new PlainEvent(mpe));
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
    private static void submitEventWithDebuggerLocation(Project project, EventName eventName, MultipartEntity mpe, SourceLocation[] stack)
    {
        if (mpe == null)
        {
            mpe = new MultipartEntity();
        }
        
        addStackTrace(mpe, "event[stack]", stack);
        
        submitEvent(project, eventName, new PlainEvent(mpe));
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
    
    private static synchronized void submitEvent(final Project project, final EventName eventName, final DataSubmitter.Event evt)
    {
        if (dontSend()) return;
        
        final String projectName = project == null ? "" : project.getProjectName();
        final String projectPathHash = project == null ? "" : md5Hash(project.getProjectDir().getAbsolutePath());
        // Must take copy to avoid problems with later modification:
        final int thisSequenceNum = sequenceNum;  
        
        /**
         * Wrap the Event we've been given to add the other normal expected fields:
         */
        DataSubmitter.submitEvent(new DataSubmitter.Event() {
            
            @Override public void success(Map<FileKey, ArrayList<String>> fileVersions)
            {
                evt.success(fileVersions);
            }
            
            @Override
            public MultipartEntity makeData(Map<FileKey, ArrayList<String>> fileVersions)
            {
                MultipartEntity mpe = evt.makeData(fileVersions);
                
                if (mpe == null)
                    return null;
                
                mpe.addPart("user[uuid]", toBody(uuid));        
                mpe.addPart("project[name]", toBody(projectName));
                mpe.addPart("project[path_hash]", toBody(projectPathHash));
                mpe.addPart("event[source_time]", toBody(DateFormat.getDateTimeInstance().format(new Date())));
                mpe.addPart("event[name]", toBody(eventName.getName()));
                mpe.addPart("event[sequence_id]", toBody(Integer.toString(thisSequenceNum)));
                
                return mpe;
            }
        });
        sequenceNum += 1;
    }

    public static void compiled(Project proj, File[] sources, List<Diagnostic> diagnostics)
    {
        MultipartEntity mpe = new MultipartEntity();
        for (Diagnostic d : diagnostics)
        {
            mpe.addPart("event[compile_output][][is_error]", toBody(d.getType() == Diagnostic.ERROR));
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
            //TODO have a flag indicated whether the error was shown to the user
        }
        submitEvent(proj, EventName.COMPILE, new PlainEvent(mpe));
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
    

    public static void bluejOpened(String osVersion, String javaVersion, String bluejVersion, String interfaceLanguage)
    {
        if (dontSend()) return;
        initUUidSequence();
        
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("installation[operating_system]", toBody(osVersion));
        mpe.addPart("installation[java_version]", toBody(javaVersion));
        mpe.addPart("installation[bluej_version]", toBody(bluejVersion));
        mpe.addPart("installation[interface_language]", toBody(interfaceLanguage));
        
        submitEvent(null, EventName.BLUEJ_START, new PlainEvent(mpe));
    }
    
    public static void projectOpened(Project proj)
    {   
        final MultipartEntity mpe = new MultipartEntity();
        
        final Map<FileKey, ArrayList<String>> versions = new HashMap<FileKey, ArrayList<String>>();
        
        for (File f : proj.getFilesInProject(false, true))
        {
            if (f.getPath().toLowerCase().endsWith(".java"))
            {
                String relative = toPath(proj, f);
                
                mpe.addPart("project[source_files][][name]", toBody(relative));
                
                String contents = readFile(proj, f);
                
                if (contents != null)
                {
                    mpe.addPart("source_histories[][source_history_type]", toBody("complete"));
                    mpe.addPart("source_histories[][name]", toBody(relative));
                    mpe.addPart("source_histories[][text]", toBody(contents));
                    versions.put(new FileKey(proj, relative), splitLines(contents));
                }
            }
        }
        
        submitEvent(proj, EventName.PROJECT_OPENING, new DataSubmitter.Event() {
            
            @Override
            public void success(Map<FileKey, ArrayList<String>> fileVersions)
            {
                fileVersions.putAll(versions);                
            }
            
            @Override
            public MultipartEntity makeData(Map<FileKey, ArrayList<String>> fileVersions)
            {
                return mpe;
            }
        });
    }

    public static void projectClosed(Project proj)
    {
        submitEventNoData(proj, EventName.PROJECT_CLOSING);
    }

    public static void bluejClosed()
    {
        submitEventNoData(null, EventName.BLUEJ_FINISH);        
    }

    public static void restartVM(Project project)
    {
        submitEventNoData(project, EventName.RESETTING_VM);        
    }

    public static void edit(final Project proj, final File path, final ArrayList<String> curDoc, final boolean includeOneLineEdits)
    {
        final FileKey key = new FileKey(proj, toPath(proj, path));
        
        submitEvent(proj, EventName.MULTI_LINE_EDIT, new DataSubmitter.Event() {

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
            public MultipartEntity makeData(Map<FileKey, ArrayList<String>> fileVersions)
            {
                ArrayList<String> previousDoc = fileVersions.get(key);
                if (previousDoc == null)
                    previousDoc = new ArrayList<String>(); // Diff against empty file
                
                MultipartEntity mpe = new MultipartEntity();
                
                Patch patch = DiffUtils.diff(previousDoc, curDoc);
                
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
                        diff.append("-" + l); //l already has newline in it
                    }
                    for (String l : (List<String>)delta.getRevised().getLines())
                    {
                        diff.append("+" + l); //l already has newline in it
                    }
                }
                
                mpe.addPart("source_histories[][diff]", toBody(diff.toString()));
                mpe.addPart("source_histories[][source_history_type]", toBody("multi_line_edit"));
                mpe.addPart("source_histories[][name]", toBody(toPath(proj, path))); 
                
                return mpe;
            }


            @Override
            public void success(Map<FileKey, ArrayList<String>> fileVersions)
            {
                if (!dontReplace)
                {
                    fileVersions.put(key, curDoc);
                }
            }
            
            
        });
    }
    
    
    public static void debuggerTerminate(Project project)
    {
        submitEventNoData(project, EventName.DEBUGGER_TERMINATE);        
    }
    public static void debuggerChangeVisible(Project project, boolean newVis)
    {
        submitEventNoData(project, newVis ? EventName.DEBUGGER_OPEN : EventName.DEBUGGER_CLOSE);        
    }
    public static void debuggerBreakpointToggle(Project project, File sourceFile, int lineNumber, boolean newState)
    {
        submitEventWithLocalLocation(project, newState ? EventName.DEBUGGER_BREAKPOINT_ADD : EventName.DEBUGGER_BREAKPOINT_REMOVE, null, sourceFile, lineNumber);
    }
    public static void debuggerContinue(Project project, String threadName)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEvent(project, EventName.DEBUGGER_CONTINUE, new PlainEvent(mpe));        
    }

    public static void debuggerHalt(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEventWithDebuggerLocation(project, EventName.DEBUGGER_HALT, mpe, stack);
    }
    
    public static void debuggerStepInto(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEventWithDebuggerLocation(project, EventName.DEBUGGER_STEP_INTO, mpe, stack);
    }
    
    public static void debuggerStepOver(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEventWithDebuggerLocation(project, EventName.DEBUGGER_STEP_OVER, mpe, stack);
    }
    
    public static void debuggerHitBreakpoint(Project project, String threadName, SourceLocation[] stack)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[thread_name]", toBody(threadName));
        submitEventWithDebuggerLocation(project, EventName.DEBUGGER_HIT_BREAKPOINT, mpe, stack);
    }
    
    public static void codePadSuccess(Project project, String command, String output)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][result]", toBody(output));
        submitEvent(project, EventName.CODEPAD_SUCCESS, new PlainEvent(mpe));
    }
    
    public static void codePadError(Project project, String command, String error)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][error]", toBody(error));
        submitEvent(project, EventName.CODEPAD_ERROR, new PlainEvent(mpe));
    }
    
    public static void codePadException(Project project, String command, String exception)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("event[codepad][command]", toBody(command));
        mpe.addPart("event[codepad][exception]", toBody(exception));
        submitEvent(project, EventName.CODEPAD_EXCEPTION, new PlainEvent(mpe));
    }


    public static void renamedClass(Project project, File oldSourceFile, File newSourceFile)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("source_histories[][source_history_type]", toBody("rename"));
        mpe.addPart("source_histories[][oldname]", toBodyLocal(project, oldSourceFile));
        mpe.addPart("source_histories[][name]", toBodyLocal(project, newSourceFile));
        submitEvent(project, EventName.RENAME, new PlainEvent(mpe));
    }
    
    public static void removeClass(Project project, File sourceFile)
    {
        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart("source_histories[][source_history_type]", toBody("file_delete"));
        mpe.addPart("source_histories[][name]", toBodyLocal(project, sourceFile));
        submitEvent(project, EventName.DELETE, new PlainEvent(mpe));
    }
    
    public static void addClass(Project project, File sourceFile)
    {
        final MultipartEntity mpe = new MultipartEntity();
        
        final String contents = readFile(project, sourceFile);
                
        if (contents == null)
        {
            return;
        }
        
        mpe.addPart("project[source_files][][name]", toBodyLocal(project, sourceFile));
        mpe.addPart("source_histories[][source_history_type]", toBody("complete"));
        mpe.addPart("source_histories[][name]", toBodyLocal(project, sourceFile));
        mpe.addPart("source_histories[][text]", toBody(contents));
        final FileKey key = new FileKey(project, toPath(project, sourceFile));
        
        submitEvent(project, EventName.ADD, new DataSubmitter.Event() {
            
            @Override
            public void success(Map<FileKey, ArrayList<String>> fileVersions)
            {
                fileVersions.put(key, splitLines(contents));                
            }
            
            @Override
            public MultipartEntity makeData(Map<FileKey, ArrayList<String>> fileVersions)
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
        submitEvent(project, EventName.VCS_SHARE, new PlainEvent(getRepoMPE(repo)));    
    }
    
    public static void teamCommitProject(Project project, Repository repo, Collection<File> committedFiles)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (File f : committedFiles)
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, f));
        }
        submitEvent(project, EventName.VCS_COMMIT, new PlainEvent(mpe));
    }
    
    public static void teamUpdateProject(Project project, Repository repo, Collection<File> updatedFiles)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (File f : updatedFiles)
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, f));
        }
        submitEvent(project, EventName.VCS_UPDATE, new PlainEvent(mpe));
    }
    
    public static void teamStatusProject(Project project, Repository repo, Map<File, String> status)
    {
        MultipartEntity mpe = getRepoMPE(repo);
        for (Map.Entry<File, String> s : status.entrySet())
        {
            mpe.addPart("vcs_files[][file]", toBodyLocal(project, s.getKey()));
            mpe.addPart("vcs_files[][status]", toBody(s.getValue()));
        }
        submitEvent(project, EventName.VCS_STATUS, new PlainEvent(mpe));
    }
    
    public static void teamHistoryProject(Project project, Repository repo)
    {
        submitEvent(project, EventName.VCS_HISTORY, new PlainEvent(getRepoMPE(repo)));    
    }


    public static void showHideTerminal(Project project, boolean show)
    {
        submitEventNoData(project, show ? EventName.TERMINAL_OPEN : EventName.TERMINAL_CLOSE);      
    }


    public static void invokeDefaultConstructor(Project project, String className, String objName, DebuggerResult result)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("invoke[class_name]", toBody(className));
        mpe.addPart("invoke[result_name]", toBody(objName));
        addInvokeResult(mpe, result);        
        submitEvent(project, EventName.INVOKE_DEFAULT_CONSTRUCTOR, new PlainEvent(mpe));        
    }


    private static void addInvokeResult(MultipartEntity mpe, DebuggerResult result)
    {
        ExceptionDescription ed;
        switch (result.getExitStatus())
        {
        case Debugger.NORMAL_EXIT:
            mpe.addPart("invoke[result_type]", toBody("success"));
            break;
        case Debugger.TERMINATED:
            mpe.addPart("invoke[result_type]", toBody("terminated"));
            break;
        case Debugger.EXCEPTION:
            mpe.addPart("invoke[result_type]", toBody("exception"));
            ed = result.getException();
            mpe.addPart("invoke[result_exception_class]", toBody(ed.getClassName()));
            mpe.addPart("invoke[result_exception_message]", toBody(ed.getText()));
            addStackTrace(mpe, "invoke[result_exception_stack", ed.getStack().toArray(new SourceLocation[0]));
            break;
        default:
            mpe.addPart("invoke[result_type]", toBody("unknown"));
            break;
        }
    }
}
