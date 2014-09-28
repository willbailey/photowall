package im.wsb.photowall;

import com.facebook.Session;
import com.facebook.SessionState;

import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class FacebookApi implements Session.StatusCallback {

  private static FacebookApi __instance = new FacebookApi();
  private final FacebookService mFacebookService;

  public static FacebookApi get() {
    return __instance;
  }

  private FacebookApi() {
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint("https://graph.facebook.com")
        .build();
    mFacebookService = restAdapter.create(FacebookService.class);
  }

  public Observable<FriendResponse> getFriends() {
    return mFacebookService.getFriends(Session.getActiveSession().getAccessToken())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  @Override
  public void call(Session session, SessionState state, Exception exception) {
  }
}
