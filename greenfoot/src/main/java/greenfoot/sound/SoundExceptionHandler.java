/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2014,2018,2022  Poul Henriksen and Michael Kolling
 
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
package greenfoot.sound;

import greenfoot.core.WorldHandler;
import greenfoot.util.GreenfootUtil;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.UnsupportedAudioFileException;

import bluej.Config;

/**
 * This class should be forwarded some of the common sound exceptions. It keeps
 * track of which type of exceptions has already been shown to the user, and
 * makes sure not to repeatedly show the same exception which would otherwise be
 * likely to happen. For instance, it is enough to tell the user once, if a
 * soundcard can't be found, not every time a sound is attempted to be played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundExceptionHandler
{
    // Whether we have handled certain exceptions. We use these flags to ensure
    // we only print an error message once to avoid flooding the error output.
    private static volatile boolean lineUnavailableHandled;
    private static volatile boolean illegalArgumentHandled;
    private static volatile boolean securityHandled;
    private static boolean mp3LibHandled;

    public static void handleUnsupportedAudioFileException(UnsupportedAudioFileException e, String filename)
    {
        throw new IllegalArgumentException("Format of sound file not supported: " + filename, e);
    }

    public static void handleFileNotFoundException(FileNotFoundException e, String filename)
    {
        throw new IllegalArgumentException("Could not find sound file: " + filename, e);
    }

    public static void handleIOException(IOException e, String filename)
    {
        throw new IllegalArgumentException("Could not open sound file: " + filename, e);
    }

    public static void handleLineUnavailableException(Exception e)
    {
        // We only want to print this error message once.
        if (!lineUnavailableHandled) {
            lineUnavailableHandled = true;
            String errMsg = Config.getString("sound-line-unavailable");
            System.err.println(errMsg);
        }
    }

    public static void handleIllegalArgumentException(IllegalArgumentException e, String filename)
    {
        // We only want to print this error message once.
        if (!illegalArgumentHandled) {
            illegalArgumentHandled = true;
            System.err.println("Could not play sound file: " + filename);
            System.err.println("If you have a sound card installed, check your system settings.");
            e.printStackTrace();
        }
    }

    public static void handleSecurityException(SecurityException e, String filename)
    {
        // We only want to print this error message once.
        if (!securityHandled) {
            securityHandled = true;
            System.err.println("Could not play sound file due to security restrictions: " + filename);
            System.err.println("If you have a sound card installed, check your system settings.");
            e.printStackTrace();
        }
    }

    public static void handleInvalidMidiDataException(InvalidMidiDataException e, String filename)
    {
        throw new IllegalArgumentException("Invalid data in MIDI file: " + filename, e);
    }

    public static void handleMp3LibNotAvailable()
    {
        if (!mp3LibHandled) {
            mp3LibHandled = true;
            System.err.println("MP3 library not available." + " You will not be able to play any mp3 audio files."
                    + " This is most likely happening because you are using a non-standard Greenfoot installation."
                    + " To get the standard version, go to https://www.greenfoot.org");
        }
    }

}
