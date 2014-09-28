package im.wsb.photowall;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.facebook.Session;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class PhotoWallService extends WallpaperService {

  private static final String TAG = PhotoWallService.class.getSimpleName();
  private Handler mHandler = new Handler();
  private SharedPreferences mSharedPreferences;
  private PhotoWallRenderer mRenderer;
  private FriendResponse mFriendResponse;

  @Override
  public void onCreate() {
    super.onCreate();
    mRenderer = new PhotoWallRenderer(PhotoWallService.this);
    loadFriendsFromCache();
    Session session = Session.openActiveSessionFromCache(this);
    if (session == null || !session.isOpened()) {
      return;
    }
    FacebookApi.get().getFriends().subscribe(new Action1<FriendResponse>() {
      @Override
      public void call(FriendResponse friendResponse) {
      }
    });
  }

  private void loadFriendsFromCache() {
    mSharedPreferences = getSharedPreferences(Constants.PREFS_FILENAME, Context.MODE_PRIVATE);
    String friendsJson = mSharedPreferences.getString("friendsResponse", "{}");
    FriendResponse friendResponse =
        new GsonBuilder().create().fromJson(friendsJson, FriendResponse.class);
    Log.d("WSB", "friendResponse" + friendResponse);
    mFriendResponse = friendResponse;
    mRenderer.setFriendResponse(mFriendResponse);
  }

  @Override
  public Engine onCreateEngine() {
    return new PhotoWallEngine();
  }

  private class PhotoWallEngine extends Engine {

    private Runnable mRunnable = new Runnable() {
      @Override
      public void run() {
        drawFrame();
      }
    };

    public PhotoWallEngine() {
    }

    @Override
    public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
      super.onSurfaceRedrawNeeded(holder);
      drawFrame();
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      super.onSurfaceChanged(holder, format, width, height);
      mRenderer.onSizeChanged(width, height);
      drawFrame();
    }

    private void drawFrame() {
      if (mFriendResponse == null) {
        renderPlaceHolder();
        return;
      }
      SurfaceHolder holder = getSurfaceHolder();
      Canvas canvas = holder.lockCanvas();
      if (canvas == null) {
        return;
      }
      try {
        int interval = mRenderer.drawFrame(canvas);
        if (interval == 0) {
          mHandler.postDelayed(mRunnable, 30);
        } else {
          mHandler.postDelayed(mRunnable, interval);
        }
      } catch (Exception e) {
        Log.e(TAG, "fail", e);
      } finally {
        holder.unlockCanvasAndPost(canvas);
      }
    }

    private void renderPlaceHolder() {

    }

  }
}
