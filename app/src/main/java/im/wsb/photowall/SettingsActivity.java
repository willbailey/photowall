package im.wsb.photowall;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.facebook.Session;
import com.facebook.UiLifecycleHelper;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SettingsActivity extends Activity {
  private Button mFacebookConnectButton;
  private UiLifecycleHelper mFacebookUiLifecycleHelper;
  private SharedPreferences mSharedPreferences;
  private Subscription mFbSessionSubscription;
  private Subscription mNumberOfColumnsSubscription;
  private Subscription mFlipIntervalSubscription;
  private Button mInterval1;
  private Button mInterval2;
  private Button mInterval3;
  private Button mInterval4;
  private View.OnClickListener mOnIntervalClick;
  private Button mCols1;
  private Button mCols2;
  private Button mCols3;
  private Button mCols4;
  private View.OnClickListener mOnColsClick;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSharedPreferences =  getSharedPreferences(
        Constants.PREFS_FILENAME, Context.MODE_PRIVATE);

    mFacebookUiLifecycleHelper = new UiLifecycleHelper(this, PhotoWallApplication.get());
    mFacebookUiLifecycleHelper.onCreate(savedInstanceState);

    setContentView(R.layout.settings);

    mFacebookConnectButton = (Button) findViewById(R.id.facebook_connect_button);
    mOnIntervalClick = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setSelectedInterval(v);
        String text = (String) ((Button) v).getText();
        String formatted = text.replace("s", "");
        float value = Float.valueOf(formatted);
        PhotoWallApplication.get().setFlipInterval(value);
      }
    };

    mInterval1 = (Button) findViewById(R.id.interval_1);
    mInterval2 = (Button) findViewById(R.id.interval_2);
    mInterval3 = (Button) findViewById(R.id.interval_3);
    mInterval4 = (Button) findViewById(R.id.interval_4);

    mInterval1.setOnClickListener(mOnIntervalClick);
    mInterval2.setOnClickListener(mOnIntervalClick);
    mInterval3.setOnClickListener(mOnIntervalClick);
    mInterval4.setOnClickListener(mOnIntervalClick);

    mOnColsClick = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setSelectedCols(v);
        String text = (String) ((Button) v).getText();
        int value = Integer.valueOf(text);
        PhotoWallApplication.get().setNumberOfColumns(value);
      }
    };

    mCols1 = (Button) findViewById(R.id.cols_1);
    mCols2 = (Button) findViewById(R.id.cols_2);
    mCols3 = (Button) findViewById(R.id.cols_3);
    mCols4 = (Button) findViewById(R.id.cols_4);

    mCols1.setOnClickListener(mOnColsClick);
    mCols2.setOnClickListener(mOnColsClick);
    mCols3.setOnClickListener(mOnColsClick);
    mCols4.setOnClickListener(mOnColsClick);

    mFacebookConnectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (Session.getActiveSession() != null && Session.getActiveSession().isOpened()) {
          disconnectFacebook();
        } else {
          connectFacebook();
        }
      }
    });
  }

  private void setSelectedInterval(View v) {
    mInterval1.setSelected(false);
    mInterval2.setSelected(false);
    mInterval3.setSelected(false);
    mInterval4.setSelected(false);
    v.setSelected(true);
  }

  private void setSelectedCols(View v) {
    mCols1.setSelected(false);
    mCols2.setSelected(false);
    mCols3.setSelected(false);
    mCols4.setSelected(false);
    v.setSelected(true);
  }

  private void connectFacebook() {
    List<String> permissions = new ArrayList<String>();
    permissions.add("user_friends");
    permissions.add("user_photos");
    Session.openActiveSession(this, true, permissions, PhotoWallApplication.get());
  }

  private void disconnectFacebook() {
    PhotoWallApplication.get().disconnectFacebook();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mFacebookUiLifecycleHelper.onResume();
    mFbSessionSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getFacebookSession())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Session>() {
              @Override
              public void call(Session session) {
                if (session == null || !session.isOpened()) {
//                  mFacebookConnectButton.setEnabled(true);
                  mFacebookConnectButton.setText("Connect Facebook");
                } else {
//                  mFacebookConnectButton.setEnabled(false);
                  mFacebookConnectButton.setText("Facebook Connected");
                }
              }
            });

    mNumberOfColumnsSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getNumberOfColumns())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Integer>() {
              @Override
              public void call(Integer columns) {
                switch (columns) {
                  case 5:
                    setSelectedCols(mCols1);
                    break;
                  case 8:
                    setSelectedCols(mCols2);
                    break;
                  case 11:
                    setSelectedCols(mCols3);
                    break;
                  case 30:
                    setSelectedCols(mCols4);
                    break;
                }
              }
            });

    mFlipIntervalSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getFlipInterval())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Float>() {
              @Override
              public void call(Float interval) {
                if (interval == 0.5f) {
                  setSelectedInterval(mInterval1);
                } else if (interval == 3f) {
                  setSelectedInterval(mInterval2);
                } else if (interval == 10f) {
                  setSelectedInterval(mInterval3);
                } else if (interval == 30f) {
                  setSelectedInterval(mInterval4);
                }
              }
            });

  }

  @Override
  protected void onPause() {
    super.onPause();
    mFacebookUiLifecycleHelper.onPause();
    mFbSessionSubscription.unsubscribe();
    mNumberOfColumnsSubscription.unsubscribe();
    mFlipIntervalSubscription.unsubscribe();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mFacebookUiLifecycleHelper.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mFacebookUiLifecycleHelper.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    mFacebookUiLifecycleHelper.onActivityResult(requestCode, resultCode, intent);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mFacebookUiLifecycleHelper.onSaveInstanceState(outState);
  }

  private float mapValueInRange(
      float value,
      float fromLow,
      float fromHigh,
      float toLow,
      float toHigh) {
    float fromRangeSize = fromHigh - fromLow;
    float toRangeSize = toHigh - toLow;
    float valueScale = (value - fromLow) / fromRangeSize;
    return toLow + (valueScale * toRangeSize);
  }


}
