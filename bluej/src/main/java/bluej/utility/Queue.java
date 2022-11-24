/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

/**
 ** General purpose Queue class
 **
 ** $Id: Queue.java 6215 2009-03-30 13:28:25Z polle $
 **/

public class Queue
{
    private Elem head;
    private Elem tail;
	
    public Queue()
    {
	head = null;
	tail = null;
    }
	
    public synchronized void enqueue(Object data)
    {
	if(tail == null)
	    head = tail = new Elem(data);
	else
	    {
		tail.next = new Elem(data);
		tail = tail.next;
	    }
    }
	
    public synchronized Object dequeue()
    {
	Object ret = null;
		
	if(head != null)
	    {
		ret = head.data;
		head = head.next;
		if(head == null)
		    tail = null;
	    }
		
	return ret;
    }
	
    public synchronized boolean isEmpty()
    {
	return tail == null;
    }
	
    class Elem
    {
	Object data;
	Elem next;
	
	Elem(Object data)
	{
	    this.data = data;
	    this.next = null;
	}
    }
}

