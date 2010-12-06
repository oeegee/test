package com.mtelo.visualexpression;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import android.os.StatFs;


public class MyFile
{
	public static final String 	AUTHORITY_DIR = "chmod 755 ";
	public static final String 	AUTHORITY_FILE = "chmod 666 ";
	
	/**
	 * 디렉토리를 생성 한다.
	 * @param DirName		디렉토리 명
	 * @param authority		디렉토리 권한
	 */
	public static boolean make_Directory(String DirName, String authority)
	{
		boolean result = true;
		
		File fDir = new File(DirName);
		
		if (fDir == null)
			result = false;
    	
    	if (fDir.exists() == false) // 디렉토리가 존재 하지 않는다. 
    	{
			boolean nRet = new File(DirName).mkdir();
			
			if (nRet == true) 
			{
				result = set_DirAuthority(DirName, authority);
			}
			else
			{
				result = false;
			}
		}
    	
    	fDir = null;
    	return result;
	}
	
	/**
	 * 디렉토리 권한 변경 함수.
	 * @param DirName		디렉토리 명
	 * @param authority		디렉토리 권한
	 */
	public static boolean set_DirAuthority(String DirName, String authority)
    {
		return set_Authority(DirName, authority);
    }

	public static boolean set_FileAuthority(String FileName, String authority)
    {
		return set_Authority(FileName, authority);
    }
	
	private static boolean set_Authority(String Name, String authority)
	{
		boolean result = true;
		
		try 
		{
			Runtime.getRuntime().exec(authority + Name);
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		
		return result;
	}
	
	

	
	// 내부 여유 메모리 크기를 반환 한다. 
	public static long get_InternalFreeMemory()
	{
		File fp = Environment.getDataDirectory();
		StatFs stfs = new StatFs(fp.getPath());
		
		long blocksize = stfs.getBlockSize();
		long blockcnt = stfs.getBlockCount();
		
		return blocksize * blockcnt;
	}
	
	
	
}
