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
	
	public UpdateResult(char statusCode, String filename){
		this.statusCode = statusCode;
		this.filename = filename;
	}
	
	public static UpdateResult parse(String m) throws UnableToParseInputException{
		char statusCode = 'X';
		String filename;
				
    	boolean hasRightStructure = (m != null) && (m.length() > 3);
    	boolean hasRightStatusCode = false;
    	boolean messageOk;
    	if (hasRightStructure){
    		statusCode = m.charAt(0);
    	    hasRightStatusCode = statusCode == 'A' ||
			statusCode == 'C' || statusCode == 'M' || statusCode == 'P' ||
			statusCode == 'R' || statusCode == 'U' || statusCode == '?';
    	}
    	messageOk = hasRightStructure && hasRightStatusCode;
    	
    	if (messageOk){
        	filename = m.substring(2);
        	return new UpdateResult(statusCode, filename);
        	//System.out.println("statusCode=" + statusCode + " filename=" + filename);
    	}
    	else {
    		throw new UnableToParseInputException(m); 
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
