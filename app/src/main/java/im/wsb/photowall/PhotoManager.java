package im.wsb.photowall;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by wbailey on 9/26/14.
 */
public class PhotoManager {

  private final Resources mResources;
  private final Bitmap mBitmap1;
  private final Bitmap mBitmap2;
  private final HashMap<String, Bitmap> mBitmaps = new HashMap<String, Bitmap>();

  public PhotoManager(Context context) {
    mResources = context.getResources();
    mBitmap1 = resourceToBitmap(R.drawable.wb);
    mBitmap2 = resourceToBitmap(R.drawable.p0);
  }

  public Bitmap getRandomBitmap(int size) {
    double rand = Math.random();
    String url = rand > 0.5 ? "0" : "1";
    String key = size + "-" + url;


    if (!mBitmaps.containsKey(key)) {
      Bitmap bitmap;
      if (url.equals("0")) {
        bitmap = mBitmap1;
      } else {
        bitmap = mBitmap2;
      }
      Bitmap scaledBitmap = bitmap.createScaledBitmap(bitmap, size, size, true);
      mBitmaps.put(key, scaledBitmap);
      bitmap.recycle();
    }

    return mBitmaps.get(key);
  }

  private Bitmap resourceToBitmap(int res) {
    Drawable drawable = mResources.getDrawable(res);
    Bitmap bitmap = Bitmap.createBitmap(
        drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(),
        Bitmap.Config.RGB_565);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }
}
