package com.tbd.NetHack;

import java.util.EnumSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class Input
{
	public enum Modifier
	{
		Shift,
		Control,
		Meta
	}
	
	// ____________________________________________________________________________________
	public static EnumSet<Modifier> modifiersFromKeyEvent(KeyEvent event)
	{
		EnumSet<Modifier> mod = EnumSet.noneOf(Modifier.class);
		
		// The ALT key is used for special characters (ike '{', '\', and '>') in the emulator
		// so I don't dare use it as a modifier for meta keys ¤
		//if(event.isAltPressed())
		//	mod.add(Modifiers.Meta);
		
		// The CTRL key is not supported until API level 11 which I can't require yet
		//if(event.isCtrlPressed())
		//	mod.add(Modifiers.Control);
		
		if(event.isShiftPressed())
			mod.add(Modifier.Shift);
		
		return mod;
	}
	
	public static int nhKeyFromMod(char ch, EnumSet<Modifier> modifiers)
	{
		if(modifiers.contains(Input.Modifier.Meta))
			return ch | 0x80;
		if(modifiers.contains(Input.Modifier.Control))
			return ch & 0x1f;
		if(modifiers.contains(Input.Modifier.Shift))
			return Character.toUpperCase(ch);
		return ch;
	}
	
	// ____________________________________________________________________________________
	public static int nhKeyFromKeyCode(int keyCode, char ch, EnumSet<Modifier> mod)
	{
		int nhKey;
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			nhKey = 0x80;
		break;
		case KeyEvent.KEYCODE_SPACE:
			nhKey = ' ';
		break;
		case 111: // KeyEvent.KEYCODE_ESCAPE:
			nhKey = '\033';
		break;
		case KeyEvent.KEYCODE_DEL:
			nhKey = 0x7f;
		break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			nhKey = 'h';
		break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			nhKey = 'l';
		break;
		case KeyEvent.KEYCODE_DPAD_UP:
			nhKey = 'k';
		break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			nhKey = 'j';
		break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			nhKey = '.';
		break;
		default:
			nhKey = ch;
			if(nhKey != 0)
			{
				if(mod.contains(Modifier.Shift))
					nhKey = Character.toUpperCase(nhKey);
				// don't apply these to the special hardware keys above
				if(mod.contains(Modifier.Meta))
					nhKey = 0x80 | Character.toLowerCase(nhKey);
				else if(mod.contains(Modifier.Control))
					nhKey = 0x1f & Character.toLowerCase(nhKey);
			}
			return nhKey;
		}
		
		if(nhKey != 0 && nhKey != 0x80 && mod.contains(Modifier.Shift))
			nhKey = Character.toUpperCase(nhKey);
		
		return nhKey;
	}

	public static EnumSet<Modifier> modifiers()
	{
		return EnumSet.noneOf(Modifier.class);
	}

	public static int keyCodeToAction(int keyCode, Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if(prefs == null)
			return keyCode;
	
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_VOLUME_UP: return Util.parseInt(prefs.getString("volup", ""), KeyAction.ZoomIn);		
		case KeyEvent.KEYCODE_VOLUME_DOWN: return Util.parseInt(prefs.getString("voldown", ""), KeyAction.ZoomOut);
		case /*KeyEvent.KEYCODE_CTRL_LEFT*/ 113: return KeyAction.Control;
		case /*KeyEvent.KEYCODE_CTRL_RIGHT*/ 114: return KeyAction.Control;
		case /*KeyEvent.KEYCODE_ZOOM_IN*/ 168: return KeyAction.ZoomIn;
		case /*KeyEvent.KEYCODE_ZOOM_OUT*/ 169: return KeyAction.ZoomOut;
		}

		return keyCode;
	}
	
	public static int toKeyCode(char primaryCode)
	{
		primaryCode = Character.toUpperCase(primaryCode);
		switch(primaryCode)
		{
		case '0': return KeyEvent.KEYCODE_0;
		case '1': return KeyEvent.KEYCODE_1;
		case '2': return KeyEvent.KEYCODE_2;
		case '3': return KeyEvent.KEYCODE_3;
		case '4': return KeyEvent.KEYCODE_4;
		case '5': return KeyEvent.KEYCODE_5;
		case '6': return KeyEvent.KEYCODE_6;
		case '7': return KeyEvent.KEYCODE_7;
		case '8': return KeyEvent.KEYCODE_8;
		case '9': return KeyEvent.KEYCODE_9;
		case 'A': return KeyEvent.KEYCODE_A;
		case '\'': return KeyEvent.KEYCODE_APOSTROPHE;
		case '@': return KeyEvent.KEYCODE_AT;
		case 'B': return KeyEvent.KEYCODE_B;
		case '\\': return KeyEvent.KEYCODE_BACKSLASH;
		case 'C': return KeyEvent.KEYCODE_C;
		case ',': return KeyEvent.KEYCODE_COMMA;
		case 'D': return KeyEvent.KEYCODE_D;
		case 'E': return KeyEvent.KEYCODE_E;
		case '=': return KeyEvent.KEYCODE_EQUALS;
		case 'F': return KeyEvent.KEYCODE_F;
		case 'G': return KeyEvent.KEYCODE_G;
		case '`': return KeyEvent.KEYCODE_GRAVE;
		case 'H': return KeyEvent.KEYCODE_H;
		case 'I': return KeyEvent.KEYCODE_I;
		case 'J': return KeyEvent.KEYCODE_J;
		case 'K': return KeyEvent.KEYCODE_K;
		case 'L': return KeyEvent.KEYCODE_L;
		case '[': return KeyEvent.KEYCODE_LEFT_BRACKET;
		case 'M': return KeyEvent.KEYCODE_M;
		case '-': return KeyEvent.KEYCODE_MINUS;
		case 'N': return KeyEvent.KEYCODE_N;
		case 'O': return KeyEvent.KEYCODE_O;
		case 'P': return KeyEvent.KEYCODE_P;
		case '.': return KeyEvent.KEYCODE_PERIOD;
		case '+': return KeyEvent.KEYCODE_PLUS;
		case 'Q': return KeyEvent.KEYCODE_Q;
		case 'R': return KeyEvent.KEYCODE_R;
		case ']': return KeyEvent.KEYCODE_RIGHT_BRACKET;
		case 'S': return KeyEvent.KEYCODE_S;
		case ';': return KeyEvent.KEYCODE_SEMICOLON;
		case '/': return KeyEvent.KEYCODE_SLASH;
		case ' ': return KeyEvent.KEYCODE_SPACE;
		case '*': return KeyEvent.KEYCODE_STAR;
		case 'T': return KeyEvent.KEYCODE_T;
		case 'U': return KeyEvent.KEYCODE_U;
		case 'V': return KeyEvent.KEYCODE_V;
		case 'W': return KeyEvent.KEYCODE_W;
		case 'X': return KeyEvent.KEYCODE_X;
		case 'Y': return KeyEvent.KEYCODE_Y;
		case 'Z': return KeyEvent.KEYCODE_Z;
		case 10: return KeyEvent.KEYCODE_ENTER;
		default: return 0;
		}
	}
}
