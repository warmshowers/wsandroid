package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import android.webkit.CookieManager;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.util.GlobalInfo;

public class WebViewActivity extends WSBaseActivity {
    WebView mWebView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        // Tell the BaseActivity to use a back action in the toolbar instead of the hamburger
        mHasBackIntent = true;

        initView();

        mWebView = (WebView) this.findViewById(R.id.webView);

        // Handle the back button so they can get back to the app after following links
        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
                return false;
            }
        });
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.getSettings().setBuiltInZoomControls(true);


        String url = this.getIntent().getDataString();
        setTitle(this.getIntent().getStringExtra("webview_title"));

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        String cookieString = "";
        try {
            cookieString = AuthenticationHelper.getAccountCookie();
        } catch (NoAccountException e) {
            Toast.makeText(this, R.string.no_account, Toast.LENGTH_SHORT);
            // We'll continue because they *could* just log in.
        }
        cookieManager.setCookie(GlobalInfo.warmshowersBaseUrl, cookieString);
        CookieSyncManager.getInstance().sync();

        mWebView.loadUrl(url);

        new MaterialDialog.Builder(this)
                .title("Leaving app")
                .content(getString(R.string.embedded_browser_warning))
                .positiveText(android.R.string.yes)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // continue
                    }
                })
                //.icon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    public static void viewOnSite(Context context, String urlString, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.setData(Uri.parse(urlString));
        intent.putExtra("webview_title", title);
        context.startActivity(intent);
    }
}
