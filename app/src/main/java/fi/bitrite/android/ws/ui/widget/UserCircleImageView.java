package fi.bitrite.android.ws.ui.widget;


import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fi.bitrite.android.ws.model.Host;

public class UserCircleImageView extends CircleImageView {

    public UserCircleImageView(Context context) {
        super(context);
    }

    public UserCircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserCircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setUser(@Nullable Host user) {
        if (user == null) {
            setUsers(null);
        } else {
            setUsers(Collections.singletonList(user));
        }
    }

    private static List<Integer> colors = Arrays.asList(
            0xffe57373,
            0xfff06292,
            0xffba68c8,
            0xff9575cd,
            0xff7986cb,
            0xff64b5f6,
            0xff4fc3f7,
            0xff4dd0e1,
            0xff4db6ac,
            0xff81c784,
            0xffaed581,
            0xffff8a65,
            0xffd4e157,
            0xffffd54f,
            0xffffb74d,
            0xffa1887f,
            0xff90a4ae
    );

    public void setUsers(@Nullable List<Host> users) {
        String url;
        String text;
        int color;
        if (users == null || users.isEmpty()) {
            url = null;
            text = null;
            color = colors.get(0);
        } else {
            int colorHash = 0;
            url = null;
            StringBuilder labelSB = new StringBuilder();
            for (Host user : users) {
                final String name = TextUtils.isEmpty(user.getFullname())
                        ? user.getName()
                        : user.getFullname();
                colorHash ^= name.hashCode();

                if (labelSB.length() < 2) {
                    labelSB.append(Character.toUpperCase(name.charAt(0)));
                }

                url = user.getProfilePictureSmall();
            }

            // No image for groups.
            if (users.size() > 1) {
                url = null;
            }

            text = labelSB.toString();
            color = colors.get(Math.abs(colorHash) % colors.size());
        }

        Picasso.with(getContext()).load(url).into(this);
        setText(text);
        setCircleBackgroundColor(color);
    }
}
