/**
 * 
 */
package com.android.phone.videophone;

import android.view.SurfaceHolder;

import com.android.internal.telephony.Phone;

/**
 * Observer  
 * @author hogyun.kim
 *
 */
public interface VTObserver {
	public boolean onUpdateVTState(Phone phone);	
	public void onFarSurfaceViewCreated(SurfaceHolder farVH);
	public void onNearSurfaceViewCreated(SurfaceHolder nearVH);
}
