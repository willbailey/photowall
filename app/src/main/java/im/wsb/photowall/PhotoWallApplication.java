package im.wsb.photowall;

import android.app.Application;

public class PhotoWallApplication extends Application {
  private static PhotoWallApplication sInstance;

  public static PhotoWallApplication get() {
    return sInstance;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    sInstance = this;
  }
}
