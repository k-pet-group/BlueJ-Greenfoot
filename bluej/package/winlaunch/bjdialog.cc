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
 
// BlueJ/Greenfoot launcher dialog

#define UNICODE
#include <windows.h>

#include <string>
#include <set>

#include "resources.h"

#ifndef APPNAME
#define APPNAME "BlueJ"
#endif


// This define must match that in bjdialog.cc
#define MSG_LAUNCHVM WM_APP

typedef std::basic_string<TCHAR> string;

// from bjlaunch.cc
extern HINSTANCE appInstance;
extern std::set<string> goodVMs;

void launchVM(string jdkLocation);
string extractFilePath(string filename);

// from javatest.cc
bool testJdkPath(string jdkLocation, string *reason);


static const LPCTSTR foundjavacaption1 = TEXT(APPNAME " has found more than one Java version that can be used.");
static const LPCTSTR foundjavacaption2 = TEXT("Please select one and select \"Launch\" to use it with " APPNAME ".");
static const LPCTSTR foundjavacaption3 = TEXT("See the README for further information about Java versions.");

static const LPCTSTR foundonejava1 = TEXT(APPNAME "has found the following suitable Java version.");
static const LPCTSTR foundonejava2 = TEXT("Click \"Launch\" to run with this Java version.");
static const LPCTSTR foundonejava3 = TEXT("See the README for further information about Java versions.");

static const LPCTSTR nojava1 = TEXT(APPNAME " could not find a Java Development Kit. A JDK must be");
static const LPCTSTR nojava2 = TEXT("installed to run " APPNAME ". If one is installed on your system,");
static const LPCTSTR nojava3 = TEXT("select \"Browse\" and then browse to its installation directory.");



void initDialog(HWND hDialog)
{
	// Set the dialog's icon
	HANDLE mainIcon = LoadImage(appInstance, TEXT("MAINICON"), IMAGE_ICON, 0, 0,
			LR_DEFAULTCOLOR || LR_SHARED);
	
	SendMessage(hDialog, WM_SETICON, ICON_SMALL, (LPARAM) mainIcon);
	SendMessage(hDialog, WM_SETICON, ICON_BIG, (LPARAM) mainIcon);
	
	// Set the text style to bold in the three status lines
	HFONT textFont = (HFONT) SendDlgItemMessage(hDialog, ID_TEXTLINE_1, WM_GETFONT, 0, 0);
	if (textFont != NULL) {
		LOGFONT fontInfo;
		GetObject(textFont, sizeof(LOGFONT), &fontInfo);
		// TODO initially pass a NULL pointer, which returns the correct size.
		// Then dynamically allocate the LOGFONT structure.
		fontInfo.lfWeight = FW_BOLD;
		textFont = CreateFontIndirect(&fontInfo);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_1, WM_SETFONT, (WPARAM) textFont, 0);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_2, WM_SETFONT, (WPARAM) textFont, 0);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_3, WM_SETFONT, (WPARAM) textFont, 0);
	}
	
	if (goodVMs.empty()) {
		SendDlgItemMessage(hDialog, ID_TEXTLINE_1, WM_SETTEXT, 0, (LPARAM) nojava1);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_2, WM_SETTEXT, 0, (LPARAM) nojava2);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_3, WM_SETTEXT, 0, (LPARAM) nojava3);
	}
	else if (goodVMs.size() == 1) {
		SendDlgItemMessage(hDialog, ID_TEXTLINE_1, WM_SETTEXT, 0, (LPARAM) foundonejava1);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_2, WM_SETTEXT, 0, (LPARAM) foundonejava2);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_3, WM_SETTEXT, 0, (LPARAM) foundonejava3);
	}
	else {
		SendDlgItemMessage(hDialog, ID_TEXTLINE_1, WM_SETTEXT, 0, (LPARAM) foundjavacaption1);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_2, WM_SETTEXT, 0, (LPARAM) foundjavacaption2);
		SendDlgItemMessage(hDialog, ID_TEXTLINE_3, WM_SETTEXT, 0, (LPARAM) foundjavacaption3);
	}
	
	// Fill the list of VMs
	for (std::set<string>::iterator i = goodVMs.begin(); i != goodVMs.end(); ++i) {
		SendDlgItemMessage(hDialog, ID_JDKLISTBOX, LB_ADDSTRING, 0, (LPARAM) i->c_str());
	}
}


string extractFileName(string filePath)
{
	int bspos = filePath.rfind('\\');
	if (bspos == string::npos) {
		return filePath;
	}
	else {
		return filePath.substr(bspos + 1, string::npos);
	}

}

//
// Dialog procedure for the main vmselect dialog.
//
INT_PTR CALLBACK MainDialogProc (HWND hwnd, 
                          UINT message, 
                          WPARAM wParam, 
                          LPARAM lParam)
{
    // static Controller* control = 0;
    switch (message)
    {
	case WM_INITDIALOG:
		initDialog(hwnd);
		return TRUE;
	case WM_COMMAND: 
		{
			// control->Command(hwnd, LOWORD (wParam), HIWORD (wParam));
			WORD code = HIWORD(wParam);
			WORD ident = LOWORD(wParam);
			HWND control = (HWND) lParam;
			
			if (ident == ID_JDKLISTBOX) {
				if (code == LBN_SELCHANGE) {
					// Enable launch button if JDK selected
					LRESULT selIndex = SendMessage(control, LB_GETCURSEL, 0, 0);
					if (selIndex != LB_ERR) {
						EnableWindow(GetDlgItem(hwnd, ID_LAUNCHBUTTON), TRUE);
					}
					else {
						EnableWindow(GetDlgItem(hwnd, ID_LAUNCHBUTTON), FALSE);
					}
					return 0;
				}
			}
			
			else if (ident == ID_LAUNCHBUTTON) {
				if (code == BN_CLICKED) {
					// Launch button clicked
					HWND jdkListboxHwnd = GetDlgItem(hwnd, ID_JDKLISTBOX);
					LRESULT selIndex = SendMessage(jdkListboxHwnd, LB_GETCURSEL, 0, 0);
					LRESULT textLen = SendMessage(jdkListboxHwnd, LB_GETTEXTLEN, selIndex, 0);
					// textLen doesn't include null terminator
					TCHAR *selectedJdk = new TCHAR[textLen + 1];
					SendDlgItemMessage(hwnd, ID_JDKLISTBOX, LB_GETTEXT, selIndex, (LPARAM) selectedJdk);
					
					// Cause the message loop to launch the VM
					PostMessage(hwnd, MSG_LAUNCHVM, 0, (LPARAM) selectedJdk);
					
					// Don't destroy the window here - that flushes the message loop
					// or causes us to exit before the VM is launched.
					//DestroyWindow(hwnd);
					
					return 0;
				}
			}
			
			else if (ident == ID_BROWSEBUTTON) {
				if (code == BN_CLICKED) {
					// Browse button clicked
					
					LPTSTR filePath = new TCHAR[2048];
					filePath[0] = 0;
					
					OPENFILENAME lpofStruc;
					ZeroMemory(&lpofStruc, sizeof(lpofStruc));
					lpofStruc.lStructSize = sizeof(lpofStruc);
					lpofStruc.hwndOwner = NULL;
					lpofStruc.lpstrFilter = TEXT("Java VM executable files\0java.exe;javaw.exe\0\0");
					lpofStruc.lpstrFile = filePath;
					lpofStruc.nMaxFile = 2048;
					lpofStruc.lpstrTitle = TEXT("Choose the java executable (java.exe)");
					lpofStruc.Flags = /* OFN_DONTADDTORECENT | */ OFN_FILEMUSTEXIST | OFN_HIDEREADONLY | OFN_EXPLORER;
					
					EnableWindow(hwnd, FALSE);
					BOOL res = GetOpenFileName(&lpofStruc);
					EnableWindow(hwnd, TRUE);
					SetActiveWindow(hwnd);
					
					if (res) {
						string exeFile = filePath;
						// TODO launch it!
						string exePath = extractFilePath(exeFile);
						string exeDir = extractFileName(exePath);
						
						bool launched = false;
						
						if (exeDir == TEXT("bin")) {
							string exeBinPath = extractFilePath(exePath);
							if (extractFileName(exeBinPath) == TEXT("jre")) {
								// It's a jre/bin, possibly inside a JDK
								string possibleJdKPath = extractFilePath(exeBinPath);
								if (testJdkPath(possibleJdKPath, NULL)) {
									TCHAR *selectedJdk = new TCHAR[possibleJdKPath.length() + 1];
									lstrcpy(selectedJdk, possibleJdKPath.c_str());
									PostMessage(hwnd, MSG_LAUNCHVM, 0, (LPARAM) selectedJdk);
									launched = true;
								}
							}
							
							if (! launched) {
								string reason;
								if (testJdkPath(exeBinPath, &reason)) {
									TCHAR *selectedJdk = new TCHAR[exeBinPath.length() + 1];
									lstrcpy(selectedJdk, exeBinPath.c_str());
									PostMessage(hwnd, MSG_LAUNCHVM, 0, (LPARAM) selectedJdk);
								}
								else {
									MessageBox(0, reason.c_str(), TEXT(APPNAME), MB_ICONERROR | MB_OK);
								}
							}
						}
						else {
							MessageBox(0, TEXT("The chosen file does not appear to be inside a Java JDK.\n"),
									TEXT(APPNAME), MB_ICONERROR | MB_OK);
						}
					}
					
					delete [] filePath;
				}
			}
		}	
		return TRUE;
	case WM_DESTROY:
		return 0;
	case WM_CLOSE:
		// delete control;
		DestroyWindow (hwnd);
		PostQuitMessage(0);
		return 0;
    }
    return FALSE;
}
