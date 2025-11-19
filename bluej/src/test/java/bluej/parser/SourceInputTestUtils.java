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
package bluej.parser;

import bluej.extensions2.SourceType;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.psi.SourceInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Test utility methods for creating SourceInput instances in tests.
 * 
 * <p>These utilities simplify common test patterns and provide
 * convenient defaults for test scenarios.
 */
public class SourceInputTestUtils {
    
    /**
     * Creates a SourceInput from String content with specified resolver.
     * Uses UTF-8 charset and auto-generated virtual path.
     * 
     * @param content String source code
     * @param sourceType Java or Kotlin
     * @param resolver EntityResolver for symbol resolution
     * @return StringSource instance
     */
    public static SourceInput.NamedStringSource createFromString(@NotNull String content,
                                                                 @NotNull SourceType sourceType,
                                                                 @NotNull EntityResolver resolver) {
        String ext = sourceType == SourceType.Kotlin ? ".kt" : ".java";
        String fileName = "TestFile" + ext;
        String virtualPath = "/test/" + fileName;
        return new SourceInput.NamedStringSource(content, sourceType, StandardCharsets.UTF_8,
                                                 virtualPath, fileName, null, resolver);
    }
    
    /**
     * Creates a SourceInput from String content for Java source with resolver.
     * Convenience method that assumes Java source type.
     * 
     * @param content String source code
     * @param resolver EntityResolver for symbol resolution
     * @return StringSource instance
     */
    public static SourceInput.NamedStringSource createFromString(@NotNull String content,
                                                                 @NotNull EntityResolver resolver) {
        return createFromString(content, SourceType.Java, resolver);
    }
    
    /**
     * Creates a SourceInput from String content with default ClassLoaderResolver.
     * Uses the class loader of this utility class.
     * 
     * @param content String source code
     * @param sourceType Java or Kotlin
     * @return StringSource instance
     */
    public static SourceInput.NamedStringSource createFromString(@NotNull String content,
                                                                 @NotNull SourceType sourceType) {
        return createFromString(content, sourceType, 
                               new ClassLoaderResolver(SourceInputTestUtils.class.getClassLoader()));
    }
    
    /**
     * Creates a SourceInput from String content assuming Java with default resolver.
     * 
     * @param content String source code
     * @return StringSource instance
     */
    public static SourceInput.NamedStringSource createFromString(@NotNull String content) {
        return createFromString(content, SourceType.Java);
    }
    
    /**
     * Creates a SourceInput from File with default settings.
     * Uses UTF-8 charset and ClassLoaderResolver.
     * 
     * @param file Source file
     * @param sourceType Java or Kotlin
     * @return FileSource instance
     */
    public static SourceInput.FileSource createFromFile(@NotNull File file,
                                                        @NotNull SourceType sourceType) {
        return SourceInput.fromFile(file, sourceType, StandardCharsets.UTF_8);
    }
    
    /**
     * Creates a SourceInput from File assuming Java source.
     * Uses UTF-8 charset.
     * 
     * @param file Source file
     * @return FileSource instance
     */
    public static SourceInput.FileSource createFromFile(@NotNull File file) {
        return createFromFile(file, SourceType.Java);
    }
    
    /**
     * Creates a SourceInput from File with custom resolver.
     * Uses UTF-8 charset.
     *
     * @param file Source file
     * @param sourceType Java or Kotlin
     * @param resolver EntityResolver for symbol resolution
     * @return FileSource instance with direct resolver
     */
    public static SourceInput.FileSource createFromFile(@NotNull File file,
                                                        @NotNull SourceType sourceType,
                                                        @NotNull EntityResolver resolver) {
        return new SourceInput.FileSource(file, sourceType, StandardCharsets.UTF_8,
                                         null, resolver);
    }
    
    /**
     * Creates a SourceInput from resource file path with custom resolver.
     * For PackageResolver, uses factory method that consumes file into memory to create ReaderSource.
     * For other resolvers, uses FileSource with direct resolver.
     *
     * @param clazz Class to load resource from
     * @param resourcePath Resource path
     * @param sourceType Java or Kotlin
     * @param resolver EntityResolver for symbol resolution
     * @return SourceInput instance properly configured for dependency tracking
     */
    public static SourceInput createFromResource(@NotNull Class<?> clazz,
                                                 @NotNull String resourcePath,
                                                 @NotNull SourceType sourceType,
                                                 @NotNull EntityResolver resolver) {
        File file = bluej.utility.ResourceFileReader.getResourceAsFile(clazz, resourcePath);
        
        // If resolver is PackageResolver, use fromReader factory which creates ReaderSource
        // This works because ReaderSource can use directResolver for entity resolution
        if (resolver instanceof bluej.parser.entity.PackageResolver) {
            try {
//                Reader reader = new FileReader(file, StandardCharsets.UTF_8);
                Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                // Use fromReader factory with resolver and virtual path
                String virtualPath = file.getName();
                return SourceInput.fromReader(reader, sourceType, resolver, virtualPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read resource: " + resourcePath, e);
            }
        }
        
        // For other resolvers, use FileSource with resolver
        return new SourceInput.FileSource(file, sourceType, StandardCharsets.UTF_8, null, resolver);
    }
    
    /**
     * Creates a SourceInput from Reader consuming it into memory.
     * Uses UTF-8 charset and auto-generated virtual path.
     * 
     * @param reader Reader to consume
     * @param sourceType Java or Kotlin
     * @param resolver EntityResolver for symbol resolution
     * @return ReaderSource instance
     * @throws IOException if reader cannot be consumed
     */
    public static SourceInput.ReaderSource createFromReader(@NotNull Reader reader,
                                                            @NotNull SourceType sourceType,
                                                            @NotNull EntityResolver resolver) 
            throws IOException {
        return SourceInput.fromReader(reader, sourceType, resolver);
    }
    
    /**
     * Creates a SourceInput from Reader assuming Java source.
     * 
     * @param reader Reader to consume
     * @param resolver EntityResolver for symbol resolution
     * @return ReaderSource instance
     * @throws IOException if reader cannot be consumed
     */
    public static SourceInput.ReaderSource createFromReader(@NotNull Reader reader,
                                                            @NotNull EntityResolver resolver) 
            throws IOException {
        return createFromReader(reader, SourceType.Java, resolver);
    }
    
    /**
     * Legacy compatibility method: Creates SourceInput from StringReader.
     * This matches the old InfoParser.parse(Reader, EntityResolver, String) pattern.
     * 
     * @param reader Reader (typically StringReader)
     * @param resolver EntityResolver
     * @param targetPkg Target package (can be null)
     * @return SourceInput instance
     * @throws IOException if reader cannot be consumed
     */
    public static SourceInput createLegacyInput(@NotNull Reader reader,
                                                @NotNull EntityResolver resolver,
                                                @Nullable String targetPkg) 
            throws IOException {
        return SourceInput.fromReader(reader, SourceType.Java, resolver);
    }
    
    /**
     * Legacy compatibility method: Creates SourceInput from Reader with SourceType.
     * 
     * @param reader Reader (typically StringReader)
     * @param sourceType Java or Kotlin
     * @param resolver EntityResolver
     * @param targetPkg Target package (can be null)
     * @return SourceInput instance
     * @throws IOException if reader cannot be consumed
     */
    public static SourceInput createLegacyInput(@NotNull Reader reader,
                                                @NotNull SourceType sourceType,
                                                @NotNull EntityResolver resolver,
                                                @Nullable String targetPkg) 
            throws IOException {
        return SourceInput.fromReader(reader, sourceType, resolver);
    }
}