package com.tbd.forkfront;

public interface NH_Handler
{
	void lockMouse();
	void setCursorPos(int wid, int x, int y);
	void putString(int wid, int attr, String msg, int append, int color);
	void setHealthColor(int color);
	void redrawStatus();
	void rawPrint(int attr, String msg);
	void printTile(int wid, int x, int y, int tile, int ch, int col, int special);
	void ynFunction(String question, byte[] choices, int def);
	void getLine(String msg, int nMaxChars, boolean b);
	void createWindow(int wid, int type);
	void displayWindow(int wid, int bBlocking);
	void clearWindow(int wid, int isRogueLevel);
	void destroyWindow(int wid);
	void startMenu(int wid);
	void addMenu(int wid, int tile, int id, int acc, int groupAcc, int attr, String msg, int bSelected, int color);
	void endMenu(int wid, String msg);
	void selectMenu(int wid, int how);
	void cliparound(int x, int y, int playerX, int playerY);
	void showDPad();
	void hideDPad();
	void showLog(int bBlocking);
	void editOpts();
	void setLastUsername(String s);
	void setNumPadOption(boolean b);
	void askName(int nMaxChars, String[] saves);
	void loadSound(String filename);
	void playSound(String filename, int volume);
}
