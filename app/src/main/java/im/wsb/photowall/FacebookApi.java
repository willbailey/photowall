package im.wsb.photowall;

import com.facebook.Session;

import retrofit.RestAdapter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FacebookApi {

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

}
