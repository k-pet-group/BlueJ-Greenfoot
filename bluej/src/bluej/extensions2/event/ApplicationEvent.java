/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2019  Michael Kolling and John Rosenberg
 
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
 * This class encapsulates BlueJ application events.
 *
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class ApplicationEvent implements ExtensionEvent
{
  /**
   * Types of application events.
   */
  public static enum EventType
  {
    /**
     * Event generated when the BlueJ application is initialised and ready.
     */
    APP_READY_EVENT,

    /**
     * Event generated when connection to the data collection servers (e.g.&nbsp;Blackbox)
     * is broken, or the event generates an error code when sent.  This means that
     * recording will be suspended for the rest of the session.
     */
    DATA_SUBMISSION_FAILED_EVENT
  }

  private EventType eventType;

  /**
   * @param eventType one of the {@link EventType} values for this ApplicationEvent.
   */
  public ApplicationEvent(EventType eventType)
    {
    this.eventType = eventType;
    }

  /**
   * Gets the event type.
   *
   * @return The {@link EventType} value associated with this ApplicationEvent.
   */
  public EventType getEventType ()
    {
    return eventType;
    }


  /**
   * Returns a meaningful description of this event.
   */
  public String toString()
  {
    String msg = null;
    switch (eventType)
    {
      case APP_READY_EVENT:
        msg = "AppEvent: APP_READY_EVENT";
        break;
      case DATA_SUBMISSION_FAILED_EVENT:
        msg = "AppEvent: DATA_SUBMISSION_FAILED_EVENT";
        break;
      default:
        msg = "AppEvent: UNKNOWN eventType=" + eventType.toString();
    }
    return msg;
  }
}
