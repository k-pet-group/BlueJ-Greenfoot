/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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

import java.util.concurrent.Executor;

/**
 * Describes a possible code completion.
 * 
 * @author Marion Zalk
 */
public abstract class AssistContent
{
    /** Get just the name of the method, to display in the box */
    public abstract String getDisplayMethodName();
    /** Get the parameters of the method (including brackets), to display in the box */
    public abstract String getDisplayMethodParams();
    
    /** Get the text to display in the code completion box for this completion */
    public abstract String getDisplayName();
    
    /** Get the completion text (to appear in front of the cursor/selection) */
    public abstract String getCompletionText();
    
    /** Get the completion text to appear selected */
    public abstract String getCompletionTextSel();
    
    /** Get the completion text (portion to appear behind the cursor/selection) */
    public abstract String getCompletionTextPost();

    /** Get the return type for this completion (as a string) */
    public abstract String getReturnType();

    /** Get the declaring class of this completion (as a string) */
    public abstract String getDeclaringClass();

    /** Return true if this completion has parameters */
    public abstract boolean hasParameters();
    
    /** Return true if the Javadoc is available, i.e. the getJavadoc call will return promptly */
    public abstract boolean javadocIsSet();
    
    /**
     * Get the javadoc comment for this completion. The comment has been stripped of the
     * delimiters (slash-star at the start and star-slash at the end) and intermediate
     * star characters.
     */
    public abstract String getJavadoc();
    
    /**
     * Callback interface for notification that javadoc is available.
     */
    public interface JavadocCallback
    {
        /**
         * The javadoc for the given method is now available
         * (call getJavadoc() to retrieve it).
         */
        void gotJavadoc(AssistContent content);
    }
    
    /**
     * Get the javadoc for this member, with an asynchronous callback.
     * (This method must be called from the event thread).
     * 
     * @param callback  Callback to be notified when the javadoc is available.
     *             (Notification will be on event thread). The callback will
     *             only be notified if the javadoc must be fetched asynchronously
     *             i.e. if this method returns false.
     * @param executor   The executor for any background tasks.
     * 
     * @return  true if the javadoc is already available, false otherwise
     *           (notification is pending).
     */
    public abstract boolean getJavadocAsync(JavadocCallback callback, Executor executor);
}
