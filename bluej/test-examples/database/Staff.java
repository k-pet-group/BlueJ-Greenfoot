/**
 ** @author Morten Knudsen & Kent Hansen
 ** @version 1.0
 ** @date July 1998
 ** Staff is a subclass of Person, for storing Staff specific data.
 ** 
 ** This class is part of a simple Database program, made to demonstrate
 ** JavaBlue by Michael Kolling.
 **/
class Staff extends Person
{
	String m_strRoom;
	String m_strPos;

	Staff()
	{
		super();
		m_strRoom = new String("");
		m_strPos = new String("professor");
	}

	Staff(String strFirstName, String strLastName,long nYearOfBirth,String strRoom,String strPos)
	{
		super(strFirstName,strLastName,nYearOfBirth);
		m_strRoom = new String(strRoom);
		m_strPos = new String(strPos);
		
	}

	public void setRoom(String strRoom)
	{
		m_strRoom = strRoom;
	}

	public String getRoom()
	{
		return m_strRoom;
	}


	public void setPos(String strPos)
	{
		m_strPos = strPos;
	}

	public String getPos()
	{
		return m_strPos;
	}

	public String toString()
	{
		return super.toString()+"Room: "+m_strRoom+"\n"+"Position: "+m_strPos+"\n";
	}

}
