/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011,2014,2015,2016,2017,2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.extensions2.SourceType;
import bluej.parser.ConstructorOrMethodReflective;
import bluej.parser.JavadocParser;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.symtab.ClassInfo;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.Comment;
import bluej.views.View;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves javadoc from classes within a project.
 * 
 * @author Davin McCall
 */
public class ProjectJavadocResolver implements JavadocResolver
{
    private Project project;
    private CommentCache commentCache = new CommentCache();
    
    public ProjectJavadocResolver(Project project)
    {
        this.project = project;
    }

    /**
     * Retrieve the javadoc for the specified method, if possible. The javadoc and
     * method parameter names will be added to the supplied MethodReflective(s).
     * The collection of methods must all come from the same declaring type.
     */
    @Override
    public void getJavadoc(Reflective declaring,
            Collection<? extends ConstructorOrMethodReflective> targetMethods)
    {
        if (targetMethods.isEmpty()) {
            return; // Nothing to do
        }

        String declName = declaring.getName();
        // The collection of reflectives (indexed by unique signature) which we still
        // need to find Javadoc for.  As we find the Javadoc, we will remove from this collection:
        Map<String, ConstructorOrMethodReflective> methodSigs =
                targetMethods.stream().collect(
                        Collectors.toMap(ProjectJavadocResolver::buildSig,  // map signature ...
                                m -> m,                                     // ... to value
                                (a,b) -> a)                                 // merging duplicate keys
                        );
        
        try {
            Class<?> cl = project.getClassLoader().loadClass(declName);
            View clView = View.getView(cl);
            List<CallableView> methods = Utility.concat(Arrays.asList(clView.getAllMethods()), Arrays.asList(clView.getConstructors()));

            for (CallableView method : methods)
            {
                String signature = method.getSignature();
                ConstructorOrMethodReflective methodReflective = methodSigs.get(signature);
                if (methodReflective != null) {
                    Comment comment = method.getComment();
                    if (comment != null) {
                        methodReflective.setJavaDoc(comment.getText());
                        List<String> paramNames = new ArrayList<String>(comment.getParamCount());
                        for (int j = 0; j < comment.getParamCount(); j++) {
                            paramNames.add(comment.getParamName(j));
                        }
                        methodReflective.setParamNames(paramNames);
                        methodSigs.remove(signature);
                    }
                }
            }
        }
        catch (ClassNotFoundException cnfe) {}
        catch (LinkageError e) {}

        // If we've found all methods, nothing more to do, so stop now:
        if (methodSigs.isEmpty()) {
            return;
        }
        
        Properties comments = commentCache.get(declName);
        if (comments == null) {
            ClassInfo classInfo = getClassInfoFromSource(declaring.getModuleName(), declName);
            if (classInfo != null)
                comments = classInfo.getComments(); 
            if (comments == null) {
                // Record a blank so we don't bother looking next time:
                commentCache.put(declName, new Properties());
                return;
            }
            commentCache.put(declName, comments);
        }

        // Find the comment for the particular method we want
        for (int i = 0; ; i++) {
            String comtarget = comments.getProperty("comment" + i + ".target");
            // If there's no more method comments to scan, stop (otherwise we'll go on forever):
            if (comtarget == null) {
                break;
            }
            ConstructorOrMethodReflective methodReflective = methodSigs.get(comtarget);
            if (methodReflective != null) {
                methodReflective.setJavaDoc(comments.getProperty("comment" + i + ".text"));
                String paramNames = comments.getProperty("comment" + i + ".params");
                StringTokenizer tokenizer = new StringTokenizer(paramNames);
                List<String> paramNamesList = new ArrayList<String>();
                while (tokenizer.hasMoreTokens()) {
                    paramNamesList.add(tokenizer.nextToken());
                }
                methodReflective.setParamNames(paramNamesList);
                methodSigs.remove(comtarget);
            }
        }

        // If we reach here and there's methods remaining, there's simply no Javadoc for those methods.
        // Must record this fact to prevent needlessly scanning them again:
        for (ConstructorOrMethodReflective methodReflective : methodSigs.values()) {
            methodReflective.setJavaDoc("");
        }
    }
        
    @Override
    public boolean getJavadocAsync(final ConstructorOrMethodReflective method, final AsyncCallback callback, Executor executor)
    {
        Reflective declaring = method.getDeclaringType();
        final String declName = declaring.getName();
        final String methodSig = buildSig(method);
        
        try {
            Class<?> cl = project.getClassLoader().loadClass(declName);
            View clView = View.getView(cl);
            CallableView[] methods = method instanceof MethodReflective ? clView.getAllMethods() : clView.getConstructors();
            
            for (int i = 0; i < methods.length; i++) {
                if (methodSig.equals(methods[i].getSignature())) {
                    Comment comment = methods[i].getComment();
                    if (comment != null) {
                        method.setJavaDoc(comment.getText());
                        List<String> paramNames = new ArrayList<String>(comment.getParamCount());
                        for (int j = 0; j < comment.getParamCount(); j++) {
                            paramNames.add(comment.getParamName(j));
                        }
                        method.setParamNames(paramNames);
                        callback.gotJavadoc(method);
                        return true;
                    }
                    break;
                }
            }
        }
        catch (ClassNotFoundException cnfe) {}
        catch (LinkageError e) {}

        Properties comments = commentCache.get(declName);
        if (comments == null) {
            // Note: this is no longer async, but actually as it stands
            // this method isn't being used anyway...
            //executor.execute(new Runnable() {
                //@Override
                //@OnThread(value = Tag.Worker, ignoreParent = true)
                //public void run()
                //{
                    comments = getCommentsFromSource(declName);
                    if (comments == null) {
                        //Platform.runLater(() -> {
                            // Javadoc not available; must notify callback.
                            callback.gotJavadoc(method);
                        //});
                        //return;
                    }
                    
                    //Platform.runLater(() -> {
                        commentCache.put(declName, comments);
                        findMethodComment(comments, callback, method, methodSig, true);
                    //});
                //}
            //});
            return false;
        }
        else {
            findMethodComment(comments, callback, method, methodSig, false);
            return true;
        }
    }
    
    /**
     * Search a set of comments for different targets to find the target we want.
     * Apply the found comment/parameter names to the method reflective, and
     * optionally notify the callback.
     * 
     * @param comments   The set of comments to search
     * @param callback   The callback to notify (if postOnQueue is true)
     * @param method     The method reflective to update
     * @param methodSig  The method signature to search for
     * @param postOnQueue  Whether to notify the callback
     */
    private void findMethodComment(final Properties comments, final AsyncCallback callback,
            final ConstructorOrMethodReflective method, String methodSig, boolean postOnQueue)
    {
        // Find the comment for the particular method we want
        for (int i = 0; ; i++) {
            String comtarget = comments.getProperty("comment" + i + ".target");
            if (comtarget == null) {
                break;
            }
            if (comtarget.equals(methodSig)) {
                String paramNames = comments.getProperty("comment" + i + ".params");
                String javadoc = comments.getProperty("comment" + i + ".text");
                StringTokenizer tokenizer = new StringTokenizer(paramNames);
                List<String> paramNamesList = new ArrayList<String>();
                while (tokenizer.hasMoreTokens()) {
                    paramNamesList.add(tokenizer.nextToken());
                }
                method.setJavaDoc(javadoc);
                method.setParamNames(paramNamesList);
                break;
            }
        }
        
        // We may or may not find the javadoc, notify the callback that the search has finished.
        if (postOnQueue) {
            callback.gotJavadoc(method);
        }
    }

    /**
     * Find the javadoc for a given class (target) by searching the project source path.
     * In particular, this normally includes the JDK source. When source for the required
     * class is found, it is parsed to extract comments.
     * 
     * @param moduleName The module name if known and applicable.  May be null.
     * @param target The fully-qualified class name.
     * @return The discovered class info, or null if not found.
     */
    private ClassInfo getClassInfoFromSource(String moduleName, String target)
    {
        List<DocPathEntry> sourcePath = project.getSourcePath();
        String pkg = JavaNames.getPrefix(target);
        String entName = target.replace('.', '/') + "." + SourceType.Java.toString().toLowerCase();
        String entNameFs = target.replace('.', File.separatorChar) + "." + SourceType.Java.toString().toLowerCase();
        EntityResolver resolver = new PackageResolver(project.getEntityResolver(), pkg);
        
        for (DocPathEntry pathEntry : sourcePath) {
            File jarFile = pathEntry.getFile();
            if (jarFile.isFile()) {
                String fullEntryName = pathEntry.getPathPrefix();
                if (fullEntryName.length() != 0 && !fullEntryName.endsWith("/")) {
                    fullEntryName += "/";
                }
                fullEntryName += entName;
                Reader r = null;
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    List<String> possibleEntries = new ArrayList<>();
                    possibleEntries.add(fullEntryName);
                    if (moduleName != null)
                    {
                        possibleEntries.add(moduleName + "/" + fullEntryName);
                    }
                    for (String entryName : possibleEntries)
                    {
                        ZipEntry zipEnt = zipFile.getEntry(entryName);
                        if (zipEnt != null)
                        {
                            InputStream zeis = zipFile.getInputStream(zipEnt);
                            r = new InputStreamReader(zeis, project.getProjectCharset());
                            ClassInfo info = JavadocParser.parse(r, resolver, null);
                            if (info == null)
                            {
                                return null;
                            }
                            return info;
                        }
                    }
                }
                catch (IOException ioe) {}
                finally {
                    if (r != null) {
                        try {
                            r.close();
                        }
                        catch (IOException e) {}
                    }
                }
            }
            else if (jarFile.isDirectory()) {
                File base = jarFile;
                String prefix = pathEntry.getPathPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    base = new File(base, prefix);
                }
                
                File srcFile = new File(base, entNameFs);
                FileInputStream fis = null;
                try {
                    if (srcFile.canRead()) {
                        fis = new FileInputStream(srcFile);
                        Reader r = new InputStreamReader(fis, project.getProjectCharset());
                        ClassInfo info = JavadocParser.parse(r, resolver, null);
                        r.close();
                        if (info == null) {
                            return null;
                        }
                        return info;
                    }
                }
                catch (IOException ioe) {
                    if (fis != null) {
                        try {
                            fis.close();
                        }
                        catch (IOException e) {}
                    }
                }
            }
        }
        
        // Try and load the source from the class path. This allows source to be bundled in
        // with the classes.
        String targetName = target.replace('.', '/') + "." + SourceType.Java.toString().toLowerCase();
        URL srcUrl = project.getClassLoader().findResource(targetName);
        if (srcUrl != null) {
            try {
                Reader r = new InputStreamReader(srcUrl.openStream(), project.getProjectCharset());
                ClassInfo info = JavadocParser.parse(r, resolver, null);
                if (info != null) {
                    return info;
                }
            }
            catch (IOException ioe) {
                Debug.message("I/O exception while trying to retrieve javadoc for " + target);
            }
        }
        
        return null;
    }
    
    /**
     * Build a method signature from a MethodReflective.
     */
    private static String buildSig(ConstructorOrMethodReflective method)
    {
        String sig = "";
        if (method instanceof MethodReflective)
        {
            sig = ((MethodReflective)method).getReturnType().getErasedType().toString();
            sig = sig.replace('$', '.');
            sig += ' ' + ((MethodReflective)method).getName();
        }
        else
        {
            // Constructor name is just the name of the class:
            sig = method.getDeclaringType().getSimpleName();
            sig = sig.replace('$', '.');
            // Now need to remove qualifiers:
            int lastDot = sig.lastIndexOf('.');
            if (lastDot != -1)
                sig = sig.substring(lastDot + 1);
        }
        
        sig +=  '(';
        Iterator<JavaType> i = method.getParamTypes().iterator();
        while (i.hasNext()) {
            JavaType ptype = i.next();
            sig += ptype.getErasedType().toString().replace('$', '.');
            if (i.hasNext()) {
                sig += ", ";
            }
        }
        sig += ')';
        
        return sig;
    }
    
    /**
     * Find the javadoc for a given class (target) by searching the project source path.
     * In particular, this normally includes the JDK source. When source for the required
     * class is found, it is parsed to extract comments.
     */
    private Properties getCommentsFromSource(String target)
    {
        List<DocPathEntry> sourcePath = project.getSourcePath();
        String pkg = JavaNames.getPrefix(target);
        String entName = target.replace('.', '/') + ".java";
        String entNameFs = target.replace('.', File.separatorChar) + ".java";
        EntityResolver resolver = new PackageResolver(project.getEntityResolver(), pkg);
        
        for (DocPathEntry pathEntry : sourcePath) {
            File jarFile = pathEntry.getFile();
            if (jarFile.isFile()) {
                String fullEntryName = pathEntry.getPathPrefix();
                if (fullEntryName.length() != 0 && !fullEntryName.endsWith("/")) {
                    fullEntryName += "/";
                }
                fullEntryName += entName;
                Reader r = null;
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    ZipEntry zipEnt = zipFile.getEntry(fullEntryName);
                    if (zipEnt != null) {
                        InputStream zeis = zipFile.getInputStream(zipEnt);
                        r = new InputStreamReader(zeis, project.getProjectCharset());
                        ClassInfo info = JavadocParser.parse(r, resolver, null);
                        if (info == null) {
                            return null;
                        }
                        return info.getComments();
                    }
                }
                catch (IOException ioe) {}
                finally {
                    if (r != null) {
                        try {
                            r.close();
                        }
                        catch (IOException e) {}
                    }
                }
            }
            else if (jarFile.isDirectory()) {
                File base = jarFile;
                String prefix = pathEntry.getPathPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    base = new File(base, prefix);
                }
                
                File srcFile = new File(base, entNameFs);
                FileInputStream fis = null;
                try {
                    if (srcFile.canRead()) {
                        fis = new FileInputStream(srcFile);
                        Reader r = new InputStreamReader(fis, project.getProjectCharset());
                        ClassInfo info = JavadocParser.parse(r, resolver, null);
                        r.close();
                        if (info == null) {
                            return null;
                        }
                        return info.getComments();
                    }
                }
                catch (IOException ioe) {
                    if (fis != null) {
                        try {
                            fis.close();
                        }
                        catch (IOException e) {}
                    }
                }
            }
        }
        
        // Try and load the source from the class path. This allows source to be bundled in
        // with the classes.
        String targetName = target.replace('.', '/') + ".java";
        URL srcUrl = project.getClassLoader().findResource(targetName);
        if (srcUrl != null) {
            try {
                Reader r = new InputStreamReader(srcUrl.openStream(), project.getProjectCharset());
                ClassInfo info = JavadocParser.parse(r, resolver, null);
                if (info != null) {
                    return info.getComments();
                }
            }
            catch (IOException ioe) {
                Debug.message("I/O exception while trying to retrieve javadoc for " + target);
            }
        }
        
        return null;
    }
    
    @Override
    public String getJavadoc(String moduleName, String className)
    {
        ClassInfo ci = getClassInfoFromSource(moduleName, className);
        
        if (ci == null)
            return null;
        
        return ci.getCommentsAsList().stream()
                    .filter(sc -> ci.getName().equals(sc.target))
                    .map(sc -> sc.comment)
                    .filter(c -> c != null)
                    .findFirst().orElse(null);
   }
}
