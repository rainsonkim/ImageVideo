# ImageVideo
自用  图片视频仿微信拍照拍视频

引用方式如下  

 maven { url "https://jitpack.io" }
 
 dependencies {
  implementation 'com.github.rainsonkim:ImageVideo:1.0.5'
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
        
        
版本说明
        
1.0.5 Latest
增加图片大图显示自定义view，支持双指缩放、移动、双击放大缩小
1.0.4
修改图片质量、视频最大秒数、码率、帧率可灵活配置
1.0.3
适配安卓11存储路径
1.0.2
对图片进行压缩，原来16M压缩为三四百K
1.0.1
删除app图标，避免覆盖现有app
1.0.0
图片视频仿微信选择，升级到androidx 更新到最新android sdk 31
