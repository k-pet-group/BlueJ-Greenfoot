/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2015,2018,2019  Michael Kolling and John Rosenberg
 
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

#define _WIN32_WINNT 0x0502

#include <windows.h>
#include "resources.h"
#include <set>
#include <string>
#include <cstring>
#include <list>
#include <shlwapi.h>
#include <cstdio>

#undef __cplusplus
#include <jni.h>
#define __cplusplus

#ifndef APPNAME
#define APPNAME "BlueJ"
#endif

#ifndef REQUIREDJAVA
#define REQUIREDJAVA "1.5"
#endif


// Handy typedef
typedef std::basic_string<TCHAR> string;

// This define must match that in bjdialog.cc
#define MSG_LAUNCHVM WM_APP

// Methods from javatest.cc
bool testJdkPath(string jdkLocation, string *reason);

// Methods from javaprops.cc
string readJavaProperty(string file, std::string propertyName);
string getBlueJProperty(std::string propertyName);

// GLOBAL VARIABLES


// Application instance
HINSTANCE appInstance = 0;

// The version number, set in WinMain
string appVersion;

// The path to BlueJ (where the launcher is located). Set in WinMain.
string bluejPath;

// The path to User's home directory (java.home). Guessed in WinMain.
LPTSTR userHomePath;

// List of arguments to be passed to BlueJ's main method
std::list<LPCWSTR> bjargs;

// List of arguments for the main VM
std::list<LPCWSTR> windowsvmargs;

// Whether we should always launch java as an external process
bool externalLaunch = false;

#ifdef GREENFOOT

static const char *VM_ARGS_PROP = "greenfoot.windows.vm.args";
static const char *VM_PROP = "greenfoot.windows.vm";

#else

static const char *VM_ARGS_PROP = "bluej.windows.vm.args";
static const char *VM_PROP = "bluej.windows.vm";

#endif

// convert a UTF16 string to a UTF8 string.
// Result should be delete[]'d when done with.
static char * wideToUTF8(std::basic_string<WCHAR> s)
{
    // Assume max of 4 UTF8 bytes to represent a WCHAR
    int inputChars = s.length();
    int outputUTF8size = inputChars * 4 + 1;

    char *outputUTF8 = new char[outputUTF8size];
    WideCharToMultiByte(CP_UTF8, 0, s.c_str(), inputChars + 1,
            outputUTF8, outputUTF8size, NULL, NULL);

    return outputUTF8;
}

// convert a UTF16 string to a string in the windows system codepage
static char * wideToACP(std::basic_string<WCHAR> s)
{
    // Assume max of 4 UTF8 bytes to represent a WCHAR
    int inputChars = s.length();
    int outputUTF8size = inputChars * 4 + 1;

    char *outputUTF8 = new char[outputUTF8size];
    WideCharToMultiByte(CP_ACP, 0, s.c_str(), inputChars + 1,
            outputUTF8, outputUTF8size, NULL, NULL);

    return outputUTF8;
}

// Escape (quote) a command line parameter if necessary.
// Windows uses a really stupid escaping system - any number of backslashes not followed
// by a quote are not escapes; (2n) or (2n+1) backslashes followed by a quote correspond
// to (n) backslashes followed by a quote when unescaped.
string escapeCmdlineParam(string arg)
{
    bool hasSpace = arg.find(TEXT(' ')) != string::npos;

    if (! hasSpace && arg.find(TEXT('\"')) == string::npos) {
        return arg;
    }

    // Contains a space or a quote; must quote the argument
    string output;
    if (hasSpace) {
        output = TEXT("\"");
    }
    unsigned bsCount = 0;
    for (string::iterator i = arg.begin(); i != arg.end(); i++) {
        TCHAR c = *i;
        if (c == TEXT('\\')) {
            bsCount++;
        }
        else if (c == TEXT('\"')) {
            // Possibly a number of backslashes before a quote.
            // We need to double the number of backslashes:
            for (unsigned j = 0; j < bsCount; j++) {
                output += TEXT('\\');
            }
            bsCount = 0;
        }
        else {
            bsCount = 0;
        }
        output += c;
    }
    if (hasSpace) {
        output += TEXT('\"');
    }
    return output;
}

// Launch VM as an external process
// Returns - true if successful
bool launchVMexternal(string jdkLocation)
{
    string javaCommand = jdkLocation + TEXT("\\bin\\javaw.exe");

    string commandLine = escapeCmdlineParam(javaCommand) + TEXT(' ');

    bool firstArg = true;
    for (std::list<LPCTSTR>::iterator i = windowsvmargs.begin(); i != windowsvmargs.end(); ++i) {
        if (! firstArg) {
            commandLine += ' ';
        }
        firstArg = false;
        commandLine += escapeCmdlineParam(*i);
    }

    if (! firstArg) {
        commandLine += TEXT(' ');
    }

    commandLine += TEXT("-classpath ");
    string classPathString = bluejPath + TEXT("\\lib\\bluej.jar");
    string sep(TEXT(";"));
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.base.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.controls.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.fxml.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.graphics.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.media.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.properties.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.swing.jar");
    classPathString += sep + bluejPath + TEXT("\\lib\\javafx\\lib\\javafx.web.jar");
    commandLine += escapeCmdlineParam(classPathString);

    commandLine += TEXT(" bluej.Boot");

    for (std::list<LPCTSTR>::iterator i = bjargs.begin(); i != bjargs.end(); ++i) {
        commandLine += TEXT(' ');
        commandLine += escapeCmdlineParam(*i);
    }

    PROCESS_INFORMATION processInfo;
    STARTUPINFO startupInfo;

    ZeroMemory(&processInfo, sizeof(processInfo));
    ZeroMemory(&startupInfo, sizeof(startupInfo));
    startupInfo.cb = sizeof(startupInfo);

    LPTSTR cmdLineCopy = new TCHAR[commandLine.length() + 1];
    lstrcpy(cmdLineCopy, commandLine.c_str());

    DWORD error = ERROR_SUCCESS;
    if (!CreateProcess(javaCommand.c_str(), cmdLineCopy, NULL, NULL, TRUE, 0, NULL,
            NULL, &startupInfo, &processInfo)) {
        // Error - try java.exe instead of javaw.exe
        error = GetLastError();
        lstrcpy(cmdLineCopy, commandLine.c_str());
        javaCommand = jdkLocation + TEXT("\\bin\\java.exe");

        if (!CreateProcess(javaCommand.c_str(), cmdLineCopy, NULL, NULL, TRUE, 0, NULL,
                bluejPath.c_str(), &startupInfo, &processInfo)) {
            if (error == ERROR_FILE_NOT_FOUND) {
                // javaw.exe wasn't found, but that's not a useful error message...
                error = GetLastError();
            }

            TCHAR buf[1024];
            wsprintf (buf, TEXT(APPNAME " could not launch Java: Error h\'%X"), GetLastError ());
            MessageBox(0, buf, TEXT(APPNAME), MB_ICONEXCLAMATION | MB_OK);
            return false;
        }
    }

    return true;
}



// Launch VM and run bluej.Boot class
//   jdkLocation - location of the JDK (no trailing slash!)
// Returns - false on failure
//       Note, if this call succeeds, it might not return at all.
bool launchVM(string jdkLocation)
{
    if (externalLaunch) {
        return launchVMexternal(jdkLocation);
    }

    // If there are VM arguments other than -Dxxx=yyy and -Xxxxx, do an external launch. It is too
    // difficul to translate java.exe arguments to javavm.dll options.
    for (std::list<LPCTSTR>::iterator i = windowsvmargs.begin(); i != windowsvmargs.end(); ++i ) {
        string vmarg = string(*i);
        if(vmarg.compare(0, 2, TEXT("-D"), 2) != 0 && vmarg.compare(0, 2, TEXT("-X"), 2) != 0) {
            return launchVMexternal(jdkLocation);
        }
    }

    typedef typeof(JNI_CreateJavaVM) *JNI_CreateJavaVM_t;
    //typedef typeof(JNI_GetDefaultJavaVMInitArgs) *JNI_GetDefaultJavaVMInitArgs_t;

    JavaVM *javaVM;
    JNIEnv *jniEnv;

    // Loading the JVM dll then requires loading msvcr71.dll (Java 6) or msvcr100.dll (Java 7).
    // The msvcrXXX.dll is sometimes in the system directory, but if it's not it won't be found
    // automatically. We use SetDllDirectory to specify the search location:

    string jvmDllPath = jdkLocation + TEXT("\\bin");
    SetDllDirectory(jvmDllPath.c_str());

    // Now load the JVM.
    HINSTANCE hJavalib;
    jvmDllPath += TEXT("\\server\\jvm.dll");
    hJavalib = LoadLibrary(jvmDllPath.c_str());

    if (hJavalib == NULL) {
        return launchVMexternal(jdkLocation);
    }

    // We've loaded a VM, but if we bail out later, let's make sure we don't load
    // another VM and have two loaded at once. That might cause problems.
    externalLaunch = true;

    // Make real classpath string. Needs to be Ansi (system codepage)

    char * bjDirACP = wideToACP(bluejPath);
    char * jdkLocACP = wideToACP(jdkLocation);

    std::string classPathOpt = "-Djava.class.path=";
    (classPathOpt += bjDirACP) += "\\lib\\bluej.jar";
    const char* sep = ";";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.base.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.controls.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.fxml.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.graphics.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.media.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.properties.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.swing.jar";
    ((classPathOpt += sep) += bjDirACP) += "\\lib\\javafx\\lib\\javafx.web.jar";

    delete [] bjDirACP;
    delete [] jdkLocACP;

    char *classPathOptArr = new char[classPathOpt.length() + 1];
    strcpy(classPathOptArr, classPathOpt.c_str());

    JavaVMInitArgs vm_args;

    JavaVMOption * options = new JavaVMOption[1 + windowsvmargs.size()];
    options[0].optionString = classPathOptArr;
    options[0].extraInfo = NULL;
    int j = 1;
    for (std::list<LPCTSTR>::iterator i = windowsvmargs.begin(); i != windowsvmargs.end(); ) {
        options[j].optionString = wideToUTF8(string(*i));
        options[j].extraInfo = NULL;
        j++; i++;
    }

    vm_args.version = 0x00010002;
    vm_args.options = options;
    vm_args.nOptions = 1 + windowsvmargs.size();
    vm_args.ignoreUnrecognized = JNI_TRUE;

    JNI_CreateJavaVM_t CreateJavaVM_ptr = (JNI_CreateJavaVM_t) GetProcAddress( hJavalib, "JNI_CreateJavaVM" );
    if (CreateJavaVM_ptr == NULL) {
        for (j = 0; j <= (int)windowsvmargs.size(); j++) {
            delete [] options[j].optionString;
        }
        delete [] options;
        return launchVMexternal(jdkLocation);
    }

    int res = CreateJavaVM_ptr(&javaVM, (void **)&jniEnv, &vm_args);

    for (j = 0; j <= (int)windowsvmargs.size(); j++) {
        delete [] options[j].optionString;
    }
    delete [] options;

    if (res < 0 || jniEnv == NULL) {
        return launchVMexternal(jdkLocation);
    }

    jclass cls = (*jniEnv)->FindClass(jniEnv, "bluej/Boot");
    jmethodID mid;
    if (cls == NULL) {
        MessageBox (0, TEXT("Couldn't find bluej.Boot class"), TEXT("BlueJ launcher"), MB_ICONEXCLAMATION | MB_OK);
        goto destroyvm;
    }

    mid = (*jniEnv)->GetStaticMethodID(jniEnv, cls, "main", "([Ljava/lang/String;)V");
    if (mid == NULL) {
        MessageBox (0, TEXT("Couldn't find main() method in bluej.Boot class"), TEXT("BlueJ launcher"), MB_ICONEXCLAMATION | MB_OK);
        goto destroyvm;
    }

    {
        // Create the String[] argument for main
        jclass stringClass = (*jniEnv)->FindClass(jniEnv, "java/lang/String");
        jobjectArray args = (*jniEnv)->NewObjectArray(jniEnv, bjargs.size(), stringClass, NULL);

        {
            int j = 0;
            for (std::list<LPCTSTR>::iterator i = bjargs.begin(); i != bjargs.end();) {
                jstring argString = (*jniEnv)->NewString(jniEnv, (const jchar *) *i, lstrlen(*i));
                (*jniEnv)->SetObjectArrayElement(jniEnv, args, j, argString);
                ++j; ++i;
            }
        }


        (*jniEnv)->CallStaticVoidMethod(jniEnv, cls, mid, args);

        if ((*jniEnv)->ExceptionOccurred(jniEnv)) {
            // if an exception occurs, output to stderr (which is usually invisible, oh well...)
            (*jniEnv)->ExceptionDescribe(jniEnv);
        }

        // Run a windows message loop. This is a simple way to sleep while BlueJ runs.
        MSG  msg;
        int status;
        while ((status = GetMessage (& msg, 0, 0, 0)) != 0)
        {
            if (status == -1) {
                return true; // shouldn't happen anyway
            }

            TranslateMessage ( & msg );
            DispatchMessage ( & msg );
        }
    }

    destroyvm:
    (*javaVM)->DestroyJavaVM(javaVM);
    return true;
}

#ifndef KEY_WOW64_64KEY
#define KEY_WOW64_64KEY 0x100
#endif

// Extract path from file
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

static bool isStdSpace(TCHAR ch)
{
    return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
}

static string trimString(const string &src)
{
    std::size_t slen = src.length();
    std::size_t i;
    for (i = 0; i < slen; i++) {
        TCHAR srcChar = src[i];
        if (! isStdSpace(srcChar)) {
            break;
        }
    }

    if (i == slen) {
        return string();
    }

    std::size_t j;
    for (j = slen - 1; j > i; j--) {
        TCHAR srcChar = src[i];
        if (! isStdSpace(srcChar)) {
            break;
        }
    }

    return src.substr(i, j - i + 1);
}

// Display a message box with a message and an "OK" button.
static void displayMessage(LPCTSTR msg)
{
    MessageBox(0, msg, TEXT(APPNAME), MB_ICONEXCLAMATION | MB_OK);
}

// Get the current working directory as a dynamically allocated array.
// Returns NULL if unsucessful or a pointer to the allocated string,
// which should be freed with delete[].
static TCHAR *getCurrentDir()
{
	DWORD reqLen = GetCurrentDirectory(0, NULL);
	if (reqLen == 0) {
		return NULL;
	}
	TCHAR * buf = new TCHAR[reqLen];
	DWORD gcdResult = GetCurrentDirectory(reqLen, buf);
	if (gcdResult == 0 || gcdResult > reqLen) {
		delete [] buf;
		return NULL;
	}
	return buf;
}

// Given two paths, convert the first to an absolute path and apply
// the second path as a relative path to it. Store the resulting path.
// (If the second path is an absolute path, returns it unmodified).
//
// Returns true if successful or false if not.
static bool getAbsolutePath(string &result, const string &bjp, const string &jdkPath)
{
    if (PathIsRelative(jdkPath.c_str()) == 0) {
        //the jdk is actually absolute. Nothing to be done.
        result = jdkPath;
        return true;
    }
    
    // jdkPath is not absolute.
    // Determine the absolute path to bjp. (Note that PathCombine does not work properly
    // if the first path given to it begins with a ".." sequence, which is why we need
    // an absolute path).

    LPCTSTR bjPath = bjp.c_str();
    bool bjPathAllocd = false;

    if (PathIsRelative(bjPath)) {
    	LPTSTR currentDirectory = getCurrentDir();
    	if (currentDirectory == NULL) {
    		displayMessage(TEXT("Unable to determine current directory."));
    		return false;
    	}

    	LPTSTR tmpPath = new TCHAR[MAX_PATH];
    	if (PathCombine(tmpPath, currentDirectory, bjPath) == NULL) {
    		return false;
    	}

    	bjPath = tmpPath;
    	bjPathAllocd = true;
    }

    // We now have an absolute path in bjPath. Combine it with jdkPath:
    LPTSTR resultPath = new TCHAR[MAX_PATH];
    if (PathCombine(resultPath, bjPath, jdkPath.c_str()) == NULL) {
    	return false;
    }

    // Canonicalize, for good measure:
    LPTSTR canonicalPath = new TCHAR[MAX_PATH];
    if (PathCanonicalize(canonicalPath, resultPath)) {
    	delete [] resultPath;
    	resultPath = canonicalPath;
    }
    else {
    	delete [] canonicalPath;
    }

    result.clear();
    result.append(resultPath);
    delete [] resultPath;

    if (bjPathAllocd) {
    	delete [] bjPath;
    }

    return true;
}

// Program entry point
int WINAPI WinMain
   (HINSTANCE hInst, HINSTANCE hPrevInst, char * cmdParam, int cmdShow)
{
    appInstance = hInst;
    SetErrorMode(SEM_FAILCRITICALERRORS | SEM_NOOPENFILEERRORBOX);

    LPTSTR commandLine = GetCommandLine();

    int argCount = 0;
    LPWSTR *args = CommandLineToArgvW(commandLine, &argCount);

    // args[0] = executable path. However, despite misleading MS
    // documentation, it is not necessarily a fully qualified path
    // (it might be a relative path).

    LPTSTR bluejPathBuffer = new TCHAR[MAX_PATH];
    if (!GetFullPathName(args[0], MAX_PATH, bluejPathBuffer, NULL)) {
            MessageBox(0, TEXT("Couldn't get path to launcher executable"), TEXT(APPNAME), MB_ICONERROR | MB_OK);
        return 1;
    }
    bluejPath = extractFilePath(bluejPathBuffer);
    delete [] bluejPathBuffer;

    // Check parameters
    for (int i = 1; i < argCount; i++) {
        if (lstrcmpi(TEXT("/javaw"), args[i]) == 0) {
            // ignore for backwards compatibility
        }
        else if (lstrcmpi(TEXT("/externalvm"), args[i]) == 0) {
            // force external launch of VM (as a separate process)
            externalLaunch = true;
        }
        else {
            bjargs.push_back(args[i]);
        }
    }

#ifdef GREENFOOT
    bjargs.push_back(TEXT("-greenfoot=true"));
    bjargs.push_back(TEXT("-bluej.compiler.showunchecked=false"));
#endif

    // Get application version string from version resource
    HRSRC hRsrc = FindResource(NULL, MAKEINTRESOURCE(1), RT_VERSION);
    if (hRsrc != NULL) {
        HGLOBAL hGlbl = LoadResource(NULL, hRsrc);
        if (hGlbl != NULL) {
            BYTE *verBuffer = (BYTE *) LockResource(hGlbl);
            if (verBuffer != NULL) {
                UINT plen = 0;
                LPTSTR productVersion = 0;
                /* BOOL verResult = */
                VerQueryValue(verBuffer,
                        TEXT("\\StringFileInfo\\04091200\\ProductVersion"),
                        (void **) &productVersion, &plen);
                appVersion = productVersion;
            }
            else {
                hRsrc = NULL;
            }
        }
        else {
            hRsrc = NULL;
        }
    }

    // Locate user home directory
    {
        string userHomeString = trimString(getBlueJProperty("bluej.userHome"));
        if (! userHomeString.empty()) {
            userHomePath = new TCHAR[userHomeString.length() + 1];
            lstrcpy(userHomePath, userHomeString.c_str());
        }
        else {
            DWORD envHomeSize = GetEnvironmentVariable(TEXT("HOME"), NULL, 0);
            if (envHomeSize != 0) {
                userHomePath = new TCHAR[envHomeSize];
                GetEnvironmentVariable(TEXT("HOME"), userHomePath, envHomeSize);
            }
        }
    }

    // Get bluej.windows.vm.args
    {
        string windowsvmargsString = trimString(getBlueJProperty(VM_ARGS_PROP));
        if (! windowsvmargsString.empty()) {
            int argCount = 0;
            // TODO it's technically wrong to use CommandLineToArgvW, as the
            // escaping technique it supports is slightly different.
            LPWSTR *args = CommandLineToArgvW(windowsvmargsString.c_str(), &argCount);
            for (int i = 0; i < argCount; i++) {
                windowsvmargs.push_back(args[i]);
            }
        }
    }

    // Check for VM in bluej.defs
    string defsVm = getBlueJProperty(VM_PROP);
    if (defsVm.length() != 0) {
        // Gets the VM's absolute path
        if (! getAbsolutePath(defsVm, bluejPath, defsVm)) {
            displayMessage(TEXT("Could not determine JDK path -\n" "specified path or current directory may be too long"));
            return 0;
        }

        string reason;                
        if (testJdkPath(defsVm, &reason)) {
            if (launchVM(defsVm)) {
                return 0;
            }
            else {
                return 0;
            }
        } 
    }

    MessageBox (0, TEXT("No (suitable) Java JDKs were found. " APPNAME " requires JDK version " REQUIREDJAVA " or later.\n"
        "Please also note, the Java Runtime Environment (JRE) is not sufficient.\n"
        "You must have a JDK to run " APPNAME "."),
        TEXT(APPNAME), MB_ICONEXCLAMATION | MB_OK);

    return 0;
}
