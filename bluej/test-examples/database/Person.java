/*
Author:  Morten Knudsen & Kent Hansen
Version: 1.0
Date:    July 1998
Short:   Person, a superclass for Staff and Student.

 This class is part of a simple Database program, made to demonstrate JavaBlue
 by Michael Kolling.

*/

class Person
{
	String m_strFirstName;
	String m_strLastName;
	long m_nYearOfBirth;

	/**
	 ** construcot for person
	 **/
	Person()
	{
		m_strFirstName = new String("");
		m_strLastName = new String("");
		m_nYearOfBirth = 1900;
	}

	Person(String strFirstName, String strLastName, long nYearOfBirth)
	{
		m_strFirstName = new String(strFirstName);
		m_strLastName = new String(strLastName);
		m_nYearOfBirth = nYearOfBirth;
	}

	/**
	 ** comment...
	 **/
	public void setFirstName(String strFirstName)
	{
		m_strFirstName = strFirstName;
	}

	public String getFirstName()
	{
		return m_strFirstName;
	}
	
	public void setLastName(String strLastName)
	{
		m_strLastName = strLastName;
	}

	public String getLastName()
	{
		return m_strLastName;
	}
 	
	public void setYearOfBirthName(long nYearOfBirth)
	{
		m_nYearOfBirth = nYearOfBirth;
	}

	public long getYearOfBirth()
	{
		return m_nYearOfBirth;
	}

	public String toString()
	{
		return "class Person \n"+"Name:"+m_strFirstName+" "+m_strLastName+"\n"+"Year of birth: "+m_nYearOfBirth+"\n";
	}
}

