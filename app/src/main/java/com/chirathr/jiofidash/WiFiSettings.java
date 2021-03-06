package com.chirathr.jiofidash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.chirathr.jiofidash.adapters.DeviceListAdapter;
import com.chirathr.jiofidash.data.DeviceViewModel;
import com.chirathr.jiofidash.data.JioFiData;
import com.chirathr.jiofidash.data.JioFiPreferences;
import com.chirathr.jiofidash.fragments.ChangeSSIDPasswordDialogFragment;
import com.chirathr.jiofidash.utils.NetworkUtils;
import com.chirathr.jiofidash.utils.VolleySingleton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WiFiSettings extends AppCompatActivity
        implements ChangeSSIDPasswordDialogFragment.OnChangeSSIDCompleteListener,
        DeviceListAdapter.OnClickListener {

    private static final String TAG = WiFiSettings.class.getSimpleName();
    private static int DELAY = 3000;

    private static boolean updateUi = true;

    private TextView wiFiSSIDTextView;
    private TextView wiFiPasswordTextView;
    private TextView wiFiDeviceCount;
    private ProgressBar loadingProgressBar;
    private ConstraintLayout wifiSettingsLayout;
    private ConstraintLayout wifiLayoutView;
    private ConstraintLayout devicesLayoutView;
    private Button changeSSIDPasswordButton;
    private ImageView showPasswordIcon;
    private boolean passwordShown = false;
    public static String SSID = null;
    public static String password = null;

    private List<DeviceViewModel> deviceViewModels;

    private RecyclerView mRecyclerView;
    private DeviceListAdapter mDeviceListAdapter;

    private static final String CSS_SELECTOR_BLOCKED_ITEM_LIST = "table[id='active_deny_list'] td[class='text_list']";
    private Snackbar noJioFiSnackBar;

    private Handler handler;
    private Runnable loadDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (updateUi) {
                loadDeviceList(WiFiSettings.this);
                loadDevicesData();
                handler.postDelayed(loadDataRunnable, DELAY);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("WiFi Settings");

        if (JioFiPreferences.ipAddressString == null) {
            JioFiPreferences.getInstance().loadWiFiIpAddress(this);
        }

        wiFiSSIDTextView = findViewById(R.id.tv_wifi_ssid);
        wiFiPasswordTextView = findViewById(R.id.tv_password);
        wiFiDeviceCount = findViewById(R.id.tv_device_count);
        loadingProgressBar = findViewById(R.id.progress_bar_wifi_settings);
        wifiLayoutView = findViewById(R.id.wifi_layout);
        devicesLayoutView = findViewById(R.id.devices_layout);
        wifiSettingsLayout = findViewById(R.id.wifi_settings_layout);
        changeSSIDPasswordButton = findViewById(R.id.button_change_ssid_password);
        showPasswordIcon = findViewById(R.id.show_password);

        noJioFiSnackBar = Snackbar.make(
                wifiSettingsLayout, "JioFi not found, check your WiFi.", Snackbar.LENGTH_INDEFINITE);

        mRecyclerView = findViewById(R.id.users_list_recyler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        mRecyclerView.setLayoutManager(layoutManager);

        handler = new Handler();
        showLoading();

        changeSSIDPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeSSIDPassDialog();
            }
        });

        showPasswordIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (passwordShown) {
                    // Hide the password and change icon
                    passwordShown = false;
                    wiFiPasswordTextView.setText(generateHiddenPassword(password));
                    showPasswordIcon.setImageResource(R.drawable.ic_round_visibility_24px);
                }
                else {
                    // Show password and change the icon
                    passwordShown = true;
                    wiFiPasswordTextView.setText(password);
                    showPasswordIcon.setImageResource(R.drawable.ic_round_visibility_off_24px);
                }
            }
        });

        updateUi = false;
    }

    private String generateHiddenPassword(String password) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            stringBuilder.append("*");
        }
        return stringBuilder.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeUIUpdateTasks();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUi = false;
    }

    // Load connected device list
    public void loadDeviceList(final Context context) {
        String urlString = NetworkUtils.getUrlString(NetworkUtils.LAN_INFO_ID);
        Log.v(TAG, urlString);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                urlString, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                deviceViewModels = new ArrayList<>();
                try {
                    wiFiDeviceCount.setText(response.getString(JioFiData.USER_COUNT));
                    String deviceListString = response.getString(JioFiData.USER_LIST);
                    String[] deviceList = deviceListString.split(";");

                    for (String deviceInfoString: deviceList) {
                        if (!deviceInfoString.contains("Static") && !deviceInfoString.contains("Dormant"))
                            deviceViewModels.add(new DeviceViewModel(deviceInfoString));
                    }

                    loadAndUpdateBlockedDevices(context);
                    hideJioFiNotFoundSnackBar();

                } catch (JSONException e) {
                    Log.v(TAG, "userlistinfo not found in json response or json error: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showJioFiNotFoundSnackBar();
            }
        });

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    // Load and Update Blocked device list, called by loadDeviceList.
    public void loadAndUpdateBlockedDevices(Context context) {

        String urlString = NetworkUtils.getUrlString(NetworkUtils.WIFI_MAC_GET_ID);
        Log.v(TAG, urlString);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlString,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Load the blocked List
                        Document wiFiMacDocument = Jsoup.parse(response);
                        Elements elementList = wiFiMacDocument.select(CSS_SELECTOR_BLOCKED_ITEM_LIST);

                        for (int i = 0; i < elementList.size(); i += 2) {
                            String macAddress = elementList.get(i).text().trim();
                            String name = elementList.get(i + 1).text().trim();
                            DeviceViewModel deviceViewModel = new DeviceViewModel(name, macAddress);

                            boolean isFound = false;
                            for (int j = 0; j < deviceViewModels.size(); ++j) {
                                if (deviceViewModels.get(j).getMacAddress().equals(deviceViewModel.getMacAddress())) {
                                    isFound = true;
                                    deviceViewModels.get(j).setIsBlocked(true);
                                }
                            }
                            if (!isFound) {
                                deviceViewModels.add(deviceViewModel);
                            }
                        }

                        Log.v(TAG, deviceViewModels.size() + "");

                        // Update the UI
                        if (updateUi) {
                            if (mDeviceListAdapter == null) {
                                mDeviceListAdapter = new DeviceListAdapter(WiFiSettings.this);
                                mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
                                mRecyclerView.setAdapter(mDeviceListAdapter);
                            } else {
                                mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + "loadAndUpdateBlockedDevices: " + error.getMessage());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return NetworkUtils.getAuthHeaders();
            }
        };

        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    public void loadDevicesData() {
        new LoadSSIDPasswordTask().execute();
    }

    // Load ssid and password
    private class LoadSSIDPasswordTask extends AsyncTask<Void, Void, Void> {

        private boolean isSuccessful;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isSuccessful = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            isSuccessful = NetworkUtils.loadCurrentSSIDAndPassword(WiFiSettings.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (isSuccessful) {
                SSID = NetworkUtils.wiFiSSID;
                wiFiSSIDTextView.setText(SSID);

                // handle password view button clicked
                password = NetworkUtils.wiFiPassword;
                if (passwordShown) {
                    wiFiPasswordTextView.setText(password);
                } else {
                    wiFiPasswordTextView.setText(generateHiddenPassword(password));
                }
                showData();
            } else {
                showLoading();
            }
        }
    }

    private void showLoading() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        wifiLayoutView.setAlpha(Float.parseFloat("0.2"));
        devicesLayoutView.setAlpha(Float.parseFloat("0.2"));
    }

    private void showData() {
        loadingProgressBar.setVisibility(View.INVISIBLE);
        wifiLayoutView.setAlpha(Float.parseFloat("1.0"));
        devicesLayoutView.setAlpha(Float.parseFloat("1.0"));
    }

    public void showChangeSSIDPassDialog() {
        DialogFragment changeSSIDPassFragment = new ChangeSSIDPasswordDialogFragment();
        changeSSIDPassFragment.show(
                getSupportFragmentManager(), ChangeSSIDPasswordDialogFragment.FRGAMENT_TAG);
    }

    // Listener called after SSID password changed
    @Override
    public void onChangeSSIDCompleteListener() {
        Snackbar.make(wifiSettingsLayout, "SSID and Password changed. Connect to the new WiFi.", Snackbar.LENGTH_LONG).show();
        // Restart WiFi as this will disconnect WiFi
        wiFiRestart();
    }

    // SSID password load failed listener
    @Override
    public void onLoadSSIDFailedListener() {
        Snackbar.make(wifiSettingsLayout, "Loading password failed!", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showChangeSSIDPassDialog();
                    }
                }).show();
    }

    private List<DeviceViewModel> blockedDeviceViewModelList = null;
    private DeviceViewModel tempDevice = null;
    private String blockDeviceSnackBarText = null;

    // Async task that blocks the devices in blockedDeviceViewModelList
    private class BlockDeviceAsyncTask extends AsyncTask<Void, Void, Void> {

        private boolean isSuccessful;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            isSuccessful = NetworkUtils.setBlockedDevices(WiFiSettings.this, blockedDeviceViewModelList);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (isSuccessful) {
                Snackbar.make(wifiSettingsLayout,
                        "Successfully " + blockDeviceSnackBarText + tempDevice.getDeviceName(),
                        Snackbar.LENGTH_LONG).show();
                tempDevice = null;
                blockedDeviceViewModelList = null;
            } else {
                Snackbar.make(wifiSettingsLayout,
                        "Failed to " + blockDeviceSnackBarText.replace("ed", "") + tempDevice.getDeviceName(),
                        Snackbar.LENGTH_LONG).setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new BlockDeviceAsyncTask().execute();
                            }
                        }).show();
            }
            handler.postDelayed(loadDataRunnable, DELAY);
            Snackbar.make(wifiSettingsLayout, "Waiting for WiFi to reconnect...", Snackbar.LENGTH_LONG).show();

            wiFiRestart();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    resumeUIUpdateTasks();
                }
            }, 2000);
        }
    }

    @Override
    public void onClickBlockListener(int itemId) {
        if (itemId >= deviceViewModels.size()) {
            return;
        }
        tempDevice = deviceViewModels.remove(itemId);
        Snackbar.make(wifiSettingsLayout, "Blocking " + tempDevice.getDeviceName(), Snackbar.LENGTH_LONG).show();
        blockedDeviceViewModelList = new ArrayList<>(deviceViewModels);


        blockDeviceSnackBarText = "blocked ";
        tempDevice.setIsBlocked(true);
        blockedDeviceViewModelList.add(tempDevice);
        mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
        new BlockDeviceAsyncTask().execute();
    }

    @Override
    public void onClickUnBlockListener(int itemId) {
        // Stop the UI from updating
        pauseUIUpdateTasks();

        if (itemId >= deviceViewModels.size()) {
            return;
        }
        tempDevice = deviceViewModels.remove(itemId);

        Snackbar.make(wifiSettingsLayout, "Unblocking " + tempDevice.getDeviceName(), Snackbar.LENGTH_LONG).show();

        blockedDeviceViewModelList = new ArrayList<>(deviceViewModels);
        mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
        blockDeviceSnackBarText = "unblocked ";

        new BlockDeviceAsyncTask().execute();
    }

    private void showJioFiNotFoundSnackBar() {
        if (!noJioFiSnackBar.isShown())
            noJioFiSnackBar.show();
    }

    private void hideJioFiNotFoundSnackBar() {
        if (noJioFiSnackBar.isShown())
            noJioFiSnackBar.dismiss();
    }

    // Function that restarts WiFi
    public void wiFiRestart() {
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    wiFiRestart();
                }
            }, 1000);
        }
        else {
            wifiManager.setWifiEnabled(true);
        }
    }

    // Functions that starts the loadDataRunnable
    public void resumeUIUpdateTasks() {
        if (!updateUi) {
            updateUi = true;
            handler.post(loadDataRunnable);
        }
    }

    public void pauseUIUpdateTasks() {
        updateUi = false;
        showLoading();
    }
}
