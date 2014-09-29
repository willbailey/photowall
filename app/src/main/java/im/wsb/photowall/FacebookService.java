package im.wsb.photowall;

import com.facebook.Response;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

public interface FacebookService {
  @GET("/me/friends")
  Observable<FriendResponse> getFriends(@Query("access_token") String accessToken);

  @GET("/v2.0/me/photos/uploaded?limit=500&fields=source")
  Response getUploadedPhotos(@Query("access_token") String accessToken);
}
