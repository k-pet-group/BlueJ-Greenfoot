unit javatest;

interface

{
  Pass in the complete path to a java executable.
  Return true if this is a valid JDK with reason being
  set to the version of the JDK.
  Path will also be changed to be the path of the JDK
  without trailing backslash.
  Return false if this is an invalid JDK, with reason being
  why.
}
function testjavapath(var path : string; var reason : string ) : boolean;

{
  Pass in the complete path to a JDK directory.
  Return true if this is a valid JDK with reason being
  set to the version of the JDK.
  Return false if this is an invalid JDK, with reason being
  why.
}
function testjdkpath(path : string; var reason : string ) : boolean;




implementation

uses
        SysUtils, Classes, ConsoleApp;

{
  Pass in the complete path to a file.
  Test if file is called java.exe and has a tools.jar that is locatable
  relative to the java.exe file.
  Return true if this is a valid JDK java.exe. In this case reason
  is set to the path of the JDK directory WITHOUT trailing path separator.
  Return false if this is not a valid JDK java.exe with reason being
  the textual reason why it is not valid.
}
function isjdk(path : string; var reason : string) : boolean;
var
        location : string;	// directory containing java.exe
begin
        result := false;

        if not (ExtractFileName(path)='java.exe') then
        begin
		reason := 'Executable not called java.exe';
        	Exit;
        end;

        if not FileExists(path) then
        begin
        	reason := 'Java executable does not exist or is not readable';
		Exit;
        end;

        { find the directory containing the java.exe WITH trailing path }
        location := ExtractFilePath(path);

        SetCurrentDir(location);

        // test for ..\lib\tools.jar
        if FileExists('..\lib\tools.jar') then
        begin
		SetCurrentDir('..');
                reason := ExcludeTrailingPathDelimiter(GetCurrentDir);
                result := true;
                Exit;
        end;

        // sometimes a JRE is located within a valid JDK directory
        // putting the tools.jar file up an extra level
        if FileExists('..\..\lib\tools.jar') then
        begin
                SetCurrentDir('..');
                SetCurrentDir('..');
                reason := ExcludeTrailingPathDelimiter(GetCurrentDir);
                result := true;
                Exit;
        end;

        reason := 'No tools.jar - this may be a JRE installation';
end;

{
  Pass in the complete path to a JDK installation directory.
  Return the version string from the java executable in this
  JDK directory
}
function getjdkversion(jdkpath : string) : string;
var
        oldpath, firstline : string;
        templist: TStrings;
begin
        result := '';

        oldpath := GetCurrentDir;
        SetCurrentDir(jdkpath);
        SetCurrentDir('bin');

        templist := TStringList.Create;	{ construct the list object }
        try                ExecConsoleApp('java.exe','-version',
                                templist,nil);

                if templist.Count > 0 then
                begin
                        firstline := templist[0];
                        templist.Clear;
                        ExtractStrings(['"',' '],[],PChar(firstline), templist);

                        if templist.Count > 2 then
                                result := templist[2];
                end;
        finally
                templist.Free;	{ destroy the list object }
        end;

        SetCurrentDir(oldpath);
end;

{
  Pass in the complete path to a java executable.
  Return true if this is a valid JDK with reason being
  set to the version of the JDK.
  Path will also be changed to be the path of the JDK
  excluding trailing backslash.
  Return false if this is an invalid JDK, with reason being
  why.
}
function testjavapath(var path : string; var reason : string ) : boolean;
var
	newpath : string;
begin
	result := false;

        if isjdk(path, newpath) then
        begin
                reason := getjdkversion(newpath);

	        if reason = '' then
                        reason := 'java.exe could not be run to determine version'
		    else
                begin
                	if StrLIComp(PChar(reason), '"1.5', 4) >= 0 then
                        begin
	                     	result := true;
	                        path := newpath;
                        end
                        else
                        begin
				            reason := 'Greenfoot needs JDK >= 1.5';
                        end;
                end;
	    end
        else
        	reason := newpath;
end;

{
  Pass in the complete path to a JDK directory.
  Return true if this is a valid JDK with reason being
  set to the version of the JDK.
  Return false if this is an invalid JDK, with reason being
  why.
}
function testjdkpath(path : string; var reason : string ) : boolean;
var
	temppath : string;
begin
	temppath := IncludeTrailingPathDelimiter(path) + 'bin\java.exe';

	result := testjavapath(temppath, reason);
end;


end.

