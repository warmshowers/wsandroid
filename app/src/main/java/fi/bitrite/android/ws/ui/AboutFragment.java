package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;

public class AboutFragment extends BaseFragment {

    @BindView(R.id.about_lbl_app_version) TextView mLblAppVersion;

    public static AboutFragment create() {
        Bundle bundle = new Bundle();

        AboutFragment aboutFragment = new AboutFragment();
        aboutFragment.setArguments(bundle);
        return aboutFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, view);

        mLblAppVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        return view;
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_about);
    }
}
