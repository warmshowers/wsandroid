package fi.bitrite.android.ws.ui;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.repository.BaseSettingsRepository;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.util.Tools;

/**
 * Dialog that shows a provided list of users that can be selected.
 * A selected user is navigated to by showing its {@link UserFragment}.
 */
public class UserSelectDialogFragment extends DialogFragment implements Injectable {
    @Inject SettingsRepository mSettingsRepository;

    @BindView(R.id.title) TextView mTxtTitle;
    @BindView(R.id.distance_from_current) TextView mTxtDistanceFromCurrent;

    private final static String KEY_USERS = "users";
    private final static String KEY_LAST_DEVICE_LOCATION = "lastDeviceLocation";

    private List<? extends SimpleUser> mUsers;
    @Nullable private Location mLastDeviceLocation;
    private BaseSettingsRepository.DistanceUnit mDistanceUnit;
    private String mDistanceUnitShort;

    public static UserSelectDialogFragment create(
            List<? extends SimpleUser> users,
            @Nullable Location lastDeviceLocation) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_USERS, new ArrayList<>(users));
        bundle.putParcelable(KEY_LAST_DEVICE_LOCATION, lastDeviceLocation);

        UserSelectDialogFragment fragment = new UserSelectDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsers = requireArguments().getParcelableArrayList(KEY_USERS);
        mLastDeviceLocation = requireArguments().getParcelable(KEY_LAST_DEVICE_LOCATION);
        mDistanceUnit = mSettingsRepository.getDistanceUnit();
        mDistanceUnitShort = mSettingsRepository.getDistanceUnitShort();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        Context dialogContext = dialogBuilder.getContext();

        View titleView = null;
        if (mUsers.size() > 1) {
            titleView = LayoutInflater.from(dialogContext).inflate(
                    R.layout.view_multiuser_dialog_header, null, false);
            ButterKnife.bind(this, titleView);

            final SimpleUser representative = mUsers.get(0);
            mTxtTitle.setText(getResources().getQuantityString(R.plurals.users_at_location,
                    mUsers.size(), mUsers.size(), representative.getStreetCityAddress()));

            final double distance = mLastDeviceLocation != null
                    ? Tools.calculateDistanceBetween(
                    Tools.latLngToLocation(representative.location), mLastDeviceLocation, mDistanceUnit)
                    : -1;
            String distanceSummary = getString(
                    R.string.distance_from_current, (int) distance, mDistanceUnitShort);
            mTxtDistanceFromCurrent.setText(distanceSummary);
            mTxtDistanceFromCurrent.setVisibility(distance >= 0 ? View.VISIBLE : View.GONE);
        }

        UserListAdapter userListAdapter = new UserListAdapter(
                dialogContext, UserListAdapter.COMPERATOR_FULLNAME_ASC, null);
        userListAdapter.resetDataset(mUsers);
        // Remember the navigationController here as sometimes the activity is no longer set
        // in the listener.
        final NavigationController navigationController =
                ((MainActivity) requireActivity()).getNavigationController();
        return dialogBuilder
                .setCustomTitle(titleView)
                .setNegativeButton(R.string.ok, (dialog, which) -> {})
                .setAdapter(userListAdapter, (dialog, index) -> {
                    SimpleUser user = mUsers.get(index);
                    navigationController.navigateToUser(user.id);
                })
                .create();
    }
}
