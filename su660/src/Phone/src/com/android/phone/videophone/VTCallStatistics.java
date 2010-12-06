/**
 * 
 */
package com.android.phone.videophone;

import android.os.SystemProperties;
import com.android.phone.videophone.VTCallStateInterface.*;


/**
 * VTCallStatistics
 * 
 * Implement VT Call statiistics 
 *  
 *
 */
public class VTCallStatistics 
{
	private static final String LOG_TAG = "VTCallStatistics";
	private static final boolean DBG =(SystemProperties.getInt("ro.debuggable", 0) == 1);	

    public static final int MAX_VALUE = 0x7FFFFFFF;

    int vtCallTotoal; 
    int vtMoCallCount; 
    int vtMtCallCount;
    int vtActiveCount;
    int vtCallSuccess; 
    int vtCallUserEnd; 
    int vtCallEnded; 




   /**
     * Constructor for VTCallStatistics()
     */ 
    public VTCallStatistics() {
        vtCallTotoal=0; 
        vtMoCallCount=0; 
        vtMtCallCount=0; 
        vtActiveCount=0;
        vtCallSuccess=0; 
        vtCallUserEnd=0; 
        vtCallEnded=0; 
    } 
   

    public void UpdatebyCallState(VTCallState vtCallState)
    {

		switch (vtCallState)
		{
    		case IDLE :
    			break;

    		case ACTIVE :
                vtActiveCount++;
                if (vtActiveCount == MAX_VALUE) vtActiveCount=0;
    			break;

    		case DIALING :
                vtMoCallCount++; 
                if (vtMoCallCount == MAX_VALUE) vtMoCallCount=0;
    			break;

    		case INCOMING :
                vtMtCallCount++;
                if (vtMtCallCount == MAX_VALUE) vtMtCallCount=0;
    			break;

    		case DISCONNECTED :
    			break;			
		}

        vtCallTotoal = vtMoCallCount + vtMtCallCount;
        if (vtCallTotoal == MAX_VALUE) vtCallTotoal=0;
        
    }
    
   
    public void doFirstVideoFrameReceived()
    {
        vtCallSuccess++;
        if (vtCallSuccess == MAX_VALUE) vtCallSuccess=0;
    }


    public void userEnd()
    {
        vtCallUserEnd++;
        if (vtCallUserEnd == MAX_VALUE) vtCallUserEnd=0;
    }


    public void callEnded()
    {
        vtCallEnded++;
        if (vtCallEnded == MAX_VALUE) vtCallEnded=0;
    }

    public String toString() 
    {
        StringBuilder str = new StringBuilder(256);

        str.append("-----------------------------------------------------------------------")
           .append("\nVTCallStatistics")
           .append("\n -----> MO(" +vtMoCallCount +")+MT("+ vtMtCallCount+")" +" = "+ vtCallTotoal)
           .append("\n -----> userEnd: "+ vtCallUserEnd + " CallEnded:  "+ vtCallEnded)
           .append("\n -----> connected: " + vtActiveCount + "  vtCallSuccess: " + vtCallSuccess + " ===> NegoFail=" + (vtActiveCount-vtCallSuccess))
           .append("\n------------------------------------------------------------------");
        return str.toString();        
    }


}


