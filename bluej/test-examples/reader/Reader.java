/**
 ** Class Reader - brief description
 **/
import java.io.*;

public class Reader
{
	/**
	 ** Constructor for objects of class Reader
	 **/
	public Reader()
	{
	}

	/**
	 ** Read a line of input, and return it
	 **/
	public String readLine()
	{
		System.out.print("Enter some text: ");

		StringBuffer buf = new StringBuffer();
		int c;

		try {
			while(((c = System.in.read()) != -1) && (c != '\n'))
				buf.append((char)c);
		} catch(IOException e) {
			// ignore it
		}

		return buf.toString();

	}
}
