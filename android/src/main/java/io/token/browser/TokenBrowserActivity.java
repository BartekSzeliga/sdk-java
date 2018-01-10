package io.token.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static io.token.browser.TokenBrowserService.MSG_CLOSE;
import static io.token.browser.TokenBrowserService.MSG_GO_TO;
import static io.token.browser.TokenBrowserService.MSG_KEY_SID;
import static io.token.browser.TokenBrowserService.MSG_KEY_URL;
import static io.token.browser.TokenBrowserService.MSG_ON_URL;

public class TokenBrowserActivity extends Activity {
    private String sessionId;
    private MessengerClient messenger;
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.sessionId = savedInstanceState.getString(MSG_KEY_SID);
        } else {
            this.sessionId = getIntent().getExtras().getString(MSG_KEY_SID);
        }

        webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Bundle data = new Bundle(2);
                data.putString(MSG_KEY_SID, sessionId);
                data.putString(MSG_KEY_URL, url);
                messenger.send(MSG_ON_URL, data);
            }
        });
        setContentView(webview);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle data = new Bundle(1);
        data.putString(MSG_KEY_SID, sessionId);
        this.messenger = new MessengerClient(this, new IncomingHandler(), data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        messenger.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(MSG_KEY_SID, sessionId);
        super.onSaveInstanceState(outState);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            String sid = data != null ? data.getString(MSG_KEY_SID) : null;

            switch (msg.what) {
                case MSG_CLOSE:
                    if (sessionId.equals(sid)) {
                        finish();
                    }
                    break;
                case MSG_GO_TO:
                    final String url = data.getString(MSG_KEY_URL);
                    if (sessionId.equals(sid)) {
                        webview.loadUrl(url);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
