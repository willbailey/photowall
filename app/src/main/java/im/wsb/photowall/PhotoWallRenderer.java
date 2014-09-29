package im.wsb.photowall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import rx.functions.Action1;

public class PhotoWallRenderer {

  private static int FLIP_INTERVAL_MS = 100;
  private static int COLS = 5;

  private final List<TileView> mTileList;
  private final List<TileView> mFlippingTiles;
  private final LinkedHashSet<TileView> mDirtyTiles;
  private final Random mRandom;
  private final List<List<TileView>> mTileMatrix;
  private final Matrix mIdentityMatrix;
  private final Rect mFlippingDirtyRect = new Rect(0, 0, 0, 0);
  private final Paint mClearPaint;
  private final Context mContext;
  private FriendResponse mFriendResponse;
  private long mLastFlipStartTs;
  private Bitmap mCachedBitmap;
  private int mDraws;
  private int mHeight = 0;
  private int mWidth = 0;

  public PhotoWallRenderer(Context context) {
    mContext = context;
    mTileList = new ArrayList<TileView>();
    mFlippingTiles = new ArrayList<TileView>();
    mDirtyTiles = new LinkedHashSet<TileView>();
    mRandom = new Random();
    mTileMatrix = new ArrayList<List<TileView>>();
    mIdentityMatrix = new Matrix();
    mClearPaint = new Paint();
    mClearPaint.setStyle(Paint.Style.FILL);
    mClearPaint.setColor(Color.BLACK);

    PhotoWallApplication.get().getNumberOfColumns().subscribe(new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        COLS = integer;
        onSizeChanged(mWidth, mHeight);
      }
    });

    PhotoWallApplication.get().getFlipInterval().subscribe(new Action1<Float>() {
      @Override
      public void call(Float interval) {
        FLIP_INTERVAL_MS = (int) (interval * 1000);
      }
    });
  }

  public void setFriendResponse(FriendResponse friendResponse) {
    Log.d("WSB", "setFriendResponse:" + friendResponse.data.size());
    mFriendResponse = friendResponse;
    onSizeChanged(mWidth, mHeight);
  }

  public void onSizeChanged(int width, int height) {
    mWidth = width;
    mHeight = height;
    mTileList.clear();
    mTileMatrix.clear();
    mDraws = 0;

    if (mFriendResponse == null || mFriendResponse.data.isEmpty()) {
      return;
    }

    int tileSize = width / COLS;
    for (int i = 0; i < width; i+=tileSize) {
      List<TileView> columnList = new ArrayList<TileView>();
      for (int j = 0; j < height; j+=tileSize) {
        TileView tileView = new TileView(mContext, mFriendResponse, i, j, tileSize, tileSize);
        columnList.add(tileView);
        mTileList.add(tileView);
      }
      mTileMatrix.add(columnList);
    }
    mDraws = 0;
  }

  public int drawFrame(Canvas canvas) {
    long ts = SystemClock.uptimeMillis();
    mFlippingTiles.clear();
    Picture picture = new Picture();
    Canvas tmpCanvas = picture.beginRecording(canvas.getWidth(), canvas.getHeight());

    canvas.setMatrix(mIdentityMatrix);
    if (ts - (FLIP_INTERVAL_MS + TileView.FLIP_ANIMATION_DURATION_MS) > mLastFlipStartTs) {
      mLastFlipStartTs = ts;
      flipTile();
    }
    determineFlippingTilesRect();
    drawBase(tmpCanvas);
    drawNeeded(tmpCanvas);
    drawFlippingTiles(tmpCanvas);

    picture.draw(canvas);

    long end = SystemClock.uptimeMillis();
    long duration = end - ts;
    if (mFlippingTiles.size() == 0) {
      if (mCachedBitmap != null) {
        mCachedBitmap.recycle();
      }
      mCachedBitmap = null;
      return FLIP_INTERVAL_MS;
    } else {
      return 0;
    }
  }
  
  private void determineFlippingTilesRect() {
    for (TileView tileView : mTileList) {
      if (tileView.isFlipping()) {
        mFlippingTiles.add(tileView);
      }
    }

    mDirtyTiles.clear();
    for (TileView tileView : mFlippingTiles) {
      appendDirtyTilesForFlippingTile(tileView);
    }
    mFlippingDirtyRect.set(0, 0, 0, 0);
    for (TileView tileView : mDirtyTiles) {
      mFlippingDirtyRect.union(tileView.getCoordinateRect());
    }
  }

  private void drawBase(Canvas canvas) {
    if (mDraws < 4) {
      mDraws++;
      for (TileView tileView : mTileList) {
        tileView.draw(canvas);
      }
    }
  }

  private void drawNeeded(Canvas canvas) {
    for (TileView tileView : mTileList) {
      if (tileView.needsDraw()) {
        tileView.draw(canvas);
      }
    }
  }

  private void drawFlippingTiles(Canvas canvas) {
    for (TileView tileView : mDirtyTiles) {
      tileView.draw(canvas);
    }
  }

  public void appendDirtyTilesForFlippingTile(TileView tileView) {
    int[] coords = tileView.getOrdinalCoords();
    safeAppendTileViewAtCoords(coords[0] - 1, coords[1]);     // north
    safeAppendTileViewAtCoords(coords[0] - 1, coords[1] + 1); // north-east
    safeAppendTileViewAtCoords(coords[0], coords[1] + 1);     // east
    safeAppendTileViewAtCoords(coords[0] + 1, coords[1] + 1); // south-east
    safeAppendTileViewAtCoords(coords[0] + 1, coords[1]);     // south
    safeAppendTileViewAtCoords(coords[0] + 1, coords[1] - 1); // south-west
    safeAppendTileViewAtCoords(coords[0], coords[1] - 1);     // west
    safeAppendTileViewAtCoords(coords[0] - 1, coords[1] - 1); // north-west
    safeAppendTileViewAtCoords(coords[0], coords[1]);         // flipping
  }

  private void safeAppendTileViewAtCoords(int row, int column) {
    if (row < 0 || column < 0) {
      return;
    }
    if (row > mTileMatrix.size() - 1) {
      return;
    }
    List<TileView> rowList = mTileMatrix.get(row);
    if (rowList != null) {
      if (column > rowList.size() - 1) {
        return;
      }
      TileView tileView = rowList.get(column);
      if (tileView != null) {
        if (!mDirtyTiles.contains(tileView)) {
          mDirtyTiles.add(tileView);
        }
      }
    }
  }

  private void flipTile() {
    TileView tileView = getRandomTile();
    tileView.startFlip();
  }

  private TileView getRandomTile() {
    int index = mRandom.nextInt(mTileList.size());
    return mTileList.get(index);
  }

}
