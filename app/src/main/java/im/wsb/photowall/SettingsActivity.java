package im.wsb.photowall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.facebook.Session;
import com.facebook.UiLifecycleHelper;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

public class SettingsActivity extends Activity {
  private Button mFacebookConnectButton;
  private UiLifecycleHelper mFacebookUiLifecycleHelper;
  private SharedPreferences mSharedPreferences;
  private Subscription mFbSessionSubscription;
  private SeekBar mFlipIntervalSeek;
  private SeekBar mNumberOfColumnSeek;
  private Subscription mNumberOfColumnsSubscription;
  private Subscription mFlipIntervalSubscription;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSharedPreferences =  getSharedPreferences(
        Constants.PREFS_FILENAME, Context.MODE_PRIVATE);

    mFacebookUiLifecycleHelper = new UiLifecycleHelper(this, PhotoWallApplication.get());
    mFacebookUiLifecycleHelper.onCreate(savedInstanceState);

    setContentView(R.layout.settings);

    mFacebookConnectButton = (Button) findViewById(R.id.facebook_connect_button);
    mFlipIntervalSeek = (SeekBar) findViewById(R.id.flip_interval_seek);
    mNumberOfColumnSeek = (SeekBar) findViewById(R.id.number_of_columns_seek);


    mFacebookConnectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        connectFacebook();
      }
    });

    mFlipIntervalSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        float interval = mapValueInRange(seekBar.getProgress(), 0, 100, 0, 10);
        PhotoWallApplication.get().setFlipInterval(interval);
      }
    });

    mNumberOfColumnSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        int columns = (int) mapValueInRange(seekBar.getProgress(), 0, 100, 3, 30);
        PhotoWallApplication.get().setNumberOfColumns(columns);
      }
    });
  }

  private void connectFacebook() {
    mFacebookConnectButton.setEnabled(false);
    Session.openActiveSession(this, true, PhotoWallApplication.get());
  }

  @Override
  protected void onResume() {
    super.onResume();
    mFacebookUiLifecycleHelper.onResume();
    mFbSessionSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getFacebookSession())
            .subscribe(new Action1<Session>() {
              @Override
              public void call(Session session) {
                if (session == null || !session.isOpened()) {
                  mFacebookConnectButton.setEnabled(true);
                  mFacebookConnectButton.setText("Connect to Facebook");
                } else {
                  mFacebookConnectButton.setEnabled(false);
                  mFacebookConnectButton.setText("Facebook is Connected");
                }
              }
            });

    mNumberOfColumnsSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getNumberOfColumns())
            .subscribe(new Action1<Integer>() {
              @Override
              public void call(Integer columns) {
                int columnsSeekPos = (int) mapValueInRange(columns, 3, 30, 0, 100);
                mNumberOfColumnSeek.setProgress(columnsSeekPos);
              }
            });

    mFlipIntervalSubscription =
        AndroidObservable.bindActivity(this, PhotoWallApplication.get().getFlipInterval())
            .subscribe(new Action1<Float>() {
              @Override
              public void call(Float interval) {
                int intervalSeekPos = (int) mapValueInRange(interval, 0, 10, 0, 100);
                mFlipIntervalSeek.setProgress(intervalSeekPos);
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
