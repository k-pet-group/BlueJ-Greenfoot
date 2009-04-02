/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
/* stream for reading from a file, using Windows API */

#include <windows.h>

#include <streambuf>

class WinFileBuf : public std::basic_streambuf<char>
{
	private:
	HANDLE myHandle;
	char myBuf[4096];

	protected:
	traits_type::int_type underflow()
	{
		DWORD bytesRead;
		BOOL rres = ReadFile(myHandle, myBuf, 4096, &bytesRead, NULL);
		if (rres == 0 || bytesRead == 0) {
			// Failure
			return traits_type::eof();
		}
		else {
			setg(myBuf, myBuf, myBuf + bytesRead);
			return myBuf[0];
		}
	}

	public:
	WinFileBuf(HANDLE fileHandle)
	{
		myHandle = fileHandle;
		setg(myBuf, myBuf + 4096, myBuf + 4096);  // first read causes underflow
	}
};
