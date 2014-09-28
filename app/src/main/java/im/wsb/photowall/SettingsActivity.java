package im.wsb.photowall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.gson.GsonBuilder;

import java.util.List;

import rx.functions.Action1;

public class SettingsActivity extends Activity implements Session.StatusCallback {
  private Button mFacebookConnectButton;
  private UiLifecycleHelper mFacebookUiLifecycleHelper;
  private SharedPreferences mSharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSharedPreferences =  getSharedPreferences(
        Constants.PREFS_FILENAME, Context.MODE_PRIVATE);

    mFacebookUiLifecycleHelper = new UiLifecycleHelper(this, this);
    mFacebookUiLifecycleHelper.onCreate(savedInstanceState);

    setContentView(R.layout.settings);
    mFacebookConnectButton = (Button) findViewById(R.id.facebook_connect_button);
    mFacebookConnectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        connectFacebook();
      }
    });
  }

  private void connectFacebook() {
    Session.openActiveSession(this, true, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mFacebookUiLifecycleHelper.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mFacebookUiLifecycleHelper.onPause();
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

  @Override
  public void call(Session session, SessionState state, Exception exception) {
    if (session.isOpened()) {
      FacebookApi.get().call(session, state, exception);
      FacebookApi.get().getFriends().subscribe(new Action1<FriendResponse>() {
        @Override
        public void call(FriendResponse friendResponse) {
          saveFriends(friendResponse);
          Log.d("WSB", "users1" + friendResponse);
        }
      }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
        }
      });
      mFacebookConnectButton.setEnabled(false);
      mFacebookConnectButton.setText("Facebook is Connected");
    } else {
      mFacebookConnectButton.setEnabled(true);
      mFacebookConnectButton.setText("Connect Facebook");
    }
  }

  private void saveFriends(FriendResponse friendResponse) {
    String json = new GsonBuilder().create().toJson(friendResponse);
    mSharedPreferences.edit().putString("friendsResponse", json).commit();
  }

}
