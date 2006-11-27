package bluej.groupwork;

/**
 * This class represents the result we get back from the server when doing an
 * update. These results are lines of text that has the form <br/>
 * statuscode filename<br/>
 * the status code can be any in {A,C,M,P,R,U,?)
 * 
 * @author fisker
 */
public class UpdateResult {
	char statusCode = 'X';
	String filename;
	public static final char ADDED = 'A';
	public static final char CONFLICT = 'C';
	public static final char MODIFIED = 'M';
	public static final char PATCHED = 'P';
	public static final char REMOVED = 'R';
	public static final char UPDATED = 'U';
	public static final char UNKNOWN = '?';
	
	/**
	 * Create an UpdateResult whith statusCode and filename
	 * @param statusCode
	 * @param filename
	 */
	private UpdateResult(char statusCode, String filename){
		this.statusCode = statusCode;
		this.filename = filename;
	}
	
	/**
	 * Parse a string and create an UpdateResult. Used to parse the strings
	 * comming from an update command
	 * @param str the String to parse
	 * @return UpdateResult the resulting UpdateResult
	 * @throws UnableToParseInputException
	 */
	public static UpdateResult parse(String str) throws UnableToParseInputException{
		char statusCode = 'X';
		String filename;
				
    	boolean hasRightStructure = (str != null) && (str.length() > 3);
    	boolean hasRightStatusCode = false;
    	boolean messageOk;
    	if (hasRightStructure){
    		statusCode = str.charAt(0);
    	    hasRightStatusCode = statusCode == ADDED ||
			statusCode == CONFLICT || statusCode == MODIFIED ||
			statusCode == PATCHED || statusCode == REMOVED || 
			statusCode == UPDATED || statusCode == UNKNOWN;
    	}
    	messageOk = hasRightStructure && hasRightStatusCode;
    	
    	if (messageOk){
        	filename = str.substring(2);
        	return new UpdateResult(statusCode, filename);
        	//System.out.println("statusCode=" + statusCode + " filename=" + filename);
    	}
    	else {
    		throw new UnableToParseInputException(str); 
    	}
	}
	
	/**
	 * @return Returns the filename.
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * @return Returns the statusCode.
	 */
	public char getStatusCode() {
		return statusCode;
	}
	
	public String toString(){
		return "statusCode: " + statusCode + " filename: " + filename;
	}
}
