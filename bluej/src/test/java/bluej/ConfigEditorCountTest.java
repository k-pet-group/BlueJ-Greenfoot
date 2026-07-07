/*
 This file is part of the BlueJ program. 
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the per-language editor-open counters used by the launch-time
 * usage statistics ping (see Main.updateStats), in particular the Kotlin
 * counter (session.numeditors.kotlin).
 */
public class ConfigEditorCountTest
{
    private static File tempUserHome;

    @BeforeClass
    public static void initConfig() throws Exception
    {
        // If another test initialised Config already, we cannot redirect the
        // user properties to a temp dir; skip rather than risk writing to the
        // developer's real bluej.properties.
        Assume.assumeFalse("Config already initialised by another test", Config.isInitialised());

        tempUserHome = Files.createTempDirectory("bluej-config-test").toFile();

        Properties props = new Properties();
        props.put("bluej.debug", "true");
        props.put("bluej.userHome", tempUserHome.getAbsolutePath());
        Config.initialise(Boot.getBluejLibDir(), props, false);

        // Double-check the redirect took effect before any test writes properties.
        Assume.assumeTrue("User config dir not redirected to temp dir",
            Config.getUserConfigDir().getAbsolutePath()
                .startsWith(tempUserHome.getAbsolutePath()));
    }

    @AfterClass
    public static void cleanup() throws Exception
    {
        // deleteOnExit() cannot remove non-empty directories, so delete recursively.
        if (tempUserHome != null) {
            try (var paths = Files.walk(tempUserHome.toPath())) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void testEditorCountLifecycle() throws Exception
    {
        // Fresh properties: no counter has ever been written, all read -1.
        assertEquals(-1, Config.getEditorCount(Config.SourceType.Java));
        assertEquals(-1, Config.getEditorCount(Config.SourceType.Stride));
        assertEquals(-1, Config.getEditorCount(Config.SourceType.Kotlin));

        // Recording increments only the matching counter.
        Config.recordEditorOpen(Config.SourceType.Kotlin);
        Config.recordEditorOpen(Config.SourceType.Kotlin);
        assertEquals(2, Config.getEditorCount(Config.SourceType.Kotlin));
        assertEquals(-1, Config.getEditorCount(Config.SourceType.Java));
        assertEquals(-1, Config.getEditorCount(Config.SourceType.Stride));

        Config.recordEditorOpen(Config.SourceType.Java);
        Config.recordEditorOpen(Config.SourceType.Stride);
        assertEquals(1, Config.getEditorCount(Config.SourceType.Java));
        assertEquals(1, Config.getEditorCount(Config.SourceType.Stride));
        assertEquals(2, Config.getEditorCount(Config.SourceType.Kotlin));

        // Reset zeroes all three (so the next launch reads 0, not -1).
        Config.resetEditorsCount();
        assertEquals(0, Config.getEditorCount(Config.SourceType.Java));
        assertEquals(0, Config.getEditorCount(Config.SourceType.Stride));
        assertEquals(0, Config.getEditorCount(Config.SourceType.Kotlin));

        // The Kotlin counter must be persisted to the user properties file
        // (recordEditorOpen/resetEditorsCount save it), so it survives to the
        // next session's stats ping.
        File propsFile = Config.getUserConfigFile("bluej.properties");
        assertTrue("bluej.properties should exist at " + propsFile, propsFile.exists());

        Properties onDisk = new Properties();
        try (var in = Files.newInputStream(propsFile.toPath())) {
            onDisk.load(in);
        }
        assertTrue("persisted properties should contain " + Config.EDITOR_COUNT_KOTLIN,
            onDisk.containsKey(Config.EDITOR_COUNT_KOTLIN));
    }
}
