package com.tbd.forkfront;

public enum MenuSelectMode {
	PickNone, PickOne, PickMany;

	public static MenuSelectMode fromInt(int i) {
		if(i == 2)
			return PickMany;
		if(i == 1)
			return PickOne;
		return PickNone;
	}
}
