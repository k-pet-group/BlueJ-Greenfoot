package bluej.editor.red;                	// This file forms part of the red package
 
/**
 ** @version $Id: UserFuncID.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **/
public abstract class UserFuncID
{
	// public constant variables
	public static final int UFNewLine = 1;
	public static final int UFOpenLine = 2;
	public static final int UFDeleteChar = 3;
	public static final int UFBackDeleteChar = 4;
	public static final int UFBackDeleteUntab = 5;
	public static final int UFTabToTabStop = 6;
	public static final int UFHalfTab = 7;
	public static final int UFIndent = 8;
	public static final int UFNewLineIndent = 9;
	public static final int UFCutWord = 10;
	public static final int UFCutToEOWord = 11;
	public static final int UFCutLine  = 12;
	public static final int UFCutToEOLine  = 13;
	public static final int UFCutRegion = 14;
	public static final int UFCut = 15;
	public static final int UFPaste = 16;
	public static final int UFSelectWord = 17;
	public static final int UFSelectLine = 18;
	public static final int UFSelectRegion = 19;
	public static final int UFSelectAll = 20; 
	public static final int UFShiftLeft = 21;
	public static final int UFShiftRight = 22;
	public static final int UFInsertFile = 23;
	public static final int UFInsertComment = 24;
	public static final int UFRemoveComment = 25;
	public static final int UF_Unused1 = 26;
	public static final int UF_Unused2 = 27;
	public static final int UFForwardChar = 28;
	public static final int UFBackwardChar = 29;
	public static final int UFForwardWord = 30;
	public static final int UFBackwardWord = 31;
	public static final int UFEndOfLine = 32;
	public static final int UFBegOfLine = 33;
	public static final int UFNextLine = 34;
	public static final int UFPrevLine = 35;
	public static final int UFScrollLineDown = 36;
	public static final int UFScrollLineUp = 37;
	public static final int UFScrollHPDown = 38;
	public static final int UFScrollHPUp = 39;
	public static final int UFPrevPage = 40;
	public static final int UFNextPage = 41;
	public static final int UFBegOfText = 42;
	public static final int UFEndOfText = 43;
	public static final int UFSwapCursorMark = 44;
	public static final int UFNextFlag = 45;
	public static final int UFNew = 46;
	public static final int UFOpen = 47;
	public static final int UFOpenSel = 48;
	public static final int UFSave = 49;
	public static final int UFSaveAs = 50;
	public static final int UFRevert = 51;
	public static final int UFClose = 52;
	public static final int UFPrint = 53;
	public static final int UFPreferences = 54;
	public static final int UFKeyBindings = 55;
	public static final int UFEditToolb = 56;
	public static final int UFSetFonts = 57;
	public static final int UFSetColours = 58;
	public static final int UFDescribeKey = 59;
	public static final int UFShowManual = 60;
	public static final int UFUndo = 61;
	public static final int UFFind = 62;
	public static final int UFFindBackward = 63;
	public static final int UFFindNext = 64;
	public static final int UFFindNextRev = 65;
	public static final int UFReplace = 66;
	public static final int UFSetMark = 67;
	public static final int UFGotoLine = 68;
	public static final int UFShowLine = 69;
	public static final int UFDefMacro = 70;
	public static final int UFEndMacro = 71;
	public static final int UFRunMacro = 72;
	public static final int UFInterface = 73;
	public static final int UFRedisplay = 74;
	public static final int UFBlueNewRout = 75;
	public static final int UFBlueExpand = 76;
	public static final int UFStatus = 77;
	public static final int UFCompile = 78;
	public static final int UF_UNUSED = 79;
	public static final int UFSetBreak = 80;
	public static final int UFClearBreak = 81;
	public static final int UFStep = 82;
	public static final int UFStepInto = 83;
	public static final int UFContinue = 84;
	public static final int UFTerminate = 85;
	public static final int UFSelfInsert = 86;
	// number of user functions
	public static final int NR_OF_USERFUNC = 87;
	public static final int NOT_BOUND = 88;
	public static final int UNKNOWN_KEY = 89;
} // end class UserFuncID
