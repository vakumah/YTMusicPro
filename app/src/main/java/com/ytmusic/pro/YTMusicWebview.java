package com.ytmusic.pro;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

/**
 * Custom WebView to maintain playback when visibility changes (Minimized/Locked)
 */
public class YTMusicWebview extends WebView {

    public YTMusicWebview(Context context) {
        super(context);
    }

    public YTMusicWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YTMusicWebview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        // Essential for background playback: 
        // We override the default behavior which normally pauses resources when hidden.
        // We only call super if we actually want to stop (which is rarely in a music app context)
        if (visibility != View.GONE && visibility != View.INVISIBLE) {
            super.onWindowVisibilityChanged(visibility);
        }
        // By skipping super.onWindowVisibilityChanged when invisible, 
        // the WebView continues to execute JS and play media.
    }
}
