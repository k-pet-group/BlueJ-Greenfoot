program vmselect;

uses
  Forms,
  Windows,
  SysUtils,
  Classes,
  Dialogs,
  main in 'main.pas' {MainForm},
  javatest in 'javatest.pas',
  consoleapp in 'consoleapp.pas',
  findfile in 'findfile.pas',
  exfile in 'exfile.pas';

{$R *.res}
{$R addedicon.res}

var
	appdir : string;
begin
  Application.Initialize;
  Application.Title := 'BlueJ Launcher';

  appdir := ExtractFilePath(Application.ExeName);

   if (DebugHook <> 0) or FileExists(appdir + 'lib\bluej.jar') then
   begin
	Application.CreateForm(TMainForm, MainForm);
	Application.Run;
   end
   else
       	MessageDlg('The file bluej.jar does not exist in the lib/ directory ' + #13#10 +
        	   'where this executable lives so I cannot launch BlueJ.', mtError, [mbOk],0);
end.
