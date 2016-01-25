/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg
 
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
package bluej.extensions.event;

/**
 * This class encapsulates BlueJ application events.
 * 
 * @version $Id: ApplicationEvent.java 15356 2016-01-25 18:23:50Z nccb $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class ApplicationEvent implements ExtensionEvent 
  {
  /**
   * Event generated when the BlueJ application is initialised and ready.
   */
  public static final int APP_READY_EVENT=1;
  /**
   * Event generated when connection to the data collection servers (e.g. Blackbox)
   * is broken, or the event generates an error code when sent.  This means that
   * recording will be suspended for the rest of the session.
   */
  public static final int DATA_SUBMISSION_FAILED_EVENT=2;

  private int eventId;

  /**
   * Constructs an ApplicationEvent
   */
  public ApplicationEvent(int anEventId)
    {
    eventId = anEventId;
    }

  /**
   * Returns the event type, one of the values defined.
   */
  public int getEvent ()
    {
    return eventId;
    }


  /**
   * Returns a meaningful description of this event.
   */
  public String toString()
    {
    if ( eventId == APP_READY_EVENT ) return "AppEvent: APP_READY_EVENT";
    if ( eventId == DATA_SUBMISSION_FAILED_EVENT ) return "AppEvent: DATA_SUBMISSION_FAILED_EVENT";

    return "AppEvent: UNKNOWN eventId="+eventId;
    }
  }
