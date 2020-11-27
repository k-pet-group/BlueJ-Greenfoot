/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2019,2020  Michael Kolling and John Rosenberg

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
package bluej.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import bluej.Config;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import javafx.application.Platform;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import bluej.Boot;
import bluej.parser.ImportedTypeCompletion;
import bluej.pkgmgr.JavadocResolver;
import bluej.pkgmgr.Project;
import bluej.parser.AssistContentThreadSafe;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class which manages scanning the classpath for available imports.
 */
public class ImportScanner
{
    // A lock item :
    private final Object monitor = new Object();
    // Root package with "" as ident.
    private CompletableFuture<RootPackageInfo> root;
    // The Project which we are scanning for:
    private final Project project;

    public ImportScanner(Project project)
    {
        this.project = project;
    }

    /**
     * For each package that we scan, we hold one PackgeInfo with details on the items
     * in that package, and links to any subpackages.  Thus the root of this tree
     * is a single PackageInfo representing the unnamed package (held in this.root).
     */
    private class PackageInfo
    {
        // Value can be null if details not loaded yet
        public final HashMap<String, AssistContentThreadSafe> types = new HashMap<>();
        public final HashMap<String, PackageInfo> subPackages = new HashMap<>();
    
        // Records a class with the given name (scoped relative to this package).
        // So first we call addClass({"java","lang"},"String") on the root package, then
        // addClass({"lang"}, "String"} on the java package, then
        // addClass({}, "String)" on the java.lang package.
        protected void addClass(Iterator<String> packageIdents, String name)
        {
            // If it's a sub-package, create it if necessary, then recurse:
            if (packageIdents.hasNext())
            {
                String ident = packageIdents.next();
                PackageInfo subPkg = subPackages.get(ident);
                if (subPkg == null)
                {
                    subPkg = new PackageInfo();
                    subPackages.put(ident, subPkg);
                }
                subPkg.addClass(packageIdents, name);
            }
            else
            {
                // Lives in this package:
                types.put(name, null);
            }
        }

        /**
         * Gets the type for the given name from this package, either using cached copy
         * or by calculating it on demand.
         * 
         * @param prefix The package name, ending in ".", e.g. "java.lang."
         * @param name The unqualified type name, e.g. "String".
         */
        @OnThread(Tag.Worker)
        private AssistContentThreadSafe getType(String prefix, String name, JavadocResolver javadocResolver)
        {
            return types.computeIfAbsent(name, s -> {
                // To safely get an AssistContentThreadSafe, we must create one from the FXPlatform thread.
                // So we need to hop across to the FXPlatform thread.  Because we are an arbitrary background
                // worker thread, it is safe to use wait afterwards; without risk of deadlock:
                try
                {
                    CompletableFuture<AssistContentThreadSafe> f = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        Class<?> c = project.loadClass(prefix + s);

                        // This happens reasonably often while the user is typing in an import in Stride,
                        // so it's not necessarily a bug:
                        if (c == null)
                        {
                            f.complete(null);
                        }
                        else
                        {
                            f.complete(new AssistContentThreadSafe(new ImportedTypeCompletion(c, javadocResolver)));
                        }
                    });
                    return f.get();
                }
                catch (Exception e)
                {
                    Debug.reportError(e);
                    return null;
                }
            });
        }

        /**
         * Gets types arising from a given import directive in the source code.
         * 
         * @param prefix The prefix of this package, ending in ".".  E.g. for the java
         *               package, we would be passed "java."
         * @param idents The next in the sequence of identifiers.  E.g. if we are the java package
         *               we might be passed {"lang", "String"}.  The final item may be an asterisk,
         *               e.g. {"lang", "*"}, in which case we return all types.  Otherwise we will
         *               return an empty list (if the type is not found), or a singleton list.
         * @return The 
         */
        @OnThread(Tag.Worker)
        public List<AssistContentThreadSafe> getImportedTypes(String prefix, Iterator<String> idents, JavadocResolver javadocResolver)
        {
            if (!idents.hasNext())
                return Collections.emptyList();
            
            String s = idents.next();
            if (s.equals("*"))
            {
                // Return all types:

                // Take a copy in case it causes problems that getType modifies the collection
                Collection<String> typeNames = new ArrayList<>(types.keySet());
                return typeNames.stream().map(t -> getType(prefix, t, javadocResolver)).filter(ac -> ac != null).collect(Collectors.toList());
            }
            else if (idents.hasNext())
            {
                // Still more identifiers to follow.  Look for package:
                if (subPackages.containsKey(s))
                    return subPackages.get(s).getImportedTypes(prefix + s + ".", idents, javadocResolver);
                else
                    return Collections.emptyList();
            }
            else
            {
                // Final identifier, not an asterisk, look for class:
                AssistContentThreadSafe ac = getType(prefix, s, javadocResolver);
                if (ac != null)
                    return Collections.singletonList(ac);
                else
                    return Collections.emptyList();
            }
        }

        public void addTypes(PackageInfo from)
        {
            types.putAll(from.types);
            from.subPackages.forEach((name, pkg) -> {
                subPackages.putIfAbsent(name, new PackageInfo());
                subPackages.get(name).addTypes(pkg);
            });
        }
    }
    
    // PackageInfo, but for the root type.
    private class RootPackageInfo extends PackageInfo
    {
        // Adds fully qualified class name to type list.
        public void addClass(String name)
        {
            String[] splitParts = name.split("\\.", -1);
            addClass(Arrays.asList(Arrays.copyOf(splitParts, splitParts.length - 1)).iterator(), splitParts[splitParts.length - 1]);
        }
    }
    
    @OnThread(Tag.Any)
    private CompletableFuture<? extends PackageInfo> getRoot()
    {
        synchronized (monitor)
        {
            // Already started calculating:
            if (root != null)
            {
                return root;
            }
            else
            {
                // Start calculating:
                root = new CompletableFuture<>();
                // We don't use runBackground because we don't want to end up
                // behind other callers of getRoot in the queue (this can
                // cause a deadlock because there are no background threads
                // available, as they are all blocked waiting for this
                // future to complete):
                new Thread() { public void run()
                {
                    RootPackageInfo rootPkg = findAllTypes();
                    try
                    {
                        loadCachedImports(rootPkg);
                    }
                    finally
                    {
                        root.complete(rootPkg);
                    }
                }}.start();
                return root;
            }
        }
    }

    /**
     * Given an import source (e.g. "java.lang.String", "java.util.*"), finds all the
     * types that will be imported.
     * 
     * If the one-time on-load import scanning has not finished yet, this method will
     * wait until it has.  Hence you should call it from a worker thread, not from a 
     * GUI thread where it could block the GUI for a long time.
     */
    @OnThread(Tag.Worker)
    public List<AssistContentThreadSafe> getImportedTypes(String importSrc)
    {
        try
        {
            return getRoot().get().getImportedTypes("", Arrays.asList(importSrc.split("\\.", -1)).iterator(), project.getJavadocResolver());
        }
        catch (InterruptedException | ExecutionException e)
        {
            Debug.reportError("Exception in getImportedTypes", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets a list of ClassGraph items which can be used to find available classes.
     * 
     * Because of the way ClassGraph works, one item is not enough for all classes;
     * we use one for system classes and one for user classes.
     */
    @OnThread(Tag.Worker)
    private List<ClassGraph> getClassloaderConfig()
    {
        // When you override the class loaders in ClassGraph's config, it no longer
        // loads the JDK classes.  So we have one ClassGraph for user code libraries
        // (e.g. JUnit, other configured BlueJ libraries):
        ArrayList<ClassLoader> cl = new ArrayList<>();
        
        try
        {
            CompletableFuture<ClassLoader> projectClassLoader = new CompletableFuture<>();
            // Safe to wait for platform thread because we are a worker thread:
            Platform.runLater(() -> {
                projectClassLoader.complete(project.getClassLoader());
            });
            cl.add(projectClassLoader.get());
        }
        catch (InterruptedException | ExecutionException e)
        {
            Debug.reportError(e);
        }
        cl.add(new URLClassLoader(Boot.getInstance().getRuntimeUserClassPath()));

        // We hide bluej.* classes as users shouldn't be accessing them:
        ClassGraph userClassGraph = new ClassGraph()
                .overrideClassLoaders(cl.toArray(new ClassLoader[0]))
                .rejectPackages("bluej.*");
        
        // We have a separate class graph for system libraries (java.*, javafx.*), from which
        // we only take public packages, thus avoiding all the com.sun classes and so on:
        // This has to be separate because enableSystemPackages() doesn't work alongside 
        // overrideClassLoaders():
        ClassGraph systemClassGraph = new ClassGraph()
            .enableSystemJarsAndModules()
            .acceptPackages("java.*", "javax.*", "javafx.*");

        return List.of(
            userClassGraph.enableClassInfo(),
            systemClassGraph.enableClassInfo()
        );
    }

    /**
     * Gets a package-tree structure which includes all packages and class-names
     * on the current class-path (by scanning all JARs and class-files on the path).
     *
     * @return A package-tree structure with all class names present, but not any further
     * details about the classes.
     */
    @OnThread(Tag.Worker)
    private RootPackageInfo findAllTypes()
    {
        List<ClassGraph> classGraphs = getClassloaderConfig();
        RootPackageInfo r = new RootPackageInfo();
        
        if (classGraphs != null)
        {
            // Special case -- ClassGraph library (deliberately) doesn't return Object in its list
            // so we must add it ourselves to avoid problems like "Unknown type: Object" messages.
            r.addClass("java.lang.Object");
            final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            for (ClassGraph classGraph : classGraphs)
            {
                try (ScanResult result = classGraph.scan(threads))
                {
                    for (ClassInfo c : result.getAllClasses())
                    {
                        r.addClass(c.getName());
                    }
                }
                catch (Throwable t)
                {
                    Debug.reportError(t);
                }
            }
        }
        return r;
    }

    /**
     * Starts scanning for available importable types from the classpath.
     * Will operate in a background thread.
     */
    public void startScanning()
    {
        // This will make sure the future has started:
        getRoot();
    }

    /**
     * Saves all java.** type information to a cache
     */
    public void saveCachedImports()
    {
        if (getRoot().isDone())
        {
            Element cache = new Element("packages");
            cache.addAttribute(new Attribute("javaHome", getJavaHome()));
            cache.addAttribute(new Attribute("version", getVersion()));
            try
            {
                PackageInfo javaPkg = getRoot().get().subPackages.get("java");
                if (javaPkg != null)
                {
                    cache.appendChild(toXML(javaPkg, "java"));
                    FileOutputStream os = new FileOutputStream(getImportCachePath());
                    Utility.serialiseCodeTo(cache, os);
                    os.close();
                }
            }
            catch (InterruptedException | ExecutionException | IOException e)
            {
                Debug.reportError(e);
            }

        }
    }

    /** Version of the currently running software */
    private static String getVersion()
    {
        return Config.isGreenfoot() ? Boot.GREENFOOT_VERSION : Boot.BLUEJ_VERSION;
    }

    /** Java home directory */
    private static String getJavaHome()
    {
        return Boot.getInstance().getJavaHome().getAbsolutePath();
    }

    /** Import cache path to save to/load from */
    private static File getImportCachePath()
    {
        return new File(Config.getUserConfigDir(), "import-cache.xml");
    }

    /**
     * Loads cached (java.**) imports into the given root package, if possible.
     */
    public void loadCachedImports(PackageInfo rootPkg)
    {
        try {
            Document xml = new Builder().build(getImportCachePath());
            Element packagesEl = xml.getRootElement();
            if (!packagesEl.getLocalName().equals("packages"))
                return;
            // If they've changed JDK or BlueJ/Greenfoot version, ignore the cache
            // (and thus generate fresh data later on):
            if (!getJavaHome().equals(packagesEl.getAttributeValue("javaHome")) || !getVersion().equals(packagesEl.getAttributeValue("version")))
                return;
            for (int i = 0; i < packagesEl.getChildElements().size(); i++)
            {
                fromXML(packagesEl.getChildElements().get(i), rootPkg);
            }
        }
        catch (ParsingException | IOException e) {
            Debug.message(e.getClass().getName() + " while reading import cache: " + e.getMessage());
        }
    }

    /**
     * Loads the given XML package item and puts it into the given parent package.
     */
    private void fromXML(Element pkgEl, PackageInfo addToParent)
    {
        String name = pkgEl.getAttributeValue("name");
        if (name == null)
            return;
        PackageInfo loadPkg = new PackageInfo();

        for (int i = 0; i < pkgEl.getChildElements().size(); i++)
        {
            Element el = pkgEl.getChildElements().get(i);
            if (el.getLocalName().equals("package"))
            {
                fromXML(el, loadPkg);
            }
            else
            {
                AssistContentThreadSafe acts = new AssistContentThreadSafe(el);
                String nameWithoutPackage = (acts.getDeclaringClass() == null ? "" : acts.getDeclaringClass() + "$") + acts.getName();
                loadPkg.types.put(nameWithoutPackage, acts);
            }
        }

        // Only store if successful:
        addToParent.subPackages.putIfAbsent(name, new PackageInfo());
        addToParent.subPackages.get(name).addTypes(loadPkg);
    }

    /**
     * Save the given PackageInfo item (with package name) to XML
     */
    private static Element toXML(PackageInfo pkg, String name)
    {
        Element el = new Element("package");
        el.addAttribute(new Attribute("name", name));
        pkg.types.values().forEach(acts -> {if (acts != null) el.appendChild(acts.toXML());});
        pkg.subPackages.forEach((subName, subPkg) -> el.appendChild(toXML(subPkg, subName)));
        return el;
    }
}
