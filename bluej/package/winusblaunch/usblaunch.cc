/*
 This file is part of the BlueJ program.
 Copyright (C) 2012  Michael Kolling and John Rosenberg

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

#define UNICODE

#include <windows.h>

#include <string>

#ifndef APPNAME
#define APPNAME "Greenfoot"
#endif

#ifndef EXENAME
#define EXENAME "greenfoot.exe"
#endif

typedef std::basic_string<TCHAR> string;

/** Extract path from file */
string extractFilePath(string filename)
{
    size_t bspos = filename.rfind('\\');
    if (bspos == string::npos) {
        return string();
    }
    else {
        return filename.substr(0, bspos);
    }
}


int WINAPI WinMain
    (HINSTANCE hInst, HINSTANCE hPrevInst, char * cmdParam, int cmdShow)
{
    // First step: figure out the path to *this* executable
    LPTSTR commandLine = GetCommandLine();

    int argCount = 0;
    LPWSTR *args = CommandLineToArgvW(commandLine, &argCount);

    // args[0] = executable path. However, despite misleading MS
    // documentation, it is not necessarily a fully qualified path
    // (it might be a relative path).

    LPTSTR pathBuffer = new TCHAR[MAX_PATH];
    if (!GetFullPathName(args[0], MAX_PATH, pathBuffer, NULL)) {
        MessageBox(0, TEXT("Couldn't get path to launcher executable"), TEXT(APPNAME), MB_ICONERROR | MB_OK);
        return 1;
    }
    string launcherPath = extractFilePath(pathBuffer);
    delete [] pathBuffer;

    // Now try and launch the app:
    PROCESS_INFORMATION processInfo;
    STARTUPINFO startupInfo;

    ZeroMemory(&processInfo, sizeof(processInfo));
    ZeroMemory(&startupInfo, sizeof(startupInfo));
    startupInfo.cb = sizeof(startupInfo);

    string executablePath = launcherPath + TEXT("\\" APPNAME "\\");
    executablePath += TEXT(EXENAME);

    int execPathLen = executablePath.length() + 1; // + 1 for null
    LPCTSTR executablePathSTR = executablePath.c_str();
    LPTSTR cmdLine = new TCHAR[executablePath.length() + 1];
    std::copy(executablePathSTR, executablePathSTR + execPathLen, cmdLine);

    if (!CreateProcess(executablePathSTR, NULL, NULL, NULL, TRUE, 0, NULL,
            launcherPath.c_str() /* working dir */, &startupInfo, &processInfo)) {
        MessageBox(0, TEXT("Couldn't find " APPNAME " folder."), TEXT(APPNAME), MB_ICONERROR | MB_OK);
        return 1;
    }

    return 0;
}
