package com.chirathr.jiofidash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.chirathr.jiofidash.adapters.DeviceListAdapter;
import com.chirathr.jiofidash.data.DeviceViewModel;
import com.chirathr.jiofidash.data.JioFiData;
import com.chirathr.jiofidash.utils.NetworkUtils;
import com.chirathr.jiofidash.utils.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class WiFiSettings extends AppCompatActivity {

    private static final String TAG = WiFiSettings.class.getSimpleName();
    private static int DELAY_SSID = 5000;
    private static int DELAY = 1000;

    private TextView wiFiSSIDTextView;
    private TextView wiFiDeviceCount;
    private Button changeSSIDPasswordButton;
    private List<DeviceViewModel> deviceViewModels;

    private RecyclerView mRecyclerView;
    private DeviceListAdapter mDeviceListAdapter;

    private Handler handler;
    private Runnable loadDeviceListRunnable = new Runnable() {
        @Override
        public void run() {
            loadDeviceList(WiFiSettings.this);
            handler.postDelayed(loadDeviceListRunnable, DELAY);
        }
    };

    private Runnable loadSSIDRunnable = new Runnable() {
        @Override
        public void run() {
            loadDevicesData(WiFiSettings.this);
            handler.postDelayed(loadSSIDRunnable, DELAY_SSID);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        wiFiSSIDTextView = findViewById(R.id.tv_wifi_ssid);
        changeSSIDPasswordButton = findViewById(R.id.change_ssid_password_button);
        wiFiDeviceCount = findViewById(R.id.tv_device_count);

        mRecyclerView = findViewById(R.id.users_list_recyler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        handler = new Handler();

        handler.post(loadDeviceListRunnable);
        handler.post(loadSSIDRunnable);
    }

    public void loadDeviceList(Context context) {
        String urlString = NetworkUtils.getUrlString(NetworkUtils.LAN_INFO_ID);

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
                        if (!deviceInfoString.contains("Static"))
                            deviceViewModels.add(new DeviceViewModel(deviceInfoString));
                    }

                    if (mDeviceListAdapter == null) {
                        mDeviceListAdapter = new DeviceListAdapter();
                        mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
                        mRecyclerView.setAdapter(mDeviceListAdapter);
                    } else {
                        mDeviceListAdapter.setDeviceViewModels(deviceViewModels);
                    }

//                    for (DeviceViewModel viewModel: deviceViewModels) {
//                        Log.v("ViewModel", viewModel.getDeviceName());
//                    }

                } catch (JSONException e) {
                    Log.v(TAG, "userlistinfo not found in json response or json error: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    public void loadDevicesData(Context context) {
        String urlString = NetworkUtils.getUrlString(NetworkUtils.LAN_INFO_PAGE_ID);

        StringRequest lanInfoRequest = new StringRequest(Request.Method.GET,
                urlString, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Document lanInfoDocument = Jsoup.parse(response);
                wiFiSSIDTextView.setText(lanInfoDocument.getElementById("ssid").text());
                showData();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v(TAG, "Volley string request error: " + error.getMessage());
                showLoading();
            }
        });

        VolleySingleton.getInstance(context).addToRequestQueue(lanInfoRequest);
    }

    private void showLoading() {
//        loadingProgressBar.setVisibility(View.VISIBLE);
//        devicesLayout.setAlpha(Float.parseFloat("0.2"));
    }

    private void showData() {
//        loadingProgressBar.setVisibility(View.INVISIBLE);
//        devicesLayout.setAlpha(Float.parseFloat("1.0"));
    }
}