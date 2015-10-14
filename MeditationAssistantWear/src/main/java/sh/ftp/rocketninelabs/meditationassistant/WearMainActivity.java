package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class WearMainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private TextView wearTimer;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                wearTimer = (TextView) stub.findViewById(R.id.wearTimer);

                wearTimer.setText("0:15");
                wearTimer.setTextSize(33 * getResources().getDisplayMetrics().scaledDensity);

                wearTimer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(WearMainActivity.this, WearEditDurationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivityForResult(intent, 1);
                    }
                });
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MeditationAssistant", "WEAR - Connection status: " + String.valueOf(mGoogleApiClient.isConnected()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        WearMeditationAssistant wma = (WearMeditationAssistant) getApplication();

        if (wma.newDuration != "") {
            wearTimer.setText(wma.newDuration);
            wma.newDuration = "";
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("MeditationAssistant", "WEAR - Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MeditationAssistant", "WEAR - Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("MeditationAssistant", "WEAR - Failed");
    }
}
