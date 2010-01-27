package bluej.groupwork;

/**
 * A result indicating that an UnsupportedSettingException was thrown.
 */
public class TeamworkCommandUnsupportedSetting extends TeamworkCommandResult
{
	private String message;

	public TeamworkCommandUnsupportedSetting(String message)
	{
		this.message = message;
	}

	@Override
	public String getErrorMessage()
	{
		return message;
	}

	@Override
	public boolean isError()
	{
		return true;
	}
	
}
