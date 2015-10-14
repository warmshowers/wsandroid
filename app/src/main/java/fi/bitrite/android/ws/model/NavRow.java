package fi.bitrite.android.ws.model;

/**
 * Represent the data in a row of Navigation Drawer
 */
public class NavRow {

    int mIconResource;
    String mRowText;
    String mOnClickAction;

    public NavRow(int iconResourceId, String rowText, String onClickAction) {
        this.mIconResource = iconResourceId;
        this.mRowText = rowText;
        this.mOnClickAction = onClickAction;
    }
    public int getIconResource() {
        return mIconResource;
    }

    public String getRowText() {
        return mRowText;
    }

    public String getOnClickAction() {
        return mOnClickAction;
    }

}
