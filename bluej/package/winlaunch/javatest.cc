/**
 * Routines to test for a JDK.
 *
 * We need a java.exe, and a tools.jar file before we
 * believe that we have a JDK.
 */
 
#define UNICODE
#include <windows.h>

#include <string>

 
typedef std::basic_string<TCHAR> string;

// Return true if the given path looks like a JDK path
// Return false if not, and set reason to a description of why not.
bool testJdkPath(string jdkLocation, string *reason)
{
	// Does java.exe exist?
	if (jdkLocation.length() == 0) {
		return false;
	}
	if (*(jdkLocation.rbegin()) != TEXT('\\'))  {
		jdkLocation += TEXT("\\");
	}
	
	DWORD binaryType = 0;
	string javaExeLocation = jdkLocation + TEXT("bin\\java.exe");
	
	BOOL result = GetBinaryType(javaExeLocation.c_str(), &binaryType);
	if (result == 0) {
		// Not executable
		if (reason != NULL) {
			*reason = TEXT("The java.exe file does not exist, or is not executable.");
		}
		return false;
	}
	
	string toolsJarLocation = jdkLocation + TEXT("lib\\tools.jar");
	result = GetBinaryType(toolsJarLocation.c_str(), &binaryType);
	if (result == 0 && GetLastError() != ERROR_BAD_EXE_FORMAT) {
		// No tools.jar
		if (reason != NULL) {
			*reason = TEXT("There is no tools.jar file - maybe this is just a JRE (and not a JDK).");
		}
		return false;
	}
	
	return true;
}
