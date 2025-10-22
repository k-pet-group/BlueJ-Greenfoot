
/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016  Michael Kolling and John Rosenberg 

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
package bluej.parser.context;

import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Project;
import javafx.application.Platform;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for CompilationUnitContext from classpath resources.
 * This class encapsulates the logic for loading .ctxt files from
 * the classpath, with proper classloader handling and optional caching.
 */
public class CompilationUnitContextLoader implements AutoCloseable {
    
    /** The primary classloader to use for loading resources */
    private ClassLoader projectClassLoader;

    /** Package roots for project classes */
    private Path packageRoot;

    /** Cache for loaded contexts, null if caching is disabled */
    private final ConcurrentHashMap<String, CompilationUnitContext> cache;

    /** The provider for ClassLoader and project directory */
    private final ClassLoaderProvider provider;


    /**
     * Production constructor - maintains backward compatibility.
     *
     * @param project The Project to load contexts for
     */
    public CompilationUnitContextLoader(Project project) {
        this((ClassLoaderProvider) project);
    }


    /**
     * Package-private constructor for testing.
     * Allows injection of a test ClassLoaderProvider implementation.
     *
     * @param provider The ClassLoaderProvider to use for loading contexts
     */
    CompilationUnitContextLoader(ClassLoaderProvider provider) {
        this.provider = provider;
        this.cache = new ConcurrentHashMap<>();
    }


    /**
     * Returns the classloader used by the project this context loader handles.
     *
     * @return the ClassLoader
     */
    @NotNull synchronized protected ClassLoader getClassLoader() {
        if (this.projectClassLoader != null) { return this.projectClassLoader; }

        return (this.projectClassLoader = this.provider.getClassLoader());
    }


    /**
     * Returns the root project path for the project this context loader handles.
     *
     * @return the Path for package root
     */
    @NotNull synchronized protected Path getPackageRoot() {
        if (this.packageRoot != null) { return this.packageRoot; }

        return (this.packageRoot = this.provider.getProjectDir().toPath());
    }


    /**
     * Loads a CompilationUnitContext for a class with caching.
     * This method searches through all registered package roots to find the .ctxt file and handles the case where
     * the file doesn't exist by returning an empty context.
     *
     * @param qualifiedName The fully qualified class name
     *
     * @return A CompilationUnitContext
     */
    @NotNull public CompilationUnitContext contextForClass(@NotNull String qualifiedName) {
        CompilationUnitContext context = resolveFromResources(getClassLoader(), qualifiedName);

        if (context != null) { return context; }

        context = resolveFromPackageRoot(qualifiedName);

        return context != null
            ? context
            : new JavaContext(qualifiedName, List.of(), List.of());
    }


    /**
     * Loads context for a class using its Class object.
     * Convenience method that extracts the qualified name and classloader.
     * 
     * <p>Given Class's classloader is given preference, if available.
     * 
     * @param clazz The class to load context for
     *
     * @return The loaded context, or an empty context if a context file is not found
     */
    @NotNull public CompilationUnitContext contextForClass(@NotNull Class<?> clazz) {
        ClassLoader specificClassLoader = clazz.getClassLoader();
        String qualifiedName = clazz.getName();

        if (specificClassLoader != null) {
            CompilationUnitContext context = resolveFromResources(specificClassLoader, qualifiedName);

            if (context != null) { return context; }
        }

        // Fall back to project classloader
        return contextForClass(qualifiedName);
    }


    /**
     * Resolves the given class FQDN against resources present on the classpath
     * 
     * @param qualifiedName the FQDN of the class to resolve
     * @param loader the classloader to resolve
     * 
     * @return a compilation unit context for this class, or null if not present
     */
    private CompilationUnitContext resolveFromResources(ClassLoader loader, String qualifiedName) {
        Path resourcePath = constructContextFilePath(qualifiedName);

//        URL resource = loader.getResource(resourcePath.toString());

        // Try Kotlin metadata detection if .class file exists
        try {
            URL resource = loader.getResource(resourcePath.toString().replace(".ctxt", ".class"));

            KotlinClassMetadata metadata = KotlinMetadataReader.readMetadata(new File(resource.getFile()));

            if (metadata != null) {
                // Kotlin class detected - use KotlinContextFactory
                return KotlinContextFactory.fromMetadata(qualifiedName, metadata);
            }
        } catch (IOException e) {
            // Metadata read failed - fall through to Java .ctxt loading
            // This is expected for Java classes or corrupted Kotlin metadata
        } catch (Exception e) {
            // Unexpected error during Kotlin metadata processing
            // Log and fall through to Java .ctxt loading for resilience
            System.err.println("Warning: Kotlin metadata processing failed for " +
                    qualifiedName + ": " + e.getMessage());
        }

        URL resource = loader.getResource(resourcePath.toString());

        return (resource != null)
            ? loadContextFromFile(new File(resource.getFile()), qualifiedName)
            : null;
    }


    /**
     * Constructs the resource path for a .ctxt file from a qualified class name.
     *
     * @param qualifiedName The fully qualified class name (e.g., "java.lang.String")
     *
     * @return The resource path (e.g., "java/lang/String.ctxt")
     */
    @NotNull protected Path constructContextFilePath(@NotNull String qualifiedName) {
        String[] elements = qualifiedName.split("\\.");

        elements[elements.length - 1] += ".ctxt";

        return Paths.get("", elements);
    }


    /**
     * Resolves the given class FQDN against BlueJ project roots with automatic language detection.
     *
     * <p>This method implements the language detection strategy:
     * <ol>
     *   <li>Check for .class file existence</li>
     *   <li>If found, attempt to read Kotlin metadata from bytecode</li>
     *   <li>If Kotlin metadata present, create KotlinContext via KotlinContextFactory</li>
     *   <li>If no Kotlin metadata, fall back to Java .ctxt file loading</li>
     * </ol>
     *
     * @param qualifiedName the FQDN of the class to resolve
     *
     * @return a compilation unit context for this class, or null if not present
     */
    private CompilationUnitContext resolveFromPackageRoot(@NotNull String qualifiedName) {
        // Construct paths for .class and .ctxt files
        String[] elements = qualifiedName.split("\\.");
        elements[elements.length - 1] += ".class";
        Path classFilePath = getPackageRoot().resolve(Paths.get("", elements));

        // Try Kotlin metadata detection if .class file exists
        if (Files.exists(classFilePath)) {
            try {
                KotlinClassMetadata metadata = KotlinMetadataReader.readMetadata(classFilePath);
                
                if (metadata != null) {
                    // Kotlin class detected - use KotlinContextFactory
                    return KotlinContextFactory.fromMetadata(qualifiedName, metadata);
                }
            } catch (IOException e) {
                // Metadata read failed - fall through to Java .ctxt loading
                // This is expected for Java classes or corrupted Kotlin metadata
            } catch (Exception e) {
                // Unexpected error during Kotlin metadata processing
                // Log and fall through to Java .ctxt loading for resilience
                System.err.println("Warning: Kotlin metadata processing failed for " +
                    qualifiedName + ": " + e.getMessage());
            }
        }

        // No Kotlin metadata or .class file not found - try Java .ctxt file
        Path ctxtPath = constructContextFilePath(qualifiedName);
        Path ctxtFile = getPackageRoot().resolve(ctxtPath);

        return Files.exists(ctxtFile)
            ? loadContextFromFile(ctxtFile.toFile(), qualifiedName)
            : null;
    }


    /**
     * Loads a compilation unit context from a file
     * 
     * @param file the file to load the compilation unit context from
     * @param qualifiedName the FQDN of the class being loaded (TODO: should be contained in the file)
     * 
     * @return a compilation unit context loaded from this file
     */
    private CompilationUnitContext loadContextFromFile(@NotNull File file, @NotNull String qualifiedName) {
        return cache.computeIfAbsent(qualifiedName, key -> PropertyContextFormat.fromFile(qualifiedName, file));
    }


    /**
     * Creates a CompilationUnitContext from ClassInfo using the fully qualified class name.
     * The loader will construct the appropriate file path internally.
     *
     * @param qualifiedName The fully qualified class name
     * @param info The ClassInfo from the parser
     *
     * @return A new CompilationUnitContext with the data from ClassInfo
     */
    @NotNull public CompilationUnitContext updateContextFromClassInfo(@NotNull String qualifiedName, @NotNull ClassInfo info) {
        // Extract ClassInfo data first (handles FX thread requirements)
        ClassInfoData data = extractClassInfoData(info);
        Path contextFilePath = constructContextFilePath(qualifiedName);

        try {
            // Create JavaContext from Properties
            JavaContext context = PropertyContextFormat.fromProperties(data.className, data.comments);
            
            File contextFile = getPackageRoot().resolve(contextFilePath).toFile();
            PropertyContextFormat.writeToFile(context, contextFile);
            
            // Invalidate cache entry so subsequent lookups reload the updated data
            evictFromCache(qualifiedName);
            
            return context;
        } catch (IOException ioe) {
            throw new UncheckedIOException("Could not write context for " + contextFilePath, ioe);
        }
    }


    /**
     * Invalidates the cached context for a class.
     * This method searches through all registered package roots to find
     * and evict the corresponding cache entry.
     *
     * @param qualifiedName The fully qualified class name
     * @return true if an entry was removed from cache, false otherwise
     */
    public boolean evictFromCache(@NotNull String qualifiedName) {
        return cache.remove(qualifiedName) != null;
    }


    /**
     * Clears the cache if caching is enabled.
     * No-op if caching is disabled.
     *
     * <p>This method is thread-safe and can be called while other threads
     * are accessing the cache.
     */
    public void clearCache() {
        cache.clear();
    }


    /**
     * Closes this loader and releases any cached resources.
     * This method clears the cache to free memory.
     *
     * <p>After closing, the loader can still be used but will need to
     * reload any previously cached contexts.
     *
     * <p>This method is idempotent and thread-safe.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void close() {
        clearCache();
    }


    /**
     * Helper method to safely extract ClassInfo data on the FX thread.
     * This method ensures that ClassInfo.getName() and ClassInfo.getComments()
     * are called on the JavaFX platform thread as required.
     *
     * @param info The ClassInfo to access
     * @return A ClassInfoData record containing the extracted data
     */
    static ClassInfoData extractClassInfoData(@NotNull ClassInfo info) {
        // Need to execute on FX thread
        CompletableFuture<ClassInfoData> future = new CompletableFuture<>();

        Runnable infoGetter = () -> {
            try {
                String className = info.getName();
                Properties comments = info.getComments();
                future.complete(new ClassInfoData(className, comments));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        try {
            if (Platform.isFxApplicationThread()) {
                infoGetter.run();
            }
            else {
                Platform.runLater(infoGetter);
            }

            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract ClassInfo data", e);
        }
    }


    /**
     * Record class to hold extracted ClassInfo data.
     * This allows ClassInfo data to be extracted on the FX thread
     * and used elsewhere without thread constraints.
     */
    public record ClassInfoData(
            @NotNull String className,
            @NotNull Properties comments
    ) {}
}