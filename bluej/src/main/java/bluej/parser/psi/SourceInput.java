/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.parser.psi;

import bluej.extensions2.SourceType;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.pkgmgr.Package;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static bluej.parser.psi.Utils.onPlatformThread;


/**
 * Sealed interface representing a source input for parsing.
 * 
 * <p>Provides three explicit modes via record variants:
 * <ul>
 *   <li>{@link FileSource} - On-demand file reading without caching</li>
 *   <li>{@link ReaderSource} - Content consumed from Reader during construction</li>
 *   <li>{@link StringSource} - Direct string content</li>
 * </ul>
 * 
 * <p><b>Pattern Matching Example</b>:
 * <pre>{@code
 * void process(SourceInput input) {
 *     switch (input) {
 *         case SourceInput.FileSource fs -> {
 *             File f = fs.file();  // Type-safe file access
 *             // ... process file
 *         }
 *         case SourceInput.ReaderSource rs,
 *              SourceInput.StringSource ss -> {
 *             // Both are in-memory, handle similarly
 *             // ... process memory content
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Factory Methods</b>: Use static factory methods for construction:
 * <ul>
 *   <li>{@link #fromFile(File, SourceType, Charset, Package)} - File with package</li>
 *   <li>{@link #fromString(String, SourceType, Package)} - String with package</li>
 *   <li>{@link #fromReader(Reader, SourceType)} - Reader consumption</li>
 * </ul>
 */
public sealed interface SourceInput 
    permits SourceInput.FileSource, SourceInput.ReaderSource, SourceInput.StringSource {
    
    // Common interface methods
    
    /**
     * Returns the source type (Java, Kotlin, or Stride).
     */
    @NotNull SourceType sourceType();
    
    /**
     * Returns the character encoding.
     */
    @NotNull Charset charset();
    
    /**
     * Returns the virtual path for PSI and display purposes.
     */
    @NotNull String path();
    
    /**
     * Returns just the filename for display.
     */
    @NotNull String filename();
    
    /**
     * Returns the EntityResolver for this source.
     * @throws IllegalStateException if no Package or direct resolver is available
     */
    @NotNull EntityResolver getEntityResolver();
    
    /**
     * Returns the Package context if available.
     * @throws IllegalStateException if no Package is associated
     */
    @NotNull Package getPackage();
    
    /**
     * Checks if this source has an associated Package.
     */
    boolean hasPackage();
    
    /**
     * Creates a Reader for this source.
     * File-based: Creates fresh Reader each time.
     * Memory-based: Creates StringReader from cached content.
     * 
     * @return Buffered reader for source content
     * @throws IOException if file cannot be read
     */
    Reader createReader() throws IOException;
    
    // Static factory methods
    
    /**
     * Factory method from File with custom charset and package.
     *
     * @param file       Source file
     * @param sourceType Java or Kotlin
     * @param charset    Character encoding
     * @param pkg        Owning BlueJ package
     * @return FileSource instance
     */
    static FileSource fromFile(@NotNull File file, @NotNull SourceType sourceType, 
                               @NotNull Charset charset, @NotNull Package pkg) {
        return new FileSource(file, sourceType, charset, pkg, null);
    }
    
    /**
     * Factory method from File with custom charset and no BlueJ package.
     */
    static FileSource fromFile(@NotNull File file, @NotNull SourceType sourceType, 
                               @NotNull Charset charset) {
        return new FileSource(file, sourceType, charset, null, null);
    }
    
    /**
     * Factory for in-memory content with UTF-8, deriving virtual path from package and type.
     */
    static StringSource fromString(@NotNull String content, @NotNull SourceType sourceType, 
                                   @NotNull Package pkg) {
        String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
        String base = "InMemory" + ext;
        String path = pkg.getQualifiedName().replace('.', '/') + "/" + base;
        return new StringSource(content, sourceType, StandardCharsets.UTF_8, path, pkg, null);
    }
    
    /**
     * Full-control factory for in-memory content, specifying charset and virtual path.
     */
    static StringSource fromString(@NotNull String content, @NotNull SourceType sourceType, 
                                   @NotNull Charset charset, @Nullable Package pkg, 
                                   @NotNull String virtualPath) {
        return new StringSource(content, sourceType, charset, virtualPath, pkg, null);
    }
    
    /**
     * Factory for in-memory content sourced from Reader. The reader is fully consumed into memory.
     * The reader is not closed by this method.
     */
    static ReaderSource fromReader(@NotNull Reader reader, @NotNull SourceType sourceType) 
            throws IOException {
        String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
        String path = "/<memory>/InMemory" + ext;
        return fromReader(reader, sourceType, path);
    }
    
    /**
     * Factory for in-memory content from Reader with explicit virtual path.
     * The reader is fully consumed into memory. The reader is not closed by this method.
     */
    static ReaderSource fromReader(@NotNull Reader reader, @NotNull SourceType sourceType, 
                                   @NotNull String virtualPath) {
        char[] buf = new char[8192];
        StringBuilder sb = new StringBuilder();
        int n;

        try {
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new ReaderSource(sb.toString(), sourceType, StandardCharsets.UTF_8, 
                               virtualPath, null, null);
    }
    
    /**
     * Factory for in-memory content from Reader with explicit resolver.
     * The reader is fully consumed into memory. The reader is not closed by this method.
     */
    static ReaderSource fromReader(@NotNull Reader reader, @NotNull SourceType sourceType, 
                                   @NotNull EntityResolver resolver) {
        String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
        String path = "/<memory>/InMemory" + ext;

        return fromReader(reader, sourceType, resolver, path);
    }
    
    /**
     * Variant of fromReader with virtual path and explicit resolver.
     * The reader is fully consumed into memory. The reader is not closed by this method.
     */
    static ReaderSource fromReader(@NotNull Reader reader, @NotNull SourceType sourceType, 
                                   @NotNull EntityResolver resolver, @NotNull String virtualPath) {
        char[] buf = new char[8192];
        StringBuilder sb = new StringBuilder();
        int n;

        try {
            reader.mark(Integer.MAX_VALUE);

            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }

            reader.reset();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new ReaderSource(sb.toString(), sourceType, StandardCharsets.UTF_8, 
                               virtualPath, null, resolver);
    }
    
    /**
     * File-based source input.
     * Reads from file on-demand without caching content.
     */
    record FileSource(
        @NotNull File file,
        @NotNull SourceType sourceType,
        @NotNull Charset charset,
        @Nullable Package pkg,
        @Nullable EntityResolver directResolver
    ) implements SourceInput {
        
        // Compact constructor with validation
        public FileSource {
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
        }
        
        @Override
        public Reader createReader() throws IOException {
            return new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(file),
                    charset
                )
            );
        }
        
        @Override
        public @NotNull String path() {
            return file.getPath();
        }
        
        @Override
        public @NotNull String filename() {
            return file.getName();
        }
        
        @Override
        public @NotNull EntityResolver getEntityResolver() {
            if (directResolver != null) {
                return directResolver;
            }

            if (pkg == null) {
                throw new IllegalStateException("No Package or direct resolver available");
            }

            return onPlatformThread(() ->
                    new PackageResolver(
                            pkg.getProject().getEntityResolver(),
                            pkg.getQualifiedName()
                    )
            );
        }
        
        @Override
        public @NotNull Package getPackage() {
            if (pkg == null) {
                throw new IllegalStateException("No Package associated with this SourceInput");
            }
            return pkg;
        }
        
        @Override
        public boolean hasPackage() {
            return pkg != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (FileSource) obj;
            return Objects.equals(this.file.getPath(), that.file.getPath()) &&
                   Objects.equals(this.sourceType, that.sourceType) &&
                   Objects.equals(this.charset, that.charset);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(file.getPath(), sourceType, charset);
        }
        
        @Override
        public String toString() {
            return "SourceInput.FileSource[" +
                   "path=" + file.getPath() + ", " +
                   "sourceType=" + sourceType + ", " +
                   "charset=" + charset + ']';
        }
    }
    
    /**
     * Reader-based source input (consumed into memory).
     * Content is read once during construction and cached.
     */
    record ReaderSource(
        @NotNull String content,
        @NotNull SourceType sourceType,
        @NotNull Charset charset,
        @NotNull String virtualPath,
        @Nullable Package pkg,
        @Nullable EntityResolver directResolver
    ) implements SourceInput {
        
        public ReaderSource {
            Objects.requireNonNull(content, "content must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
            Objects.requireNonNull(virtualPath, "virtualPath must not be null");
        }
        
        @Override
        public Reader createReader() {
            return new BufferedReader(new StringReader(content));
        }
        
        @Override
        public @NotNull String path() {
            return virtualPath;
        }
        
        @Override
        public @NotNull String filename() {
            int slash = Math.max(virtualPath.lastIndexOf('/'), virtualPath.lastIndexOf('\\'));
            return slash >= 0 ? virtualPath.substring(slash + 1) : virtualPath;
        }
        
        @Override
        public @NotNull EntityResolver getEntityResolver() {
            if (directResolver != null) {
                return directResolver;
            }

            if (pkg == null) {
                throw new IllegalStateException("No Package or direct resolver available");
            }

            return onPlatformThread(() -> new PackageResolver(
                pkg.getProject().getEntityResolver(),
                pkg.getQualifiedName()
            ));
        }
        
        @Override
        public @NotNull Package getPackage() {
            if (pkg == null) {
                throw new IllegalStateException("No Package associated with this SourceInput");
            }
            return pkg;
        }
        
        @Override
        public boolean hasPackage() {
            return pkg != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ReaderSource) obj;
            return Objects.equals(this.virtualPath, that.virtualPath) &&
                   Objects.equals(this.sourceType, that.sourceType) &&
                   Objects.equals(this.charset, that.charset) &&
                   Objects.equals(this.content, that.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(virtualPath, sourceType, charset, content);
        }
        
        @Override
        public String toString() {
            return "SourceInput.ReaderSource[" +
                   "path=" + virtualPath + ", " +
                   "sourceType=" + sourceType + ", " +
                   "charset=" + charset + ']';
        }
    }
    
    /**
     * String-based source input (in-memory).
     * Content is provided directly as String.
     */
    record StringSource(
        @NotNull String content,
        @NotNull SourceType sourceType,
        @NotNull Charset charset,
        @NotNull String virtualPath,
        @Nullable Package pkg,
        @Nullable EntityResolver directResolver
    ) implements SourceInput {
        
        public StringSource {
            Objects.requireNonNull(content, "content must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
            Objects.requireNonNull(virtualPath, "virtualPath must not be null");
        }
        
        @Override
        public Reader createReader() {
            return new BufferedReader(new StringReader(content));
        }
        
        @Override
        public @NotNull String path() {
            return virtualPath;
        }
        
        @Override
        public @NotNull String filename() {
            int slash = Math.max(virtualPath.lastIndexOf('/'), virtualPath.lastIndexOf('\\'));
            return slash >= 0 ? virtualPath.substring(slash + 1) : virtualPath;
        }
        
        @Override
        public @NotNull EntityResolver getEntityResolver() {
            if (directResolver != null) {
                return directResolver;
            }
            if (pkg == null) {
                throw new IllegalStateException("No Package or direct resolver available");
            }
            return onPlatformThread(() -> new PackageResolver(
                pkg.getProject().getEntityResolver(),
                pkg.getQualifiedName()
            ));
        }
        
        @Override
        public @NotNull Package getPackage() {
            if (pkg == null) {
                throw new IllegalStateException("No Package associated with this SourceInput");
            }
            return pkg;
        }
        
        @Override
        public boolean hasPackage() {
            return pkg != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (StringSource) obj;
            return Objects.equals(this.virtualPath, that.virtualPath) &&
                   Objects.equals(this.sourceType, that.sourceType) &&
                   Objects.equals(this.charset, that.charset) &&
                   Objects.equals(this.content, that.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(virtualPath, sourceType, charset, content);
        }
        
        @Override
        public String toString() {
            return "SourceInput.StringSource[" +
                   "path=" + virtualPath + ", " +
                   "sourceType=" + sourceType + ", " +
                   "charset=" + charset + ']';
        }
    }
}