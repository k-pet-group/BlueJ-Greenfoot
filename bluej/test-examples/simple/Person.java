package simple;

/**
 ** This class is part of a simple Database program, made to demonstrate
 ** JavaBlue by Michael Kolling.
 ** @author Morten Knudsen & Kent Hansen
 ** @version 1.0 July 1998
 **/
public class Person
{
	/** This person's first name **/
	public String firstName;
	
	/** This person's last name **/
	public String lastName;
	
	/** This person's year of birth **/
	public long yearOfBirth;

	/**
	 ** Constructor - create a new (empty) person
	 **/
	Person()
	{
		firstName = "";
		lastName = "";
		yearOfBirth = 0;
	}

	/**
	 ** Constructor - create a person with the specifed attributes
	 **/
	Person(String firstName, String lastName, long yearOfBirth)
	{
		setFirstName(firstName);
		setLastName(lastName);
		setYearOfBirth(yearOfBirth);
	}

	/**
	 ** set this person's first name
	 **/
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	/**
	 ** get this person's first name
	 **/
	public String getFirstName()
	{
		return firstName;
	}
	
	/**
	 ** set this person's last name
	 **/
	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	/**
	 ** get this person's last name
	 **/
	public String getLastName()
	{
		return lastName;
	}
 	
	/**
	 ** set this person's year of birth
	 **/
	public void setYearOfBirth(long yearOfBirth)
	{
		this.yearOfBirth = yearOfBirth;
	}

	/**
	 ** get this person's year of birth
	 **/
	public long getYearOfBirth()
	{
		return yearOfBirth;
	}

	/**
	 ** create a String representing this person
	 **/
	public String toString()
	{
		return "Name:" + firstName + " " + lastName + "\n" + "Year of birth: " + yearOfBirth + "\n";
	}
}
