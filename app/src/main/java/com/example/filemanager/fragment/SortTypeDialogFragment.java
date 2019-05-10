package com.example.filemanager.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.example.App;
import com.example.filemanager.R;
import com.example.filemanager.model.SortType;
import com.example.filemanager.repository.settings.SettingsRepository;

public class SortTypeDialogFragment extends DialogFragment {
    private SettingsRepository settingsRepository;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        injectSettingsRepository();
        String[] choices = getActivity().getResources().getStringArray(R.array.sort_types);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sort_type_dialog_title)
                .setSingleChoiceItems(choices, getSortType(), (dialog, which) -> {})
                .setPositiveButton(R.string.sort_type_dialog_positive_button, (dialog, id) -> {
                    handlePositiveButtonClicked(dialog);
                })
                .create();
    }

    private void injectSettingsRepository() {
        App app = (App) getActivity().getApplication();
        settingsRepository = app.getSettingsRepository();
    }

    private void handlePositiveButtonClicked(@NonNull DialogInterface dialog) {
        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
        saveSortType(selectedPosition);
    }

    private int getSortType() {
        return settingsRepository.getSortType().toInt();
    }

    private void saveSortType(int sortType) {
        settingsRepository.setSortType(SortType.fromInt(sortType));
    }
}
