package im.wsb.photowall;

import android.content.Context;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FriendResponse {

  private static Picasso picasso = Picasso.with(PhotoWallApplication.get());

  public List<Friend> data = new ArrayList<Friend>();
  public Paging paging = new Paging();

  public Friend randomFriend() {
    int rand = new Random().nextInt(data.size());
    return data.get(rand);
  }

  public static class Paging {
    public String next;
  }

  public static class Friend {
    public String id;
    public String name;

    public Friend() {
    }

    public void getProfilePhoto(Context context, Target target) {
      PhotoWallApplication.get().fetchInto(getProfilePictureUrl(), target);
    }

    public String getProfilePictureUrl() {
      return "https://graph.facebook.com/" + id + "/picture?type=large";
    }
  }

}
