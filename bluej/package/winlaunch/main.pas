unit main;

interface

{$I-}
uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, StdCtrls, FindFile, ComCtrls, ConsoleApp, ExFile, REgistry,
  Buttons;

type
  TMainForm = class(TForm)
    LaunchButton: TButton;
    SearchButton: TButton;
    GoodVM: TListView;
    BadVM: TListView;
    Label1: TLabel;
    Label2: TLabel;
    StatusBar: TStatusBar;
    BrowseButton: TButton;
    OpenDialog1: TOpenDialog;
    BitBtn1: TBitBtn;
    procedure SearchButtonClick(Sender: TObject);
    procedure LaunchButtonClick(Sender: TObject);
    procedure BrowseButtonClick(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure GoodVMSelectItem(Sender: TObject; Item: TListItem;
      Selected: Boolean);
    procedure FormDestroy(Sender: TObject);
    procedure FormShow(Sender: TObject);
    procedure GoodVMExit(Sender: TObject);
    procedure BitBtn1Click(Sender: TObject);
  private
        ff : TFindFile;
        goodvmsfound : TStringList;

        procedure FindFileFound(Sender: TObject; Folder: String;
                                var FileInfo: TSearchRec);

        procedure NewFolder(Sender: TObject; Folder: String;
                                var IgnoreFolder: Boolean);

        procedure SearchDone(Sender: TObject);

        procedure AddGoodVM(jdkpath, ver : string);

        procedure GoodVMCheck;

        function LaunchBlueJ(jdkpath : string) : boolean;

  public

  end;

var
  MainForm: TMainForm;

  dumbmode : boolean = false;
  forcedialog : boolean = false;

const
        jdkregkey : string = '\Software\JavaSoft\Java Development Kit';
        bluejregkey : string = '\Software\BlueJ';

implementation

uses
        javatest;

{$R *.dfm}

function BuildDriveStrings : string;
var
        bitmask,i  : integer;
begin
        bitmask := GetLogicalDrives;

        for i := 0 to 31 do
        begin
                if (bitmask and (1 shl i)) > 0 then
                        result := result + Chr(Ord('A') + i) + ':\;';
        end;

end;

procedure TMainForm.SearchButtonClick(Sender: TObject);
begin
        if ff.Busy then
                ff.Abort
        else
        begin
                // Fills FileFile properties
                ff.Threaded := true;
                // - Name & Location
                ff.Filename := 'java.exe';
                ff.Location := BuildDriveStrings;
                ff.Subfolders := true;

                ff.OnFound := FindFileFound;
                ff.OnNewFolder := NewFolder;
                ff.OnComplete := SearchDone;

                ff.Execute;

                SearchButton.Caption := 'Stop Search';
        end;
end;

procedure TMainForm.NewFolder(Sender: TObject; Folder: String;
                                var IgnoreFolder: Boolean);
begin
        StatusBar.SimpleText := 'Searching ' + Folder + '...';
        GoodVMCheck;
end;

procedure TMainForm.FindFileFound(Sender: TObject; Folder: String;
					var FileInfo: TSearchRec);
var
        javapath, reason : string;
begin
        javapath := Folder + '\' + FileInfo.Name;
        reason := '';

        StatusBar.SimpleText := 'Found VM at ' + javapath;

        if testjavapath(javapath, reason) then
        begin
	        AddGoodVM(javapath, reason);
        end
        else
        begin
                with BadVM.Items.Add do
                begin
                        Caption := javapath;
                        SubItems.Add(reason)
                end;
        end;

        if not ff.Threaded then
                Application.ProcessMessages;

end;

procedure TMainForm.AddGoodVM(jdkpath, ver : string);
begin
        try
                goodvmsfound.Add(jdkpath);

                with GoodVM.Items.Add do
                begin
                        Caption := jdkpath;
                        SubItems.Add(ver);
                end;

                GoodVMCheck;

        except on EStringListError do
                ;
        end;


end;

procedure TMainForm.SearchDone(Sender: TObject);
begin
        StatusBar.SimpleText := 'Searching complete';
        SearchButton.Caption := 'Start Complete Search';

        GoodVMCheck;
end;

function TMainForm.LaunchBlueJ(jdkpath : string) : boolean;
var
        appdir, appdirlib, vmfilename, tooljarfilename,
          bluejjarfilename, editorjarfilename,
          antlrjarfilename, mrjjarfilename : string;
        exfile : TExFile;
begin
	result := false;

        appdir := ExtractFilePath(Application.ExeName);

	// vmfilename is automatically wrapped in quotes by ExecConsoleApp so
	// there is no need for us to do it
	vmfilename := ExcludeTrailingPathDelimiter(jdkpath) + '\bin\java.exe';

        appdirlib := '"' + appdir + 'lib\';

        bluejjarfilename := appdirlib + 'bluej.jar' + '"';
        editorjarfilename := appdirlib + 'editor.jar' + '"';
        antlrjarfilename := appdirlib + 'antlr.jar' + '"';
        mrjjarfilename := appdirlib + 'MRJToolkitStubs.zip' + '"';

        tooljarfilename := '"' + ExcludeTrailingPathDelimiter(jdkpath) + '\lib\tools.jar' + '"';

        exfile := TExFile.Create(MainForm);

        exfile.WaitUntilDone := false;
        exfile.WindowType := wtMinimize;
        exfile.ProcFileName := vmfilename;
        exfile.ProcParameters := '-cp ' + bluejjarfilename + ';' +
                                           editorjarfilename + ';' +
                                           antlrjarfilename + ';' +
                                           mrjjarfilename + ';' +
                                           tooljarfilename + ' bluej.Main';
        result := exfile.Execute;
end;

procedure TMainForm.LaunchButtonClick(Sender: TObject);
var
	reg : TRegistry;
begin
        if GoodVM.Selected = nil then
                Exit;

	LaunchBlueJ(GoodVM.Selected.Caption);

        reg := TRegistry.Create;
        try
                reg.RootKey := HKEY_CURRENT_USER;

                if reg.OpenKey(bluejregkey, true) then
                begin
                	reg.WriteString('CurrentVM', GoodVM.Selected.Caption);
                end;
	finally
        	reg.Free;
        end;

        Close;
end;

procedure TMainForm.BrowseButtonClick(Sender: TObject);
var
        javapath, reason : string;
begin
        OpenDialog1.Execute;

        javapath := OpenDialog1.FileName;
	reason := '';

        if testjavapath(javapath, reason) then
        begin
      	        AddGoodVM(javapath, reason);
	end;

        GoodVMCheck;
end;

procedure TMainForm.FormCreate(Sender: TObject);
var
        reg: TRegistry;
        i : integer;
        home, mode, ver : string;
begin
	MainForm.Height := 200;

        ff := TFindFile.Create(MainForm);

        goodvmsfound := TStringList.Create;
        goodvmsfound.Duplicates := dupError;
        goodvmsfound.CaseSensitive := false;
        goodvmsfound.Sorted := true;

        for i := 1 to ParamCount do
        begin
                if LowerCase(ParamStr(i)) = '-select' then
                        forcedialog := true
                else if LowerCase(ParamStr(i)) = '-express' then
                        dumbmode := true;
        end;

        reg := TRegistry.Create;
        try
                reg.RootKey := HKEY_CURRENT_USER;

                if reg.OpenKey(bluejregkey, false) then
                begin
                        home := reg.ReadString('CurrentVM');
                      	mode := reg.ReadString('UserMode');

                        reg.CloseKey;

			// if forcedialog is true then the user has indicated
                        // on the command line to always show the vm selection
                        // dialog
			if (not forcedialog) and (home <> '') then
                        begin
                                if testjdkpath(home, ver) then
                                begin
	                                LaunchBlueJ(home);
                                        Application.Terminate;
                                end;
			end;
                end;
        finally
                reg.Free;
        end;
end;

procedure TMainForm.FormDestroy(Sender: TObject);
begin
        goodvmsfound.Free;

        ff.Abort;
        ff.Free;
end;

procedure TMainForm.GoodVMSelectItem(Sender: TObject; Item: TListItem;
  Selected: Boolean);
begin
        LaunchButton.Enabled := selected;
end;

procedure TMainForm.FormShow(Sender: TObject);

var
        reg: TRegistry;
        subkeys : TStrings;
        i : integer;
        home, ver : string;
begin
	if Application.Terminated then
        	Exit;

        subkeys := TStringList.Create;
        reg := TRegistry.Create;

        try
                reg.RootKey := HKEY_LOCAL_MACHINE;
                if reg.OpenKey(jdkregkey, false) then
                begin
                        reg.GetKeyNames(subkeys);
                        reg.CloseKey;
                end;

                for i := 0 to subkeys.Count-1 do
                begin
                        if reg.OpenKeyReadOnly(jdkregkey + '\' + subkeys[i]) then
                        begin
                                home := reg.ReadString('JavaHome');

                                if home <> '' then
                                begin
                                        if testjdkpath(home, ver)  then
                                        begin
                                                AddGoodVM(home, ver);
                                        end;
                                end;
                                reg.CloseKey;
                        end;
                end;
        finally
                reg.Free;
        end;
end;

procedure TMainForm.GoodVMExit(Sender: TObject);
begin
        GoodVMCheck;
end;

procedure TMainForm.GoodVMCheck;
begin
        if GoodVM.ItemIndex = -1 then
                LaunchButton.Enabled := false;
end;

procedure TMainForm.BitBtn1Click(Sender: TObject);
begin
	if (BitBtn1.Caption = 'Simple') then
	begin
        	BitBtn1.Caption := 'Advanced';
		Height := 200;
        end
        else
        begin
        	BitBtn1.Caption := 'Simple';
                Height := 383;
        end;
end;

end.
