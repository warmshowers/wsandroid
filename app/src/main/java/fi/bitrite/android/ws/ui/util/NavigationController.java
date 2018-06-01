package fi.bitrite.android.ws.ui.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.ui.AboutFragment;
import fi.bitrite.android.ws.ui.ContactUserFragment;
import fi.bitrite.android.ws.ui.FavoriteUsersFragment;
import fi.bitrite.android.ws.ui.FeedbackFragment;
import fi.bitrite.android.ws.ui.MainActivity;
import fi.bitrite.android.ws.ui.MapFragment;
import fi.bitrite.android.ws.ui.MessageThreadFragment;
import fi.bitrite.android.ws.ui.MessageThreadsFragment;
import fi.bitrite.android.ws.ui.SearchFragment;
import fi.bitrite.android.ws.ui.SettingsFragment;
import fi.bitrite.android.ws.ui.UserFragment;
import fi.bitrite.android.ws.ui.UserListFragment;
import io.reactivex.subjects.BehaviorSubject;

/**
 * A utility class that handles navigation in {@link MainActivity}.
 */
public class NavigationController {

    public final static String NAVIGATION_TAG_MAP = "map";
    public final static String NAVIGATION_TAG_FAVORITE_USERS = "favoriteUsers";
    public final static String NAVIGATION_TAG_MESSAGE_THREADS = "messageThreads";
    public final static String NAVIGATION_TAG_ACCOUNT = "account";
    public final static String NAVIGATION_TAG_SETTINGS = "settings";
    public final static String NAVIGATION_TAG_ABOUT = "about";

    private final static String NAVIGATION_TAG_MAIN = NAVIGATION_TAG_MAP;

    private final FragmentManager mFragmentManager;

    // The tag of the current top-level navigation item (messageThreads/10432 -> messageThreads).
    private BehaviorSubject<String> mTopLevelNavigationItemTag = BehaviorSubject.create();

    public NavigationController(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;

        // Listens for changes in the backstack and updates the top level tag.
        mFragmentManager.addOnBackStackChangedListener(() -> {
            int numBackStackEntries = mFragmentManager.getBackStackEntryCount();
            String tag;
            if (numBackStackEntries > 0) {
                BackStackEntry entry =
                        mFragmentManager.getBackStackEntryAt(numBackStackEntries - 1);
                tag = entry.getName();
            } else {
                tag = NAVIGATION_TAG_MAIN;
            }

            // Sets the new top-level navigation item tag.
            mTopLevelNavigationItemTag.onNext(getTopLevelTag(tag));
        });
    }

    public void navigateToTag(String tag) {
        switch (tag) {
            case NAVIGATION_TAG_MAP:
                navigateToMap();
                break;

            case NAVIGATION_TAG_FAVORITE_USERS:
                navigateToFavoriteUsers();
                break;

            case NAVIGATION_TAG_MESSAGE_THREADS:
                navigateToMessageThreads();
                break;

            case NAVIGATION_TAG_ACCOUNT:
                navigateToAccount();
                break;

            case NAVIGATION_TAG_SETTINGS:
                navigateToSettings();
                break;

            case NAVIGATION_TAG_ABOUT:
                navigateToAbout();
                break;

            default:
                throw new RuntimeException("Invalid navigation item tag: " + tag);
        }
    }

    public void navigateToMainFragment() {
        navigateToTag(NAVIGATION_TAG_MAIN);
    }

    public void navigateToAbout() {
        navigateTo(NAVIGATION_TAG_ABOUT, AboutFragment.create(), true);
    }

    public void navigateToAccount() {
        // TODO: Implement
//        navigateTo(
//                "account",
//                AccountFragment.create());
    }

    public void navigateToMap() {
        // This is the main fragment.
        navigateTo(NAVIGATION_TAG_MAP, MapFragment.create(), false, true);
    }
    public void navigateToMap(LatLng latLng) {
        navigateTo(NAVIGATION_TAG_MAP, MapFragment.create(latLng), false);
    }

    public void navigateToMessageThreads() {
        navigateTo(NAVIGATION_TAG_MESSAGE_THREADS, MessageThreadsFragment.create(), true);
    }

    public void navigateToMessageThread(int threadId) {
        navigateTo(NAVIGATION_TAG_MESSAGE_THREADS + '/' + threadId,
                MessageThreadFragment.create(threadId), false);
    }

    public void navigateToSettings() {
        navigateTo(NAVIGATION_TAG_SETTINGS, SettingsFragment.create(), true);
    }

    public void navigateToFavoriteUsers() {
        navigateTo(NAVIGATION_TAG_FAVORITE_USERS, FavoriteUsersFragment.create(), true);
    }

    public void navigateToUser(int userId) {
        navigateTo(NAVIGATION_TAG_MAIN + "/user-" + userId, UserFragment.create(userId), false);
    }

    public void navigateToFeedback(int recipientId) {
        navigateTo(NAVIGATION_TAG_MAIN + "/user-" + recipientId + "/feedback",
                FeedbackFragment.create(recipientId), false);
    }

    public void navigateToContactUser(Host recipient) {
        navigateTo(NAVIGATION_TAG_MAIN + "/user-" + recipient.getId() + "/contact",
                ContactUserFragment.create(recipient), false);
    }

    public void navigateToUserList(ArrayList<Integer> userIds) {
        navigateTo(NAVIGATION_TAG_MAIN + "/user_list-" + userIds.hashCode(),
                UserListFragment.create(userIds), false);
    }

    public void navigateToSearch(String query) {
        navigateTo(NAVIGATION_TAG_MAIN + "/search-" + query.hashCode(),
                SearchFragment.create(query), false);
    }

    // The backstack is always destroyed when selecting a new item in the navigation drawer.
    // TODO(saemy): Should this be automatically done when using tags that are deeper than /xxx ?
    private void navigateTo(String tag, Fragment fragment, boolean deleteBackStack) {
        navigateTo(tag, fragment, true, deleteBackStack);
    }

    private void navigateTo(String tag, Fragment fragment, boolean addToBackStack,
                            boolean deleteBackStack) {
        // Empties the backstack if requested.
        if (deleteBackStack) {
            clearBackStack();
        }

        // We need to manually update the top level tag if we do not add to the backstack as our
        // listener does not get fired.
        if (!addToBackStack && deleteBackStack) {
            mTopLevelNavigationItemTag.onNext(getTopLevelTag(tag));
        }

        // Starts the fragment transition transaction.
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, fragment, tag);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }

        fragmentTransaction.commitAllowingStateLoss(); // TODO(saemy): Do we want this?
    }

    /**
     * Removes all entries on the backstack.
     */
    public void clearBackStack() {
        if (!isBackstackEmpty()) {
            int firstFragmentId = mFragmentManager.getBackStackEntryAt(0).getId();
            mFragmentManager.popBackStack(firstFragmentId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * Removes the top element of the backstack.
     */
    public void popBackStack() {
        mFragmentManager.popBackStack();
    }

    public boolean isBackstackEmpty() {
        return mFragmentManager.getBackStackEntryCount() == 0;
    }

    /**
     * Decides if the burger icon of the navigation drawer should be replaced by a back arrow.
     *
     * @return true, iff a back arrow should be displayed.
     */
    public boolean isShowHomeAsUp() {
        int backStackSize = mFragmentManager.getBackStackEntryCount();
        if (backStackSize == 0) {
            return false;
        }


        BackStackEntry entry = mFragmentManager.getBackStackEntryAt(backStackSize - 1);
        String tag = entry.getName();

        // messageThreads/4324 -> [ "messageThreads", "4324" ]
        String[] parts = tag.split("/");

        return parts.length >= 2;
    }

    public BehaviorSubject<String> getTopLevelNavigationItemTag() {
        return mTopLevelNavigationItemTag;
    }

    private static String getTopLevelTag(String tag) {
        return tag.split("/", 2)[0];
    }
}
