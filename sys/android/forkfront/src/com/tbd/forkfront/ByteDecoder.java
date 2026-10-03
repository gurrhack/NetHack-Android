package com.tbd.forkfront;

public interface ByteDecoder
{
	char decode(int b);
	String decode(byte[] bytes);
}
