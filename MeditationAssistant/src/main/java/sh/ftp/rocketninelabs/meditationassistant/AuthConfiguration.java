package sh.ftp.rocketninelabs.meditationassistant;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.connectivity.DefaultConnectionBuilder;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Reads and validates the demo app configuration from `res/raw/auth_config.json`. Configuration
 * changes are detected by comparing the hash of the last known configuration to the read
 * configuration. When a configuration change is detected, the app state is reset.
 */
public final class AuthConfiguration {

    private static final String TAG = "Configuration";

    private static final String PREFS_NAME = "config";
    private static final String KEY_LAST_HASH = "lastHash";

    private static WeakReference<AuthConfiguration> sInstance = new WeakReference<>(null);

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final Resources mResources;

    private JSONObject mConfigJson;
    private String mConfigError;

    private String mClientId;
    private String mScope;
    private Uri mRedirectUri;
    private Uri mDiscoveryUri;
    private Uri mAuthEndpointUri;
    private Uri mTokenEndpointUri;
    private Uri mRegistrationEndpointUri;

    public static AuthConfiguration getInstance(Context context) {
        AuthConfiguration config = sInstance.get();
        if (config == null) {
            config = new AuthConfiguration(context);
            sInstance = new WeakReference<AuthConfiguration>(config);
        }

        return config;
    }

    public AuthConfiguration(Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mResources = context.getResources();

        try {
            readConfiguration();
        } catch (InvalidConfigurationException ex) {
            mConfigError = ex.getMessage();
        }
    }

    /**
     * Indicates whether the current configuration is valid.
     */
    public boolean isValid() {
        return mConfigError == null;
    }

    /**
     * Returns a description of the configuration error, if the configuration is invalid.
     */
    @Nullable
    public String getConfigurationError() {
        return mConfigError;
    }


    @Nullable
    public String getClientId() {
        return mClientId;
    }

    @NonNull
    public String getScope() {
        return mScope;
    }

    @NonNull
    public Uri getRedirectUri() {
        return mRedirectUri;
    }

    @Nullable
    public Uri getDiscoveryUri() {
        return mDiscoveryUri;
    }

    @Nullable
    public Uri getAuthEndpointUri() {
        return mAuthEndpointUri;
    }

    @Nullable
    public Uri getTokenEndpointUri() {
        return mTokenEndpointUri;
    }

    @Nullable
    public Uri getRegistrationEndpointUri() {
        return mRegistrationEndpointUri;
    }

    public ConnectionBuilder getConnectionBuilder() {
        return DefaultConnectionBuilder.INSTANCE;
    }

    private void readConfiguration() throws InvalidConfigurationException {
        mClientId = "";
        mScope = "openid email profile";
        mRedirectUri = Uri.parse(MeditationAssistant.URL_MEDINET + "/oauth");

        if (!isRedirectUriRegistered()) {
            throw new InvalidConfigurationException(
                    "redirect_uri is not handled by any activity in this app! "
                            + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                            + "is correctly configured, or that an appropriate intent filter "
                            + "exists in your app manifest.");
        }

        /*if (getConfigString("discovery_uri") == null) {
            mAuthEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri");

            mTokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri");

            if (mClientId == null) {
                mRegistrationEndpointUri = getRequiredConfigWebUri("registration_endpoint_uri");
            }
        } else {
            mDiscoveryUri = getRequiredConfigWebUri("discovery_uri");
        }*/
        mDiscoveryUri = Uri.parse("");
    }

    private boolean isRedirectUriRegistered() {
        // ensure that the redirect URI declared in the configuration is handled by some activity
        // in the app, by querying the package manager speculatively
        Intent redirectIntent = new Intent();
        redirectIntent.setPackage(mContext.getPackageName());
        redirectIntent.setAction(Intent.ACTION_VIEW);
        redirectIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        redirectIntent.setData(mRedirectUri);

        return !mContext.getPackageManager().queryIntentActivities(redirectIntent, 0).isEmpty();
    }

    public static final class InvalidConfigurationException extends Exception {
        InvalidConfigurationException(String reason) {
            super(reason);
        }

        InvalidConfigurationException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
}
