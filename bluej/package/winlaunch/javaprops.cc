/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg
 
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

/* Read Java property files */

#define UNICODE
#include <windows.h>

#include <string>
#include <istream>

#include "winfilebuf.h"

typedef std::basic_string<TCHAR> string;


extern string bluejPath;  // from bjlaunch.cc

extern LPTSTR userHomePath; // from bjlaunch.cc


// Unescape a string read from a Java properties file - "\x" becomes "x"
// TODO should also unescape unicode sequences (\uXXXX)
static string dePropify(std::string str)
{
        std::string::iterator ii = str.begin();
        std::string::iterator end = str.end();
        string rval;

        while (ii != end) {
                char c = *ii;
                if (c == '\\') {
                        ++ii;
                        if (ii != end) {
                                c = *ii;
                                ++ii;
                                if (c == '\\' || c == ':') {
                                        // Backslashes and colons are normally escaped
                                        rval += (TCHAR) c;

                                }
                                else {
                                        // Other characters aren't. If we get a backslash followed
                                        // by such a character, we'll assume the user edited the file
                                        // by hand and forgot to double the backslash.
                                        rval += TEXT('\\');
                                        rval += (TCHAR) *ii;
                                }
                        }
                        else {
                                // strange - trailing backslash.
                                rval += '\\';
                        }
                }
                else {
                        rval += (TCHAR) c;
                        ++ii;
                }
        }

        return rval;
}


// Try to read a property value from a file
//
// Note, property names should be pre-encoded with \uXXXX sequences
// if they contains non-ascii characters. Returned string won't have
// such sequences decoded if they occur in the value.
// Note '=' must immediately follow the property name (TODO).
string readJavaProperty(string file, std::string propertyName)
{
        using std::fstream;
        using std::ios_base;
        using std::getline;

        // Open the file
        HANDLE fileHandle = CreateFile(file.c_str(), FILE_READ_DATA, FILE_SHARE_READ, NULL, OPEN_EXISTING,
                        FILE_ATTRIBUTE_NORMAL, NULL);
        if (fileHandle == INVALID_HANDLE_VALUE) {
                return TEXT("");
        }

        WinFileBuf fileBuf(fileHandle);
        std::istream inputStream(&fileBuf);

        propertyName += '=';
        int propSize = propertyName.length();

        string retval;
        std::string line;

        while(getline(inputStream, line))
        {
                if (line.compare(0, propSize, propertyName) == 0) {
                        // it's a match
                        // Remove the "property=" part and the trailing newline.

                	    // Check there actually is a trailing newline before removing it...
                		TCHAR lineEndChar = line[line.length() - 1];
                		int lineEndStrip = (lineEndChar == TEXT('\r') || lineEndChar == TEXT('\n')) ? 1 : 0;

                        std::string matchPart = line.substr(propSize, line.length() - propSize - lineEndStrip);
                        retval = dePropify(matchPart);
                        // break; - no; later values override earlier ones
                }
        }

        CloseHandle(fileHandle);
        return retval;
}

// Read a BlueJ property
string getBlueJProperty(std::string propertyName)
{
        if (userHomePath != NULL) {
                string bjpropsPath = userHomePath;
                bjpropsPath += TEXT("\\bluej\\bluej.properties");
                string rval = readJavaProperty(bjpropsPath, propertyName);
                if (rval.length() != 0) {
                        return rval;
                }
        }
#ifdef GREENFOOT
        return readJavaProperty(bluejPath + TEXT("\\lib\\greenfoot.defs"), propertyName);
#else
        return readJavaProperty(bluejPath + TEXT("\\lib\\bluej.defs"), propertyName);
#endif
}
