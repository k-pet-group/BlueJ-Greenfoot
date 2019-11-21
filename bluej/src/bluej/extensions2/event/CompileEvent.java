/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.event;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;

/**
 * This class encapsulates compiler events.
 * It allows an extension writer to know when a compilation starts and
 * finishes, whether it succeeds or fails, and what warnings or errors are 
 * generated.
 * 
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
@OnThread(Tag.Any)
public class CompileEvent implements ExtensionEvent
{
    /**
     * Types of compilation events.
     */
    public static enum EventType
    {
        /**
         * Event generated when compilation begins.
         */
        COMPILE_START_EVENT,

        /**
         * Event generated when a compilation warning occurs.
         * A warning event is one that will not invalidate the compilation.
         */
        COMPILE_WARNING_EVENT,

        /**
         * Event generated when a compilation error occurs.
         * An error event is one that will invalidate the compilation
         */
        COMPILE_ERROR_EVENT,

        /**
         * Event generated when a compilation finishes successfully.
         */
        COMPILE_DONE_EVENT,

        /**
         * Event generated when a compilation finishes unsuccessfully.
         */
        COMPILE_FAILED_EVENT
    }

    private final boolean isUserGeneratedCompilation;
    private EventType    eventType;
    private File[] fileNames;   // An array of names this event belong to
    private int    errorLineNumber;
    private int    errorColumn;
    private int    endErrorLine;
    private int    endErrorColumn;
    private String errorMessage;

    /**
     * Constructor to use for user-generated compilations.
     *
     * @param eventType one of the {@link EventType} values for this CompileEvent.
     * @param aFileNames an array of {@link File} objects referring to the compiled files.
     */
    public CompileEvent(EventType eventType, File[] aFileNames)
    {
        this.eventType = eventType;
        fileNames = aFileNames;
        // Legacy constructor, from when we always used to keep classes:
        this.isUserGeneratedCompilation = true;
    }

    /**
     * @param eventType one of the {@link EventType} values for this CompileEvent.
     * @param isUserGeneratedCompilation  a boolean indicating whether the compilation was triggered by the user (<code>true</code>) or by BlueJ (<code>false</code>).
     * @param aFileNames an array of {@link File} objects referring the compiled files.
     */
    public CompileEvent(EventType eventType, boolean isUserGeneratedCompilation, File[] aFileNames)
    {
        this.eventType = eventType;
        this.isUserGeneratedCompilation = isUserGeneratedCompilation;
        fileNames = aFileNames;
    }

    /**
     * Returns the eventType associated with this CompileEvent
     *
     * @return One of the {@link EventType} values associated with this CompileEvent.
     */
    public EventType getEventType()
    {
        return eventType;
    }

    /**
     * Returns the files related to this event.
     * The array can be empty if no file are related to this event.
     *
     * @return The array of {@link File} objects referring to the compiled files related to this event.
     */
    public File[] getFiles()
    {
        return fileNames;
    }

    /**
     * Sets the line number where an error or warning occurred.
     *
     * @param aLineNumber a Line number to be associated with this CompileEvent.
     */
    public void setErrorLineNumber(int aLineNumber)
    {
        errorLineNumber = aLineNumber;
    }
    
    /**
     * Sets the error position.
     *
     * @param errorPosition an array of four integers for this CompileEvent, containing the error position information: beginning line [0] and column [1], ending line [2] and column [3].
     */
    public void setErrorPosition(int [] errorPosition)
    {
        errorLineNumber = errorPosition[0];
        errorColumn = errorPosition[1];
        endErrorLine = errorPosition[2];
        endErrorColumn = errorPosition[3];
    }

    /**
     * Returns the line number where the compilation error occurs.
     * Only valid in the case of an error or warning event.
     */
    public int getErrorLineNumber()
    {
        return errorLineNumber;
    }
    
    /**
     * Gets the error position.
     *
     * @return An array of four integers containing the error position information: beginning line [0] and column [1], ending line [2] and column [3].
     */
    public int[] getErrorPosition()
    {
        int [] r = new int[4];
        r[0] = errorLineNumber;
        r[1] = errorColumn;
        r[2] = endErrorLine;
        r[3] = endErrorColumn;
        return r;
    }

    /**
     * Sets the error message for an error or warning event.
     *
     * @param anErrorMessage an error message for this CompileEvent.
     */
    public void setErrorMessage(String anErrorMessage)
    {
        errorMessage = anErrorMessage;
    }

    /**
     * Returns the error message generated by the compiler.
     * Only valid in the case of an error or warning event.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * Returns an indicator for user or BlueJ generated compilation.
     * Two types of compilation can occur when using BlueJ.
     * One is BlueJ-generated, an internal automatic check for errors, which does a full compilation (and thus will trigger
     * these compile events), but discards the resulting class files.  The other is a user-generated proper
     * compilations which compile and keep the class files.  This method lets the extension distinguish
     * between the two.
     *
     * @return The boolean value indicating whether the compilation associated with this CompileEvent is user-generated (<code>true</code>) or BlueJ-generated (<code>false</code>).
     */
    public boolean isUserGeneratedCompilation()
    {
        return isUserGeneratedCompilation;
    }

    /**
     * Returns a meaningful description of this event.
     */
    @Override
    public String toString()
    {
        StringBuffer aRisul = new StringBuffer(500);

        aRisul.append("CompileEvent:");

        switch (eventType)
        {
            case COMPILE_START_EVENT:
                aRisul.append(" COMPILE_START_EVENT");
                break;
            case COMPILE_WARNING_EVENT:
                aRisul.append(" COMPILE_WARNING_EVENT");
                break;
            case COMPILE_ERROR_EVENT:
                aRisul.append(" COMPILE_ERROR_EVENT");
                break;
            case COMPILE_DONE_EVENT:
                aRisul.append(" COMPILE_DONE_EVENT");
                break;
            case COMPILE_FAILED_EVENT:
                aRisul.append(" COMPILE_FAILED_EVENT");
                break;
        }

        aRisul.append(" getFiles().length=");
        aRisul.append(fileNames.length);

        for(int i = 0; i < fileNames.length; i++) {
            aRisul.append(" getFiles()[" + i + "]=");
            aRisul.append(fileNames[i]);
        }

        if ( eventType == EventType.COMPILE_WARNING_EVENT || eventType == EventType.COMPILE_ERROR_EVENT )
        {
            aRisul.append(" errorLineNumber=" + errorLineNumber);
            aRisul.append(" errorMessage=" + errorMessage);
        }

        return aRisul.toString();
    }
}
