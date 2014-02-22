package com.example.autovol;

import edu.mit.media.funf.probe.Probe.Base;

public class AudioProbe extends Base {
    /**
     * Called when the probe switches from the enabled state to active
     * running state. This should be used to send any data broadcasts, but
     * must return quickly. If you have any long running processes they
     * should be started on a separate thread created by this method, or
     * should be divided into short runnables that are posted to this
     * threads looper one at a time, to allow for the probe to change state.
     */
	@Override
    protected void onStart() {
		super.onStart();

    }

    /**
     * Called with the probe switches from the running state to the enabled
     * state. This method should be used to stop any running threads
     * emitting data, or remove a runnable that has been posted to this
     * thread's looper. Any passive listeners should continue running.
     */
	@Override
    protected void onStop() {
		super.onStop();
    }
}
