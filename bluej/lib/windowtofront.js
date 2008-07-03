/* Brings a window to front on a Windows OS. */

var shell = WScript.CreateObject("WScript.Shell");
shell.AppActivate(WScript.Arguments.Item(0));
