unit main;

interface

{$I-}
uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, StdCtrls, FindFile, ComCtrls, ConsoleApp, ExFile, REgistry,
  Buttons, ImgList, ExtCtrls, jpeg;

const
        jdkregkey : string = '\Software\JavaSoft\Java Development Kit';
	ibmregkey : string = '\Software\IBM\Java Development Kit';
        bluejregkey : string = '\Software\BlueJ\BlueJ\1.3.0 beta 4';

        searchingstartcaption : string = 'Search drives for all Java versions...';
        searchingstopcaption : string = 'Stop Search';

        foundjavacaption1 : string = 'BlueJ has found more than one Java version that can be used.';
        foundjavacaption2 : string = 'Please select one and select "Launch" to use it with BlueJ or';
        foundjavacaption3 : string = 'select "Advanced" if you wish to search for other Java versions';

        foundonejavacaption1 : string = 'BlueJ has found the following Java version that can be used.';
        foundonejavacaption2 : string = 'Please select "Launch" if you wish to use it with BlueJ or';
        foundonejavacaption3 : string = 'select "Advanced" if you wish to look for other Java versions';

        nojavacaption1 : string = 'BlueJ could not find any Java systems. A JDK/J2SDK must be';
        nojavacaption2 : string = 'installed to run BlueJ. If one is installed on your system,';
        nojavacaption3 : string = 'select "Advanced" and then browse to its installation directory';

        simplecaption : string = 'Simple';
        advancedcaption : string = 'Advanced';

type
  TMainForm = class(TForm)
    LaunchButton: TButton;
    SearchButton: TButton;
    GoodVM: TListView;
    BadVM: TListView;
    Label1: TLabel;
    StartMessage1: TLabel;
    StatusBar: TStatusBar;
    BrowseButton: TButton;
    OpenDialog1: TOpenDialog;
    BitBtn1: TBitBtn;
    StartMessage2: TLabel;
    ImageList1: TImageList;
    StartMessage3: TLabel;
    Image1: TImage;
    procedure SearchButtonClick(Sender: TObject);
    procedure LaunchButtonClick(Sender: TObject);
    procedure BrowseButtonClick(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure GoodVMSelectItem(Sender: TObject; Item: TListItem;
      Selected: Boolean);
    procedure FormDestroy(Sender: TObject);
    procedure FormShow(Sender: TObject);
    procedure GoodVMExit(Sender: TObject);
    procedure BitBtn1ClickToAdvanced(Sender: TObject);
    procedure BitBtn1ClickToSimple(Sender: TObject);
  private
        ff : TFindFile;
        goodvmsfound : TStringList;
        goodparams : TStringList;

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

  forcedialog : boolean = false;
  usejavaw : boolean = false;
  simplewinheight : integer = 10;
  advancedwinheight : integer = 10;

implementation

uses
        javatest;

{$R *.dfm}

function DelphiIsRunning : boolean;
begin
	Result := DebugHook <> 0;
end;

procedure TMainForm.SearchButtonClick(Sender: TObject);

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

                SearchButton.Caption := searchingstopcaption;
        end;
end;

{ Update a message to show user that progress is being made through
  the folders on the computer }
procedure TMainForm.NewFolder(Sender: TObject; Folder: String;
                                var IgnoreFolder: Boolean);
begin
        StatusBar.SimpleText := 'Searching ' + Folder + '...';
        GoodVMCheck;
end;

{ When we find a file called java.exe, check if its a valid VM and
  add it to the list if it is }
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
        SearchButton.Caption := searchingstartcaption;

        GoodVMCheck;
end;

function TMainForm.LaunchBlueJ(jdkpath : string) : boolean;
var
        appdir, appdirlib, vmfilename, tooljarfilename,
          bluejjarfilename, editorjarfilename, extjarfilename,
          antlrjarfilename, junitjarfilename, mrjjarfilename : string;
        exfile : TExFile;
begin
        appdir := ExtractFilePath(Application.ExeName);

	// vmfilename is automatically wrapped in quotes by ExecConsoleApp so
	// there is no need for us to do it
        if usejavaw then
        	vmfilename := ExcludeTrailingPathDelimiter(jdkpath) + '\bin\javaw.exe'
        else
        	vmfilename := ExcludeTrailingPathDelimiter(jdkpath) + '\bin\java.exe';

        appdirlib := '"' + appdir + 'lib\';

        bluejjarfilename := appdirlib + 'bluej.jar' + '"';
        editorjarfilename := appdirlib + 'bluejeditor.jar' + '"';
        extjarfilename := appdirlib + 'bluejext.jar' + '"';
        antlrjarfilename := appdirlib + 'antlr.jar' + '"';
        junitjarfilename := appdirlib + 'junit.jar' + '"';
        mrjjarfilename := appdirlib + 'MRJ141Stubs.jar' + '"';

        tooljarfilename := '"' + ExcludeTrailingPathDelimiter(jdkpath) + '\lib\tools.jar' + '"';

        exfile := TExFile.Create(MainForm);

        exfile.WaitUntilDone := false;
        exfile.WindowType := wtMinimize;
        exfile.ProcFileName := vmfilename;
        exfile.ProcParameters := '-jar ' + bluejjarfilename + ' ' + goodparams.DelimitedText;
//                                              ';' +
//                                           editorjarfilename + ';' +
//                                           extjarfilename + ';' +
//                                           antlrjarfilename + ';' +
//                                           junitjarfilename + ';' +
//                                           mrjjarfilename + ';' +
//                                           tooljarfilename +
//                                        ' bluej.Main ' + goodparams.DelimitedText;
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

        if javapath = '' then
        	Exit;
                
	reason := '';

        if  testjavapath(javapath, reason) then
      	        AddGoodVM(javapath, reason)
        else
	        with BadVM.Items.Add do
                begin
                        Caption := javapath;
                        SubItems.Add(reason)
                end;

        GoodVMCheck;
end;

procedure TMainForm.FormCreate(Sender: TObject);
var
        reg: TRegistry;
        i : integer;
        home, mode, ver : string;
begin
	advancedwinheight := ClientHeight;
	simplewinheight := LaunchButton.Height + LaunchButton.Top + 8;

	BitBtn1ClickToSimple(Sender);

        ff := TFindFile.Create(MainForm);

        goodvmsfound := TStringList.Create;
        goodvmsfound.Duplicates := dupError;
        goodvmsfound.CaseSensitive := false;
        goodvmsfound.Sorted := true;

        goodparams := TStringList.Create;
        goodparams.Delimiter := ' ';
        goodparams.QuoteChar := '"';

        for i := 1 to ParamCount do
        begin
                if LowerCase(ParamStr(i)) = '/select' then
                        forcedialog := true
                else if LowerCase(ParamStr(i)) = '/javaw' then
                        usejavaw := true
                else
                	goodparams.Add(ParamStr(i));
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

	{ look for Sun JDK's }
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

        reg := TRegistry.Create;

	{ look for IBM JDK's }
        try
        	subkeys.Clear;

                reg.RootKey := HKEY_LOCAL_MACHINE;
                if reg.OpenKey(ibmregkey, false) then
                begin
                        reg.GetKeyNames(subkeys);
                        reg.CloseKey;
                end;

                for i := 0 to subkeys.Count-1 do
                begin
                        if reg.OpenKeyReadOnly(ibmregkey + '\' + subkeys[i]) then
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

	{ change opening message depending on how many VM's we find }
        if goodvmsfound.Count = 0 then
        begin
	        StartMessage1.Caption := nojavacaption1;
        	StartMessage2.Caption := nojavacaption2;
	        StartMessage3.Caption := nojavacaption3;
        end
        else if goodvmsfound.Count = 1 then
        begin
	        StartMessage1.Caption := foundonejavacaption1;
        	StartMessage2.Caption := foundonejavacaption2;
	        StartMessage3.Caption := foundonejavacaption3;

                GoodVM.ItemIndex := 0;
        end
        else
        begin
	        StartMessage1.Caption := foundjavacaption1;
        	StartMessage2.Caption := foundjavacaption2;
	        StartMessage3.Caption := foundjavacaption3;

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

procedure TMainForm.BitBtn1ClickToAdvanced(Sender: TObject);
begin
       	BitBtn1.Caption := simplecaption;
        BitBtn1.Glyph := nil;
	BitBtn1.OnClick := BitBtn1ClickToSimple;

        ImageList1.GetBitmap(0, BitBtn1.Glyph);

        ClientHeight := advancedwinheight;
end;

procedure TMainForm.BitBtn1ClickToSimple(Sender: TObject);
begin
       	BitBtn1.Caption := advancedcaption;
        BitBtn1.Glyph := nil;
	BitBtn1.OnClick := BitBtn1ClickToAdvanced;

        ImageList1.GetBitmap(1, BitBtn1.Glyph);

 	ClientHeight := simplewinheight;
end;

end.
