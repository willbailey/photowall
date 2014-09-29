package im.wsb.photowall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Random;

public class TileView {

  public static long FLIP_ANIMATION_DURATION_MS = 1000;
  private Target mBackTarget;
  private Target mFrontTarget;
  private int mNeedsDraw;

  public enum Side {
    FRONT,
    BACK
  }

  private final FriendResponse mFriendResponse;
  private final Context mContext;
  private final int mX;
  private final int mY;
  private final int mWidth;
  private final int mHeight;
  private final Paint mFrontPaint;
  private final Paint mBackPaint;
  private final Matrix mLayoutMatrix;
  private final Matrix mFrontPaintMatrix;
  private final Matrix mBackPaintMatrix;
  private final Camera mCamera;
  private final float mPivotX;
  private final float mPivotY;
  private final RectF mBitmapRect;
  private final RectF mLayoutRect;
  private final Paint mClearPaint;
  private final int[] mOrdinalCoords;
  private final Rect mCoordinateRect;

  private boolean mFlipping;
  private long mStartFlippingAt;
  private Paint mCurrentPaint;
  private boolean mFlippedDrawnSide;
  private int mLastFlipDegreesEnd = 0;
  private Side mCurrentSide;
  private boolean mHasBitmap;
  private boolean mNeedsLastDraw;

  public TileView(Context context, FriendResponse friendResponse, int x, int y, int width, int height) {
    mContext = context;
    mFriendResponse = friendResponse;
    mX = x;
    mY = y;
    mWidth = width;
    mHeight = height;
    mClearPaint = new Paint();
    mFrontPaint = new Paint();
    mBackPaint = new Paint();
    mLayoutMatrix = new Matrix();
    mFrontPaintMatrix = new Matrix();
    mBackPaintMatrix = new Matrix();
    mBitmapRect = new RectF();
    mLayoutRect = new RectF(0, 0, mWidth, mHeight);
    mCoordinateRect = new Rect(mX, mY, mX + mWidth, mY + mHeight);
    mCamera = new Camera();
    mPivotX = mWidth / 2f;
    mPivotY = mHeight / 2f;

    mClearPaint.setColor(Color.BLACK);
    mFrontPaint.setColor(Color.BLACK);
    mBackPaint.setColor(Color.BLACK);

    mFrontPaint.setColorFilter(new LightingColorFilter(0xFF999999, 0x00000000));
    mBackPaint.setColorFilter(new LightingColorFilter(0xFF999999, 0x00000000));

    setFriendForSide(mFriendResponse.randomFriend(), Side.FRONT);
    setFriendForSide(mFriendResponse.randomFriend(), Side.BACK);

    mCurrentSide = Side.FRONT;
    mCurrentPaint = mFrontPaint;
    mOrdinalCoords = new int[]{mX / mWidth, mY / mHeight};
  }

  public Rect getCoordinateRect() {
    return mCoordinateRect;
  }

  public int[] getOrdinalCoords() {
    return mOrdinalCoords;
  }

  public boolean hasBitmap() {
    return mHasBitmap;
  }

  private void setFriendForSide(FriendResponse.Friend friend, final Side side) {
    Target target = new Target() {
      @Override
      public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        setBitmapForSide(bitmap, side);
        mNeedsDraw = 5;
      }

      @Override
      public void onBitmapFailed(Drawable errorDrawable) {
        Log.e("WSB", "error onBitmapFailed");
      }

      @Override
      public void onPrepareLoad(Drawable placeHolderDrawable) {
      }
    };
    if (side == Side.BACK) {
      mBackTarget = target;
    } else {
      mFrontTarget = target;
    }
    friend.getProfilePhoto(mContext, target);
  }

  public TileView setBitmapForSide(Bitmap bitmap, Side side) {
    if (bitmap == null) {
      return this;
    }
    mHasBitmap = true;
    // TODO: need to rotate 180 for backside or not reset the rotation degrees
    Paint paint = side == Side.FRONT ? mFrontPaint : mBackPaint;
    Matrix matrix = side == Side.FRONT ? mFrontPaintMatrix : mBackPaintMatrix;
    mBitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
    matrix.setRectToRect(mBitmapRect, mLayoutRect, Matrix.ScaleToFit.CENTER);
    matrix.mapRect(mBitmapRect);
    float widthAspect = mBitmapRect.width() / mLayoutRect.width();
    float heightAspect = mBitmapRect.height() / mLayoutRect.height();
    float smallerAspect = widthAspect < heightAspect ? widthAspect : heightAspect;
    float scaleFactor = 1 / smallerAspect;
    matrix.postScale(scaleFactor, scaleFactor, mPivotX, mPivotY);
    BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    bitmapShader.setLocalMatrix(matrix);
    paint.setShader(bitmapShader);
    return this;
  }

  public boolean isFlipping() {
    return mFlipping;
  }

  public void startFlip() {
    mStartFlippingAt = getCurrentTime();
    mFlipping = true;
    mFlippedDrawnSide = false;
  }

  public void endFlip() {
    mStartFlippingAt = 0;
    mFlipping = false;
    Side sideToSet = mCurrentSide;
    mCurrentSide = mCurrentSide == Side.FRONT ? Side.BACK : Side.FRONT;
    setFriendForSide(mFriendResponse.randomFriend(), sideToSet);
  }

  public boolean needsDraw() {
    return mNeedsDraw > 0;
  }

  private long getCurrentTime() {
    return SystemClock.uptimeMillis();
  }

  public int getRotationDegrees() {
    long currentTime = getCurrentTime();
    float flipRatio = (currentTime - mStartFlippingAt) / (float) FLIP_ANIMATION_DURATION_MS;
    flipRatio = Math.min(Math.max(flipRatio, 0), 1);
    flipRatio = (float)(Math.cos((flipRatio + 1) * Math.PI) / 2.0f) + 0.5f;
    int flipDegrees = Math.round(flipRatio * 180);
    return flipDegrees;
  }

  public TileView draw(Canvas canvas) {
    mNeedsDraw--;
    mLayoutMatrix.reset();
    canvas.save();

    int rotationDegrees = mLastFlipDegreesEnd;
    if (isFlipping()) {
      rotationDegrees += getRotationDegrees();
      if (rotationDegrees % 180 >= 90 && !mFlippedDrawnSide) {
        if (mCurrentPaint == mFrontPaint) {
          mCurrentPaint = mBackPaint;
        } else {
          mCurrentPaint = mFrontPaint;
        }
        mFlippedDrawnSide = true;
      }
    }

    // Clear the drawing space for this tile.
    mLayoutMatrix.postTranslate(mX, mY);
    canvas.setMatrix(mLayoutMatrix);
    canvas.drawRect(0, 0, mWidth, mHeight, mClearPaint);

    mCamera.save();
    mCamera.setLocation(0, 0, -8);
    mCamera.rotateY(rotationDegrees);
    mCamera.getMatrix(mLayoutMatrix);
    mCamera.restore();
    mLayoutMatrix.preTranslate(-mPivotX, -mPivotY);
    mLayoutMatrix.postTranslate(mPivotX, mPivotY);

    if (isFlipping()) {
      if (getCurrentTime() - mStartFlippingAt > FLIP_ANIMATION_DURATION_MS) {
        if (mNeedsLastDraw) {
          endFlip();
          mLastFlipDegreesEnd = rotationDegrees;
          mNeedsLastDraw = false;
        } else {
          mNeedsLastDraw = true;
        }
      }
    }


    mLayoutMatrix.postTranslate(mX, mY);
    canvas.setMatrix(mLayoutMatrix);
    canvas.drawRect(0, 0, mWidth, mHeight, mCurrentPaint);
    canvas.restore();

    return this;
  }
}
