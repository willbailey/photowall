package im.wsb.photowall;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private Paint mPlaceHolderBgPaint;
    private Paint mPlaceHolderFgPaint;

    public PhotoWallEngine() {
      mRenderer = new PhotoWallRenderer(PhotoWallService.this);
      mPlaceHolderBgPaint = new Paint();
      mPlaceHolderBgPaint.setColor(Color.argb(255, 51, 181, 229));
      mPlaceHolderFgPaint = new Paint();
      mPlaceHolderFgPaint.setStyle(Paint.Style.FILL);
      mPlaceHolderFgPaint.setColor(Color.WHITE);
      mPlaceHolderFgPaint.setTextSize(48);
      mPlaceHolderFgPaint.setTextAlign(Paint.Align.CENTER);
      drawFrame();
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
        int interval;
        if (mFriendResponse == null || mFriendResponse.data.isEmpty()) {
          renderPlaceHolder(canvas);
          mHandler.postDelayed(mRunnable, 30);
          interval = 0;
        } else {
          interval = mRenderer.drawFrame(canvas);
        }
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

    private void renderPlaceHolder(Canvas canvas) {
      canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPlaceHolderBgPaint);
      canvas.drawText("Connect to Facebook in Settings", canvas.getWidth() / 2, canvas.getHeight() / 2, mPlaceHolderFgPaint);
    }

  }
}
