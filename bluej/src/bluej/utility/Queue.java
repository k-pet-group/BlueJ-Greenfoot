package bluej.utility;

/**
 ** General purpose Queue class
 **
 ** $Id: Queue.java 93 1999-05-28 00:54:37Z mik $
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

