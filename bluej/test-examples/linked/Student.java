/*
Author:  Morten Knudsen & Kent Hansen
Version: 1.0
Date:    July 1998
Short:   Student is a subclass of Person, for storing Student specific data.

 This class is part of a simple Database program, made to demonstrate JavaBlue
 by Michael Kolling.

*/

class Student extends Person
{
	String m_strStudentID;
	String m_strAccountName;

	Student()
	{
		super();
		m_strStudentID = new String("");
		m_strAccountName = new String("");
		
		
	}

	Student(String strFirstName, String strLastName,long nYearOfBirth,String strStudentID,String strAccountName)
	{
		super(strFirstName,strLastName,nYearOfBirth);
		m_strStudentID = new String(strStudentID);
		m_strAccountName = new String(strAccountName);
		
	}

	public void setStudentID(String strStudentID)
	{
		m_strStudentID = strStudentID;
	}

	public String getStudentID()
	{
		return m_strStudentID;
	}

	public void setAccountName(String strAccountName)
	{
		m_strAccountName = strAccountName;
	}

	public String getAccountName()
	{
		return m_strAccountName;
	}

	public String toString()
	{
		return super.toString()+"StudentID: "+m_strStudentID+"\n"+"AccountName: "+m_strAccountName+"\n";
	}

}
