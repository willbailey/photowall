package com.facebook;

/**
 * This is a hack around a limitation in Buck currently. Buck does not automatically generate the BuildConfig without
 * manually scripting. To get around this we are just referencing this HackBuildConfig instead inside of the Facebook
 * SDK.
 */
public abstract class HackBuildConfig {
  public static final boolean DEBUG = false;
}
