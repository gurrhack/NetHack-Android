package com.tbd.forkfront;

import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

public class SoundPlayer {

	private SoundPool mSoundPool;
	private final Map<String, Integer> mSoundIds = new HashMap<>();;

	public SoundPlayer() {
	}

	public void load(String filename)
	{
		if(!mSoundIds.containsKey(filename))
		{
			if(mSoundPool == null)
				mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
			int soundId = mSoundPool.load(filename, 1);
			mSoundIds.put(filename, soundId);
		}
	}

	public void play(String filename, int volume)
	{
		Integer soundId = mSoundIds.get(filename);
		if(soundId == null || soundId == 0)
			return;
		float fVolume = Math.max(0.f, Math.min(1.f, volume / 100.f));
		mSoundPool.play(soundId, fVolume, fVolume, 1, 0, 1);
	}
}
