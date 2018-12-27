package fi.bitrite.android.ws.ui.listadapter;


import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.text.format.DateUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MessageListAdapter extends
        DataBoundListAdapter<Message, MessageListAdapter.ItemBinding> {

    public final static Comparator<Message> COMPARATOR =
            (left, right) -> left.date.compareTo(right.date);

    private final LoggedInUserHelper mLoggedInUserHelper;
    private final UserRepository mUserRepository;

    private boolean mIsGroupChat;

    public MessageListAdapter(LoggedInUserHelper loggedInUserHelper,
                              UserRepository userRepository) {
        mLoggedInUserHelper = loggedInUserHelper;
        mUserRepository = userRepository;
    }

    @Override
    public Completable replaceRx(List<Message> messages) {
        mIsGroupChat = false;
        Set<Integer> participantIds = new HashSet<>();
        for (Message message : messages) {
            participantIds.add(message.authorId);

            if (participantIds.size() > 2) {
                mIsGroupChat = true;
                break;
            }
        }

        return super.replaceRx(messages);
    }

    @Override
    protected ItemBinding createBinding(ViewGroup parent) {
        return new ItemBinding(parent);
    }

    @Override
    protected void sort(List<Message> messages) {
        Collections.sort(messages, COMPARATOR);
    }

    @Override
    protected boolean areItemsTheSame(Message left, Message right) {
        return left.id == right.id;
    }

    @Override
    protected boolean areContentsTheSame(Message left, Message right) {
        return left.id == right.id
               && left.threadId == right.threadId
               && left.authorId == right.authorId
               && left.isPushed == right.isPushed
               && left.date.equals(right.date)
               && left.rawBody.equals(right.rawBody);
    }

    class ItemBinding implements
            DataBoundListAdapter.ViewDataBinding<Message> {

        @BindView(R.id.message_lbl_sender) TextView mLblSender;
        @BindView(R.id.message_lbl_body) TextView mLblBody;
        @BindView(R.id.message_lbl_date) TextView mLblDate;

        @BindDimen(R.dimen.message_bubble_margin_big) int mMarginBubbleBig;
        @BindDimen(R.dimen.message_bubble_margin_small) int mMarginBubbleSmall;
        @BindDrawable(R.drawable.message_incoming_bubble) Drawable mDrawableBubbleIncoming;
        @BindDrawable(R.drawable.message_outgoing_bubble) Drawable mDrawableBubbleOutgoing;

        private final View mRoot;
        private CompositeDisposable mDisposables = new CompositeDisposable();

        private final int[] mParticipantColors;
        private int mNextParticipantColorIdx = 0;
        private final SparseIntArray mParticipantColorMap = new SparseIntArray();


        ItemBinding(ViewGroup parent) {
            mRoot = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false);
            ButterKnife.bind(this, mRoot);

            // Parses the message sender colors.
            TypedArray ta = parent.getResources().obtainTypedArray(R.array.colors_message_author);
            mParticipantColors = new int[ta.length()];
            for (int i = 0; i < ta.length(); i++) {
                mParticipantColors[i] = ta.getColor(i, 0);
            }
            ta.recycle();
        }

        @Override
        public View getRoot() {
            return mRoot;
        }

        @Override
        public void bind(@NonNull Message message) {
            mDisposables.dispose();
            mDisposables = new CompositeDisposable();

            final User loggedInUser = mLoggedInUserHelper.get();
            final int loggedInUserId = loggedInUser == null ? -1 : loggedInUser.id;
            final boolean isIncoming = message.authorId != loggedInUserId;

            // Sets the margin of the bubble.
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) mRoot.getLayoutParams();
            lp.leftMargin = isIncoming ? mMarginBubbleSmall : mMarginBubbleBig;
            lp.rightMargin = isIncoming ? mMarginBubbleBig : mMarginBubbleSmall;
            mRoot.setLayoutParams(lp);

            // Sets the background of the bubble.
            ViewCompat.setBackground(mRoot,
                    isIncoming ? mDrawableBubbleIncoming : mDrawableBubbleOutgoing);

            // Shows the sender if the message is incoming and this is a group chat.
            boolean showSender = isIncoming && mIsGroupChat;
            mLblSender.setVisibility(showSender ? View.VISIBLE : View.GONE);
            mLblSender.setTextColor(isIncoming ? getSenderColor(message.authorId) : Color.BLACK);

            // Sets the sender.
            if (showSender) {
                mDisposables.add(mUserRepository.get(message.authorId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(userResource -> {
                            User user = userResource.data;
                            mLblSender.setText(user == null ? "" : user.getName());
                        }));
            }

            mLblBody.setText(message.body);
            mLblDate.setText(!message.isPushed
                    ? "..." // Its not sent yet. // TODO(saemy): Add an hourglass icon?
                    : DateUtils.getRelativeTimeSpanString(
                            message.date.getTime(), new Date().getTime(),
                            0, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
                               | DateUtils.FORMAT_ABBREV_RELATIVE)
                            .toString());
        }

        @ColorInt
        private int getSenderColor(int authorId) {
            int color = mParticipantColorMap.get(authorId, -1);
            if (color == -1) {
                mNextParticipantColorIdx =
                        (mNextParticipantColorIdx + 1) % mParticipantColors.length;
                color = mParticipantColors[mNextParticipantColorIdx];
                mParticipantColorMap.put(authorId, color);
            }
            return color;
        }
    }
}
