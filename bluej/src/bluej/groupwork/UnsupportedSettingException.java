package bluej.groupwork;

/**
 * Due to the way URLs are constructed for the repositories, certain characters are
 * not supported in the username and password fields.  This exception is thrown
 * if the user has such an unsupported username or password
 *
 */
public class UnsupportedSettingException extends Exception
{
	private String reason;

	public UnsupportedSettingException(String reason)
	{
		this.reason = reason;
	}

	@Override
	public String getLocalizedMessage()
	{
		return reason;
	}

	
}
