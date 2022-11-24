/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2019 Michael Kolling and John Rosenberg
 
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

/**
 * This interface allows an extension to listen for application events.
 *
 */
public interface ApplicationListener
{
    /**
     * This method will be called when the BlueJ application is initialised.
     * Warning: If the extension is loaded with a Project, it will not get this event since
     * BlueJ has already completed its initialisation when the project is loaded.
     * Note that this method is called from the JavaFX (GUI) thread and therefore the extension must
     * return as quickly as possible. 
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event the event of type {@link ApplicationEvent.EventType#APP_READY_EVENT} sent over.
     */
    public void blueJReady(ApplicationEvent event);

    /**
     * This method will be called when submission to the current data recording server
     * (Blackbox, or your local purpose-run recording server) has failed.
     *
     * @param event the event of type {@link ApplicationEvent.EventType#DATA_SUBMISSION_FAILED_EVENT} sent over.
     */
    default public void dataSubmissionFailed(ApplicationEvent event) { }
}
