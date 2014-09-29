package im.wsb.photowall;

import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import rx.functions.Action1;

public class PhotoWallService extends WallpaperService {

  private static final String TAG = PhotoWallService.class.getSimpleName();
  private Handler mHandler = new Handler();
  private FriendResponse mFriendResponse;

  @Override
  public void onCreate() {
    super.onCreate();
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
    private PhotoWallRenderer mRenderer;

    public PhotoWallEngine() {
      mRenderer = new PhotoWallRenderer(PhotoWallService.this);
      PhotoWallApplication.get().getFriends().subscribe(new Action1<FriendResponse>() {
        @Override
        public void call(FriendResponse friendResponse) {
          Log.d("WSB", "update data:" + friendResponse.data.size());
          mFriendResponse = friendResponse;
          mRenderer.setFriendResponse(friendResponse);
          drawFrame();
        }
      });
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
      SurfaceHolder holder = getSurfaceHolder();
      Canvas canvas = holder.lockCanvas();
      if (canvas == null) {
        return;
      }
      try {
        if (mFriendResponse == null || mFriendResponse.data.isEmpty()) {
          renderPlaceHolder(canvas);
          mHandler.postDelayed(mRunnable, 30);
        } else {
          int interval = mRenderer.drawFrame(canvas);
          if (interval == 0) {
            mHandler.postDelayed(mRunnable, 30);
          } else {
            mHandler.postDelayed(mRunnable, interval);
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "fail", e);
      } finally {
        holder.unlockCanvasAndPost(canvas);
      }
    }

    private void renderPlaceHolder(Canvas canvas) {
      Log.d("WSB", "placeholder rendering");
    }

  }
}
