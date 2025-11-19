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

import bluej.editor.flow.Document;
import bluej.extensions2.SourceType;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.lexer.LineColPos;
import bluej.parser.nodes.ReparseableDocument;
import bluej.pkgmgr.Package;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

import static bluej.parser.psi.Utils.onPlatformThread;


/**
 * Sealed interface representing a source input for parsing.
 * 
 * <p>Provides four explicit modes via record variants:
 * <ul>
 *   <li>{@link FileSource} - On-demand file reading without caching</li>
 *   <li>{@link ReaderSource} - Content consumed from Reader during construction</li>
 *   <li>{@link NamedStringSource} - Named string-based source (in-memory with explicit filename)</li>
 *   <li>{@link UnnamedStringSource} - Unnamed synthetic string-based source (test code, REPL)</li>
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
 *              SourceInput.NamedSource ns,
 *              SourceInput.UnnamedSource us -> {
 *             // All are in-memory, handle similarly
 *             // ... process memory content
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Factory Methods</b>: Use static factory methods for construction:
 * <ul>
 *   <li>{@link #fromFile(File, SourceType, Charset, Package)} - File with package</li>
 *   <li>{@link #fromString(String, SourceType, Package)} - Unnamed synthetic content</li>
 *   <li>{@link #fromNamedString(String, SourceType, Charset, String, String, Package)} - Named content</li>
 *   <li>{@link #fromReader(Reader, SourceType)} - Reader consumption</li>
 * </ul>
 */

public sealed interface SourceInput
    permits SourceInput.FileSource, SourceInput.ReaderSource, SourceInput.NamedStringSource, SourceInput.UnnamedStringSource, SourceInput.DocumentSource {

    record Range(Optional<LineColPos> start, Optional<LineColPos> end) {

    }

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

    default @NotNull Optional<Range> range() {
        return Optional.empty();
    }

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
     * Factory for unnamed synthetic content with UTF-8.
     * Use for test code, REPL input, or temporary snippets.
     */
    static UnnamedStringSource fromString(@NotNull String content, @NotNull SourceType sourceType,
                                          @NotNull Package pkg) {
        return new UnnamedStringSource(content, sourceType, StandardCharsets.UTF_8, pkg, null);
    }
    
    /**
     * Factory for unnamed synthetic content without package.
     */
    static UnnamedStringSource fromString(@NotNull String content, @NotNull SourceType sourceType) {
        return new UnnamedStringSource(content, sourceType, StandardCharsets.UTF_8, null, null);
    }
    
    /**
     * Factory for named string content with explicit filename and path.
     * Use when the content corresponds to a real file.
     * 
     * @param filename The actual filename (e.g., "MyClass.java")
     * @param virtualPath Full virtual path (must contain filename)
     */
    static NamedStringSource fromNamedString(@NotNull String content, @NotNull SourceType sourceType,
                                             @NotNull Charset charset, @NotNull String filename,
                                             @NotNull String virtualPath, @Nullable Package pkg) {
        return new NamedStringSource(content, sourceType, charset, filename, virtualPath, pkg, null);
    }
    
    /**
     * Factory for in-memory content sourced from Reader. The reader is fully consumed into memory.
     * The reader is not closed by this method.
     */
    static ReaderSource fromReader(@NotNull Reader reader, @NotNull SourceType sourceType) {
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
//        char[] buf = new char[8192];
//        StringBuilder sb = new StringBuilder();
//        int n;
//
//        try {
//            while ((n = reader.read(buf)) != -1) {
//                sb.append(buf, 0, n);
//            }
//        }
//        catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }

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
            // reader.mark(Integer.MAX_VALUE);
            // TODO: dumb workaround for tests, should be fixed properly
            reader.mark(65535);

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

    static DocumentSource fromDocument(@NotNull ReparseableDocument document) {
        return new DocumentSource(
            document,
            document.getSourceType(),
            document.getCharset(),
            document.getVirtualPath(),
            null,
            null,
            Optional.empty()
        );
    }
    
    /**
     * File-based source input.
     * Reads from file on-demand without caching content.Integer.MAX_VALUE
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

        public String content() {
            try {
                return Files.readString(file().toPath(), charset());
            }
            catch (IOException e) {
//                throw new UncheckedIOException(e);
            }

            return null;
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
     * Reader-based source input (consumed into memory).
     * Content is read once during construction and cached.
     */
    record DocumentSource(
            @NotNull ReparseableDocument document,
            @NotNull SourceType sourceType,
            @NotNull Charset charset,
            @NotNull String virtualPath,
            @Nullable Package pkg,
            @Nullable EntityResolver directResolver,
            @NotNull Optional<Range> range
    ) implements SourceInput {

        public DocumentSource {
            Objects.requireNonNull(document, "document must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
            Objects.requireNonNull(virtualPath, "virtualPath must not be null");

            if (range == null) {
                range = Optional.empty();
            }
        }

        public DocumentSource withRange(@NotNull Optional<Range> range) {
            return new DocumentSource(document, sourceType, charset, virtualPath, pkg, directResolver, range);
        }

        public String content() {
            StringBuilder sb = new StringBuilder();

            try (Reader reader = createReader()) {
                char[] buffer = new char[4096];
                int n = -1;

                while ((n = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, n);
                }

                return sb.toString();
            }
            catch (IOException e) {
                //
            }

            return null;
        }

        @Override
        public Reader createReader() {
            var parseStart = 0;
            var parseEnd = document.getLength();

            if (range.isPresent()) {
                var range = this.range.get();

                if (range.start().isPresent()) {
                    parseStart = range.start().get().position();
                }

                if (range.end().isPresent()) {
                    parseEnd = range.end().get().position();
                }
            }

            return document.makeReader(parseStart, parseEnd);
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
            var that = (DocumentSource) obj;
            return Objects.equals(this.virtualPath, that.virtualPath) &&
                    Objects.equals(this.sourceType, that.sourceType) &&
                    Objects.equals(this.charset, that.charset) &&
                    Objects.equals(this.document, that.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(virtualPath, sourceType, charset, document);
        }

        @Override
        public String toString() {
            return "SourceInput.DocumentSource[" +
                    "path=" + virtualPath + ", " +
                    "sourceType=" + sourceType + ", " +
                    "charset=" + charset + ']';
        }
    }
    
    /**
     * Named string-based source input (in-memory with explicit filename).
     * Used when the source corresponds to a real file with a meaningful name.
     */
    record NamedStringSource(
        @NotNull String content,
        @NotNull SourceType sourceType,
        @NotNull Charset charset,
        @NotNull String filename,
        @NotNull String virtualPath,
        @Nullable Package pkg,
        @Nullable EntityResolver directResolver
    ) implements SourceInput {
        
        public NamedStringSource {
            Objects.requireNonNull(content, "content must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
            Objects.requireNonNull(filename, "filename must not be null");
            Objects.requireNonNull(virtualPath, "virtualPath must not be null");
            
            // Extract basename from filename for validation - handles both full paths and basenames
            String fileBasename = filename.contains("/") || filename.contains("\\")
                ? filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1)
                : filename;
            
            // Validate basename appears in virtualPath
            if (!virtualPath.contains(fileBasename)) {
                throw new IllegalArgumentException(
                    "basename '" + fileBasename + "' (from filename '" + filename +
                    "') must appear in virtualPath '" + virtualPath + "'"
                );
            }
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
            return filename;
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
            var that = (NamedStringSource) obj;
            return Objects.equals(this.virtualPath, that.virtualPath) &&
                   Objects.equals(this.filename, that.filename) &&
                   Objects.equals(this.sourceType, that.sourceType) &&
                   Objects.equals(this.charset, that.charset) &&
                   Objects.equals(this.content, that.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(virtualPath, filename, sourceType, charset, content);
        }
        
        @Override
        public String toString() {
            return "SourceInput.NamedSource[" +
                   "filename=" + filename + ", " +
                   "path=" + virtualPath + ", " +
                   "sourceType=" + sourceType + ", " +
                   "charset=" + charset + ']';
        }
    }
    
    /**
     * Unnamed string-based source input (synthetic in-memory content).
     * Used for test code, REPL input, or temporary code snippets that don't correspond to files.
     */
    record UnnamedStringSource(
        @NotNull String content,
        @NotNull SourceType sourceType,
        @NotNull Charset charset,
        @Nullable Package pkg,
        @Nullable EntityResolver directResolver
    ) implements SourceInput {

        public UnnamedStringSource {
            Objects.requireNonNull(content, "content must not be null");
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
        }

        @Override
        public Reader createReader() {
            return new BufferedReader(new StringReader(content));
        }
        
        @Override
        public @NotNull String path() {
            // Generate synthetic path to prevent accidental reliance
            String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
            return "/<synthetic>/" + System.identityHashCode(this) + ext;
        }
        
        @Override
        public @NotNull String filename() {
            // Generate synthetic filename
            String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
            return "<synthetic-" + System.identityHashCode(this) + ">" + ext;
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
            var that = (UnnamedStringSource) obj;
            return Objects.equals(this.sourceType, that.sourceType) &&
                   Objects.equals(this.charset, that.charset) &&
                   Objects.equals(this.content, that.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(sourceType, charset, content);
        }
        
        @Override
        public String toString() {
            return "SourceInput.UnnamedSource[" +
                   "sourceType=" + sourceType + ", " +
                   "charset=" + charset + ", " +
                   "contentLength=" + content.length() + ']';
        }
    }
}