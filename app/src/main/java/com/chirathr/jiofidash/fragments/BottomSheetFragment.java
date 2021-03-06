package com.chirathr.jiofidash.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chirathr.jiofidash.R;
import com.chirathr.jiofidash.data.JioFiPreferences;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    public static final int OPTION_RESTART_ID = 0;
    public static final int OPTION_WIFI_SETTINGS_ID = 1;
    public static final int OPTION_PUSH_WPS_BUTTON = 2;
    public static final int OPTION_CHANGE_SAVED_USERNAME_PASSWORD = 3;
    public static final int OPTION_CHANGE_SELECTED_DEVICE = 4;
    public static final int OPTION_ADMIN_WEB_UI = 5;
    public static final int OPTION_ABOUT_ID = 6;

    private onOptionSelectedListener mListener;



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (onOptionSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onOptionSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_fragment_card, container, false);

        view.findViewById(R.id.action_restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_RESTART_ID);
            }
        });

        view.findViewById(R.id.action_wifi_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_WIFI_SETTINGS_ID);
            }
        });

        view.findViewById(R.id.action_push_wps_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_PUSH_WPS_BUTTON);
            }
        });

        view.findViewById(R.id.action_open_admin_web_ui).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_ADMIN_WEB_UI);
            }
        });

        view.findViewById(R.id.action_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_ABOUT_ID);
            }
        });

        TextView changeSavedLoginDetails = view.findViewById(R.id.action_change_saved_username_password);
        changeSavedLoginDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_CHANGE_SAVED_USERNAME_PASSWORD);
            }
        });

        if (!JioFiPreferences.getInstance().isLoginDataAvailable(getActivity())) {
            changeSavedLoginDetails.setText(getContext().getString(R.string.add_login_details));
        }

        view.findViewById(R.id.action_change_selected_device)
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onOptionSelected(OPTION_CHANGE_SELECTED_DEVICE);
            }
        });

        return view;
    }

    public interface onOptionSelectedListener {
        void onOptionSelected(int optionId);
    }
}
