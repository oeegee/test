/**
 * 
 */
package com.android.phone.videophone;

import android.view.SurfaceHolder;

/**
 * When Call.State is changed, Call a callback to observer which attached. 
 * 
 * @author hogyun.kim
 *
 */
public interface VTObserable {
	/**
	 * add a VTObserver
	 * */
	public void attach(VTObserver o);

	/**
	 * remove a VTObserver
	 * */
	public void detach(VTObserver o);

	/**
	 * notifiy call&phone state to VTObservers
	 * */	
	public boolean notifyStateChanged();

	/**
	 * notify created surfaceView to VTObservers
	 * */
	public void notifyCreatedFarSurfaceView(SurfaceHolder far);
	public void notifyCreatedNearSurfaceView(SurfaceHolder far);
}
