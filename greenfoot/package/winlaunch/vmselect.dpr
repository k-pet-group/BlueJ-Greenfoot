program vmselect;

{%File 'ModelSupport\default.txvpck'}
{%File 'ModelSupport\exfile\exfile.txvpck'}
{%File 'ModelSupport\main\main.txvpck'}
{%File 'ModelSupport\findfile\findfile.txvpck'}
{%File 'ModelSupport\javatest\javatest.txvpck'}

uses
  Forms,
  Windows,
  SysUtils,
  Classes,
  Dialogs,
  main in 'main.pas' {MainForm},
  javatest in 'javatest.pas',
  findfile in 'findfile.pas',
  exfile in 'exfile.pas';

{$R *.res}
{$R addedicon.res}

var
	appdir : string;
begin
  Application.Initialize;
  Application.Title := 'Greenfoot Launcher';
  appdir := ExtractFilePath(Application.ExeName);

   if (DebugHook <> 0) or FileExists(appdir + 'lib\bluej.jar') then
   begin
	Application.CreateForm(TMainForm, MainForm);
    Application.Run;
   end
   else
       	MessageDlg('The file bluej.jar does not exist in the lib/ directory ' + #13#10 +
        	   'where this executable lives so I cannot launch Greenfoot.', mtError, [mbOk],0);
end.
