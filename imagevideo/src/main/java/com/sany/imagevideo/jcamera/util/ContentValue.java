package com.sany.imagevideo.jcamera.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class ContentValue {

	// 图片在SD卡中的缓存路径
//	public static final String PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "sany" + File.separator;

	public static String getRootPath(Context mContext){
		return mContext.getExternalCacheDir().getPath()+ File.separator + "sany" + File.separator;
	}
	public static String getImagePath(Context mContext){
		return getRootPath(mContext)+ "images" + File.separator;
	}
	public static String getVideoPath(Context mContext){
		return getRootPath(mContext)+ "video";
	}
	public static String getDownloadPath(Context mContext){
		return getRootPath(mContext)+ "download" + File.separator;
	}

//	public static final String IMAGE_PATH = PATH + "images" + File.separator;
//	public static final String DOWNLOAD_PATH = PATH + "download" + File.separator;
//	public static final String VIDEO_PATH = PATH + "video";

	public final static String FILE_START_NAME = "VID_";
	public final static String VIDEO_EXTENSION = ".mp4";
}