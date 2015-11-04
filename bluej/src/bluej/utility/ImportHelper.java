package bluej.utility;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Boot;
import bluej.parser.ImportedTypeCompletion;
import bluej.pkgmgr.JavadocResolver;
import bluej.stride.generic.AssistContentThreadSafe;

public class ImportHelper
{
    private static final Object monitor = new Object();
    private static PackageInfo root; // Root package with "" as ident
    private static Reflections reflections;
    
    private static class PackageInfo
    {
        // Value can be null if details not loaded yet
        public final HashMap<String, AssistContentThreadSafe> types = new HashMap<>();
        public final HashMap<String, PackageInfo> subPackages = new HashMap<>();
    
        public void addClass(String name)
        {
            String[] splitParts = name.split("\\.", -1);
            addClass(Arrays.asList(Arrays.copyOf(splitParts, splitParts.length - 1)).iterator(), splitParts[splitParts.length - 1]);
        }
        
        private void addClass(Iterator<String> packageIdents, String name)
        {
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
                types.put(name, null);
            }
        }
        
        private AssistContentThreadSafe getType(String prefix, String name, JavadocResolver javadocResolver)
        {
            return types.computeIfAbsent(name, s -> { 
                Class<?> c = reflections.typeNameToClass(prefix + s);
                CompletableFuture<AssistContentThreadSafe> f = new CompletableFuture<>();
                try
                {
                    SwingUtilities.invokeAndWait(() -> f.complete(new AssistContentThreadSafe(new ImportedTypeCompletion(c, javadocResolver))));
                    return f.get();
                }
                catch (Exception e)
                {
                    Debug.reportError(e);
                    return null;
                }
            });
        }

        public List<AssistContentThreadSafe> getImportedTypes(String prefix, Iterator<String> idents, JavadocResolver javadocResolver)
        {
            PackageInfo p = this;
            if (!idents.hasNext())
                return Collections.emptyList();
            
            String s = idents.next();
            if (s.equals("*"))
            {
                // Return all types:
                return types.keySet().stream().map(t -> getType(prefix, t, javadocResolver)).filter(ac -> ac != null).collect(Collectors.toList());
            }
            else if (idents.hasNext())
            {
                // Look for package:
                if (subPackages.containsKey(s))
                    return subPackages.get(s).getImportedTypes(prefix + s + ".", idents, javadocResolver);
                else
                    return Collections.emptyList();
            }
            else
            {
                // Look for class:
                AssistContentThreadSafe ac = getType(prefix, s, javadocResolver);
                if (ac != null)
                    return Collections.singletonList(ac);
                else
                    return Collections.emptyList();
            }
        }

        public Stream<String> getAvailableImports()
        {
            return Stream.concat(
                    types.keySet().stream(),
                    subPackages.entrySet().stream().flatMap(
                            e -> Stream.concat(
                                    Stream.of(e.getKey() + ".*"),
                                    e.getValue().getAvailableImports().map(s -> e.getKey() + "." + s)
                                    )
                    ));
        }
        
        
    }
    
    private static PackageInfo getRoot()
    {
        synchronized (monitor)
        {
            // Already calculated:
            if (root != null)
            {
                return root;
            }
            else
            {
                root = findAllTypes();
                return root;
            }
        }
    }
    
    @OnThread(Tag.Any)
    public static List<AssistContentThreadSafe> getImportedTypes(String importSrc, JavadocResolver javadocResolver)
    {
        return getRoot().getImportedTypes("", Arrays.asList(importSrc.split("\\.", -1)).iterator(), javadocResolver);
    }
    
    private static ConfigurationBuilder getClassloaderConfig()
    {
        List<ClassLoader> classLoadersList = new ArrayList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        
        Collection<URL> urls = new ArrayList<>();
        urls.addAll(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])));
        urls.addAll(Arrays.asList(Boot.getInstance().getRuntimeUserClassPath()));
        // By default, rt.jar doesn't appear on the classpath, but it contains all the core classes:
        try {
            urls.add(Boot.getJREJar("rt.jar"));
        }
        catch (MalformedURLException e) {
            Debug.reportError(e);
        }
        
        // Stop jnilib files being processed on Mac:
        urls.removeIf(u -> u.toExternalForm().endsWith("jnilib") || u.toExternalForm().endsWith("zip"));
        
        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));
        
        return new ConfigurationBuilder()
            .setScanners(new SubTypesScanner(false /* don't exclude Object.class */))
            .setUrls(urls)
            .addClassLoader(cl);
    }
                
    private static Reflections getReflections(List<String> importSrcs)
    {
        FilterBuilder filter = new FilterBuilder();
        
        for (String importSrc : importSrcs)
        {
            if (importSrc.endsWith(".*"))
            {
                // Chop off star but keep the dot, then escape dots:
                String importSrcRegex = importSrc.substring(0, importSrc.length() - 1).replace(".","\\.");
                filter = filter.include(importSrcRegex + ".*"); 
            }
            else
            {
                // Look for that exactly.  It seems we need .* because I think the library
                // uses the same filter to match files as classes, so an exact match will miss the class file:
                filter = filter.include(importSrc.replace(".", "\\.") + ".*");
            }
        }
        // Exclude $1, etc classes -- they cannot be used directly, and asking about them causes errors:
        filter = filter.exclude(".*\\$\\d.*");
        filter = filter.exclude("com\\.sun\\..*");
        
        try
        {
            return new Reflections(getClassloaderConfig()
                .filterInputsBy(filter)
                );
        }
        catch (Throwable e)
        {
            Debug.reportError(e);
            return null;
        }
    }
    
    public static Set<String> getAvailableImports(JavadocResolver javadocResolver)
    {
        return getRoot().getAvailableImports().collect(Collectors.toSet());
    }
    
    private static String getSafeCanonicalName(Class<?> c)
    {
        try
        {
            return c.getCanonicalName();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static PackageInfo findAllTypes()
    {
        reflections = getReflections(Collections.emptyList());
        
        if (reflections == null)
            return new PackageInfo();
        
        Set<String> classes;
        try
        {
            classes = reflections.getSubTypeNamesOf(Object.class);
        }
        catch (Throwable t)
        {
            Debug.reportError(t);
            classes = new HashSet<>();
        }
        
        // Also add Object itself, not found by default:
        classes.add(Object.class.getName());

        PackageInfo r = new PackageInfo();
        classes.forEach(c -> r.addClass(c));
        return r;
    }

    public static void startScanning()
    {
        Utility.getBackground().submit(ImportHelper::getRoot);
    }
}
