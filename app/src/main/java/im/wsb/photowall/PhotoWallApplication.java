package im.wsb.photowall;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

public class PhotoWallApplication extends Application implements Session.StatusCallback {
  private static PhotoWallApplication sInstance;

  private final BehaviorSubject<FriendResponse> mFriendResponseSubject = BehaviorSubject.create();
  private final BehaviorSubject<Session> mFacebookSessionSubject = BehaviorSubject.create();
  private final BehaviorSubject<Integer> mNumberOfColumns = BehaviorSubject.create();
  private final BehaviorSubject<Float> mFlipInterval = BehaviorSubject.create();
  private SharedPreferences mSharedPreferences;
  private Picasso mPicasso;

  public static PhotoWallApplication get() {
    return sInstance;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    sInstance = this;
    mPicasso = Picasso.with(this);

    mSharedPreferences = getSharedPreferences(Constants.PREFS_FILENAME, Context.MODE_PRIVATE);
    mFacebookSessionSubject.onNext(Session.openActiveSessionFromCache(this));

    loadFriendsFromCache();
    loadFriendsFromNetwork();

    mNumberOfColumns.onNext(mSharedPreferences.getInt("numberOfColumns", Constants.DEFAULT_COLS));
    mFlipInterval.onNext(
        mSharedPreferences.getFloat("flipInterval", Constants.DEFAULT_FLIP_INTERVAL));
  }

  public Observable<Session> getFacebookSession() {
    return mFacebookSessionSubject.asObservable();
  }

  public Observable<Boolean> getFacebookConnectionStatus() {
    return getFacebookSession().map(new Func1<Session, Boolean>() {
      @Override
      public Boolean call(Session session) {
        return session != null && session.isOpened();
      }
    });
  }

  public Observable<FriendResponse> getFriends() {
    return mFriendResponseSubject.asObservable();
  }

  public Observable<Integer> getNumberOfColumns() {
    return mNumberOfColumns.asObservable();
  }

  public void setNumberOfColumns(int columns) {
    mSharedPreferences.edit().putInt("numberOfColumns", columns).commit();
    mNumberOfColumns.onNext(columns);
  }

  public Observable<Float> getFlipInterval() {
    return mFlipInterval.asObservable();
  }

  public void setFlipInterval(float interval) {
    mSharedPreferences.edit().putFloat("flipInterval", interval).commit();
    mFlipInterval.onNext(interval);
  }

  private void loadFriendsFromCache() {
    String friendsJson = mSharedPreferences.getString("friendsResponse", "{}");
    FriendResponse friendResponse =
        new GsonBuilder().create().fromJson(friendsJson, FriendResponse.class);
    mFriendResponseSubject.onNext(friendResponse);
  }

  private void loadFriendsFromNetwork() {
    getFacebookSession().subscribe(
        new Action1<Session>() {
          @Override
          public void call(Session session) {
            Log.d("WSB", "loadFriendsFromNetwork");
            if (Session.getActiveSession() != null && Session.getActiveSession().isOpened()) {
              Log.d("WSB", "for Real");
              FacebookApi.get().getFriends().subscribe(new Action1<FriendResponse>() {
                @Override
                public void call(FriendResponse friendResponse) {
                  Log.d("WSB", "result:" + friendResponse.data.size());
                  String friendResponseJson = new GsonBuilder().create().toJson(friendResponse);
                  mSharedPreferences.edit().putString("friendsResponse", friendResponseJson);
                  mFriendResponseSubject.onNext(friendResponse);
                }
              });
            }
          }
        });
  }

  public void disconnectFacebook() {
    if (Session.getActiveSession() != null) {
      Session.getActiveSession().closeAndClearTokenInformation();
    }
    mSharedPreferences.edit().clear();
    mFacebookSessionSubject.onNext(Session.getActiveSession());
  }

  public void prefetch(String url) {
    mPicasso.load(url).fetch();
  }

  public void fetchInto(String url, Target target) {
    mPicasso.load(url).into(target);
  }

  @Override
  public void call(Session session, SessionState state, Exception exception) {
    mFacebookSessionSubject.onNext(session);
  }

}
