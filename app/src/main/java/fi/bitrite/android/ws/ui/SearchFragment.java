package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class SearchFragment extends BaseFragment {

    private static final String KEY_QUERY = "query";
    private static final String KEY_USER_IDS = "user_ids";

    @Inject UserRepository mUserRepository;

    @BindView(R.id.search_lst_result) ListView mLstSearchResult;
    @BindColor(R.color.primaryColorAccent) int mDecoratorForegroundColor;

    private UserListAdapter mSearchResultListAdapter;

    private String mQuery;

    private final BehaviorSubject<List<Integer>> mUserIds = BehaviorSubject.create();
    private final BehaviorSubject<SearchResult> mLastSearchResult = BehaviorSubject.create();
    private Disposable mProgressDisposable;

    public static Fragment create(String query) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);

        Fragment fragment = new SearchFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle icicle) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        ButterKnife.bind(this, view);

        if (mQuery == null) {
            // If we return from the UserFragment after clicking an entry, we often do not need to
            // re-do all initialization here, as it is still around.

            // Checks if we already have the search result.
            boolean searchSuccessful = icicle != null && icicle.containsKey(KEY_USER_IDS);
            if (searchSuccessful) {
                mUserIds.onNext(icicle.getIntegerArrayList(KEY_USER_IDS));
            }

            // Does the requested search if it did not finish yet.
            final Bundle args = getArguments();
            mQuery = args != null ? args.getString(KEY_QUERY) : null;
            if (mQuery != null && !searchSuccessful) {
                doTextSearch(mQuery);
            }
        }

        // Initializes the search result list.
        if (mSearchResultListAdapter == null) {
            Decorator decorator = new Decorator(mQuery, mDecoratorForegroundColor);
            mSearchResultListAdapter = new UserListAdapter(getContext(), mComparator, decorator);
        }
        mLstSearchResult.setAdapter(mSearchResultListAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        getResumePauseDisposable().add(mUserIds
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userIds -> {
                    List<Observable<Resource<User>>> users = mUserRepository.get(userIds);
                    mSearchResultListAdapter.resetDataset(users, 0);
                }));

        // Structure for decoupling the message send callback that is processed upon arrival from
        // its handler that can only be executed when the app is in the foreground. Handler.
        getResumePauseDisposable().add(mLastSearchResult
                .filter(result -> !result.isHandled)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    result.isHandled = true;

                    if (mProgressDisposable != null) {
                        mProgressDisposable.dispose();
                    }

                    if (result.throwable != null) {
                        // TODO(saemy): Better error message.
                        DialogHelper.alert(getContext(), R.string.http_server_access_failure);
                    } else {
                        if (result.userIds.isEmpty()) {
                            DialogHelper.alert(getContext(), R.string.no_results);
                        }
                        mUserIds.onNext(result.userIds);
                    }
                }));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        SearchResult lastSearchResult = mLastSearchResult.getValue();
        if (lastSearchResult != null && lastSearchResult.userIds != null) {
            outState.putIntegerArrayList(KEY_USER_IDS, new ArrayList<>(lastSearchResult.userIds));
        }

        super.onSaveInstanceState(outState);
    }

    private void doTextSearch(String text) {
        mProgressDisposable = ProgressDialog.create(R.string.performing_search)
                .show(getActivity());

        // Structure for decoupling the message send callback that is processed upon arrival from
        // its handler that can only be executed when the app is in the foreground. Callback
        Disposable unused = mUserRepository
                .searchByKeyword(text)
                .subscribe(userIds -> mLastSearchResult.onNext(new SearchResult(userIds)),
                        throwable -> {
                            Log.e(WSAndroidApplication.TAG, throwable.getMessage());
                            mLastSearchResult.onNext(new SearchResult(throwable));
                        });
    }

    @OnItemClick(R.id.search_lst_result)
    public void onUserClicked(int position) {
        User user = (User) mLstSearchResult.getItemAtPosition(position);
        getNavigationController().navigateToUser(user.id);
    }

    @Override
    protected CharSequence getTitle() {
        CharSequence title = getString(R.string.title_fragment_search);
        if (mQuery != null) {
            title = title + ": \"" + mQuery + "\"";
        }
        return title;
    }

    private final static Comparator<? super SimpleUser> mComparator = (left, right) -> {
        int caLeft = left.isCurrentlyAvailable ? 1 : 0;
        int caRight = right.isCurrentlyAvailable ? 1 : 0;

        return caLeft != caRight
                ? caRight - caLeft
                : left.getName().compareTo(right.getName()); // TODO(saemy): Something smarter?
    };

    private static class Decorator implements UserListAdapter.Decorator {
        private final String mQuery;
        private final Pattern mQueryPattern;
        @ColorInt private int mForegroundColor;

        Decorator(String query, @ColorInt int foregroundColor) {
            mQuery = query;
            mQueryPattern = query != null
                    ? Pattern.compile(Pattern.quote(query),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    : null;
            mForegroundColor = foregroundColor;
        }

        @Override
        public SpannableStringBuilder decorateFullname(String fullname) {
            return mQueryPattern.matcher(fullname).find()
                    ? getTextMatch(mQuery, fullname)
                    : new SpannableStringBuilder(fullname);
        }

        @Override
        public SpannableStringBuilder decorateLocation(String location) {
            final String locationLower = location.toLowerCase();
            return mQueryPattern.matcher(locationLower).find()
                    ? getTextMatch(mQuery, locationLower)
                    : new SpannableStringBuilder(location);
        }

        private SpannableStringBuilder getTextMatch(String pattern, String match) {
            final Pattern p =
                    Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            final Matcher matcher = p.matcher(match);

            // TODO: ignore accents and other special characters
            final SpannableStringBuilder spannable = new SpannableStringBuilder(match);
            final ForegroundColorSpan span = new ForegroundColorSpan(mForegroundColor);
            while (matcher.find()) {
                spannable.setSpan(
                        span, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }
    }

    private class SearchResult {
        final List<Integer> userIds;
        final Throwable throwable;
        boolean isHandled = false;

        private SearchResult(@NonNull List<Integer> userIds) {
            this.userIds = userIds;
            this.throwable = null;
        }
        private SearchResult(@NonNull Throwable throwable) {
            this.userIds = null;
            this.throwable = throwable;
        }
    }
}
