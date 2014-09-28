package im.wsb.photowall;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

public interface FacebookService {
  @GET("/me/friends")
  Observable<FriendResponse> getFriends(@Query("access_token") String accessToken);
}
