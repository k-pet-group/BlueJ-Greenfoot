/**
 ** Class PersonList - Maintains a list of Persons
 ** 
 ** Author: John Rosenberg
 ** Date: 5th February, 1999
 **/
public class PersonList
{
    // instance variables - replace the example below with your own
    private PersonListNode head, tail,current;

    /**
     ** Constructor for objects of class PersonList
     **/
    public PersonList()
    {
	// initialise instance variables
	head = null;
	tail = null;
    }

    /**
     ** Add a Person to the list
     **/
    public void addPerson (Person thePerson)
    {
	PersonListNode node = new PersonListNode(thePerson);
	if (head == null)
	   head = node;
	else
	   tail.setNext(node);
	tail = node;
    }

    /**
     ** Start an iteration of the list
     **/
    public void start ()
    {
	current = head;
    }

    /**
     ** Get next in iteration
     **/
    public Person getNext()
    {
	if (current == null)
	    return null;
	else {
	    Person p = current.getPerson();
	    current = current.getNext();
	    return p;
	}
    }
}
