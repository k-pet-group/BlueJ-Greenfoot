package bluej.views;

import java.lang.reflect.*;

/**
 ** @version $Id: ViewFilter.java 3318 2005-02-17 05:04:12Z davmac $
 ** @author Michael Cahill
 **
 ** A filter for views - allows only certain parts of a view to be seen.
 ** Used to implement BlueJ's "public", "package" and "inherited" views.
 **/
public final class ViewFilter
{
	public static final int PUBLIC = Modifier.PUBLIC;
    public static final int PROTECTED = PUBLIC | Modifier.PROTECTED;
	public static final int PACKAGE = PROTECTED | 0x10000;
	public static final int PRIVATE = PACKAGE | Modifier.PRIVATE;
	
	public static final int STATIC = Modifier.STATIC;
	public static final int INSTANCE = 0x20000;
	
	public static final int ABSTRACT = Modifier.ABSTRACT;
	public static final int CONCRETE = 0x40000;
	
	static final int allbits = PRIVATE | STATIC | INSTANCE | ABSTRACT | CONCRETE;
	
	int modifiers;
	
	public ViewFilter(int modifiers)
	{
		if(((modifiers & STATIC) == 0) && ((modifiers & INSTANCE) == 0))
			modifiers |= STATIC | INSTANCE;
			
		if(((modifiers & ABSTRACT) == 0) && ((modifiers & CONCRETE) == 0))
			modifiers |= ABSTRACT | CONCRETE;
			
		this.modifiers = modifiers;
	}
	
	public boolean accept(int othermods)
	{
		if((othermods & 7) == 0)
			othermods |= 0x10000;
		if((othermods & STATIC ) == 0)
			othermods |= INSTANCE;
			
		return ((allbits & othermods & ~modifiers) == 0);
	}
	
	public boolean accept(Member member)
	{
		return accept(member.getModifiers());
	}
	
	public boolean accept(MemberView member)
	{
		return accept(member.getModifiers());
	}
}
