
/**
 * Used to house "utility" compare to objects of same type.
 * The main intended use is for assisting in the translation of
 * BlueJ UI text for localisation to languages other than English.
 * 
 * @author Bruce Quig 
 * @version 0.1
 */

import java.util.*;

public class Comparer
{
	
	/**
	 * Compares to Properties files and finds missing keys in slave file
	 * @param  y   a sample parameter for a method
	 * @return     the sum of x and y 
	 */
	public static void findMissingKeys(Properties master, Properties slave)
	{
            Enumeration mKeys = master.keys();
            //Iterator it = mKeys.iterator();
            while(mKeys.hasMoreElements()) {
                String value = (String)mKeys.nextElement();
                if(!slave.containsKey(value))
                    System.out.println("Untranslated: " + value);
            }         
        
	}
}
