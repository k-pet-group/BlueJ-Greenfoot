/**
 ** Class PersonListNode - Node in a linked list of Persons
 ** 
 ** Author: John Rosenberg
 ** Date: 5th February, 1999
 **/
public class PersonListNode
{
    // instance variables - replace the example below with your own
    private PersonListNode next;
    private Person thePerson;

    /**
     ** Constructor for objects of class PersonListNode
     **/
    public PersonListNode(Person p)
    {
	// initialise instance variables
	next = null;
	thePerson = p;
    }

    /**
     ** Get the next node in a list
     **/
    public PersonListNode getNext()
    {
	return next;
    }

   /**
     ** Get the person for a given node
     **/
    public Person getPerson()
    {
	return thePerson;
    }

    /**
     ** Set the next node in the list
     **/
    public void setNext(PersonListNode n)
    {
	next = n;
    }
}
