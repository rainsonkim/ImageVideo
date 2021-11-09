# ImageVideo
自用  图片视频仿微信拍照拍视频

引用方式如下  

 maven { url "https://jitpack.io" }
 
 dependencies {
  implementation 'com.github.rainsonkim:ImageVideo:1.0.0'
 }
 
 
自己获取权限后引用

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAPTURE_AUDIO_OUTPUT,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };


if(Build.VERSION.SDK_INT >= 21){
            Intent intent = new Intent(this, VideoCameraActivity.class);
            startActivityForResult(intent, 1);
        }else {
            Intent intent = new Intent();
            intent.setClass(this, CaptureImageVideoActivity.class);
            startActivityForResult(intent, 1);
        }
        
