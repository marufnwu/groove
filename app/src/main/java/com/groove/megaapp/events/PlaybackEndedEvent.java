package com.groove.megaapp.events;

public class PlaybackEndedEvent {

    private final int mClip;

    public PlaybackEndedEvent(int clip) {
        mClip = clip;
    }

    public int getClip() {
        return mClip;
    }
}
