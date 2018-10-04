/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2018  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */

/**
 * Routines to test for a JDK.
 *
 * We need a java.exe, and a javac.exe file before we
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
	
	string javacExeLocation = jdkLocation + TEXT("bin\\javac.exe");
	result = GetBinaryType(javacExeLocation.c_str(), &binaryType);
	if (result == 0) {
		// No tools.jar
		if (reason != NULL) {
			*reason = TEXT("The javac.exe file does not exist or is not executable - "
					"maybe this is just a JRE (and not a JDK).");
		}
		return false;
	}
	
	return true;
}
