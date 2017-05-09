/*
 * 	Copyright (c) 2017. Token Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tokenbrowser.presenter.webview;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.tokenbrowser.R;
import com.tokenbrowser.crypto.HDWallet;
import com.tokenbrowser.model.local.UnsignedW3Transaction;
import com.tokenbrowser.model.sofa.SofaAdapters;
import com.tokenbrowser.presenter.Presenter;
import com.tokenbrowser.util.LogUtil;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.WebViewActivity;
import com.tokenbrowser.view.custom.listener.OnLoadListener;
import com.tokenbrowser.view.fragment.DialogFragment.PaymentConfirmationDialog;
import com.tokenbrowser.view.fragment.DialogFragment.WebPaymentConfirmationListener;

import java.io.IOException;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class WebViewPresenter implements Presenter<WebViewActivity> {

    private WebViewActivity activity;
    private SofaWebViewClient webClient;
    private SofaInjector sofaInjector;

    private boolean isLoaded = false;

    @Override
    public void onViewAttached(final WebViewActivity view) {
        this.activity = view;
        initWebClient();
        initView();
    }

    private void initWebClient() {
        if (this.webClient != null) {
            hideLoadingSpinner();
            return;
        }
        this.webClient = new SofaWebViewClient(this.loadedListener);
        this.sofaInjector = new SofaInjector(this.loadedListener);
        this.activity.getBinding().webview.getSettings().setJavaScriptEnabled(true);
        this.activity.getBinding().webview.getSettings().setBuiltInZoomControls(true);
        this.activity.getBinding().webview.getSettings().setDisplayZoomControls(false);
        this.activity.getBinding().webview.addJavascriptInterface(new SOFAHost(), "SOFAHost");
        this.activity.getBinding().webview.setWebViewClient(this.webClient);
    }

    private void initView() {
        initToolbar();
        animateLoadingSpinner();
    }

    private void initToolbar() {
        final String address = getAddress();
        this.activity.getBinding().title.setText(address);
        this.activity.getBinding().closeButton.setOnClickListener(__ -> this.activity.onBackPressed());
    }

    private void animateLoadingSpinner() {
        if (this.activity == null || this.isLoaded) return;
        final Animation rotateAnimation = AnimationUtils.loadAnimation(this.activity, R.anim.rotate);
        this.activity.getBinding().loadingView.startAnimation(rotateAnimation);
    }

    private final OnLoadListener loadedListener = new OnLoadListener() {
        @Override
        public void onReady() {
            if (activity == null) return;
            final String address = getAddress();

            sofaInjector.loadUrl(address)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::handleWebResourceResponse,
                            this::onError
                    );
        }

        private void handleWebResourceResponse(final SofaInjectResponse response) {
            if (activity == null) return;
            activity.getBinding()
                    .webview
                    .loadDataWithBaseURL(
                        response.getAddress(),
                        response.getData(),
                        response.getMimeType(),
                        response.getEncoding(),
                        null);
        }

        @Override
        public void onLoaded() {
            if (activity == null) return;
            hideLoadingSpinner();
            isLoaded = true;
        }

        @Override
        public void onError(final Throwable t) {
            LogUtil.exception(getClass(), "Unable to load Dapp", t);
            if (activity == null) return;
            Toast.makeText(activity, R.string.error__dapp_loading, Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    };

    private void hideLoadingSpinner() {
        activity.getBinding().loadingView.clearAnimation();
        activity.getBinding().loadingView.setVisibility(View.GONE);
        activity.getBinding().webview.setVisibility(View.VISIBLE);
    }

    private String getAddress() {
        return this.activity.getIntent().getStringExtra(WebViewActivity.EXTRA__ADDRESS).trim();
    }

    @Override
    public void onViewDetached() {
        this.activity = null;
        // WebView doesn't handle orientation change so
        // we can destroy it. This seems to be an Android issue.
        // this kinda sucks.
        destroy();
    }

    @Override
    public void onDestroyed() {
        this.activity = null;
    }

    private void destroy() {
        this.sofaInjector.destroy();
        this.sofaInjector = null;
        this.webClient = null;
        this.isLoaded = false;
    }

    private HDWallet getWallet() {
        return BaseApplication
                .get()
                .getTokenManager()
                .getWallet()
                .toBlocking()
                .value();
    }

    private class SOFAHost {

        @JavascriptInterface
        public String getRcpUrl() {
            return BaseApplication.get().getResources().getString(R.string.rcp_url);
        }

        @JavascriptInterface
        public String getAccounts() {
            return "[\"" + getWallet().getPaymentAddress() + "\"]";
        }

        @JavascriptInterface
        public boolean approveTransaction(final String unsignedTransaction) {
            final UnsignedW3Transaction transaction;
            try {
                transaction = SofaAdapters.get().unsignedW3TransactionFrom(unsignedTransaction);
            } catch (final IOException e) {
                LogUtil.exception(getClass(), "Unable to parse unsigned transaction. ", e);
                return false;
            }

            return transaction.getFrom().equals(getWallet().getPaymentAddress());
        }

        @JavascriptInterface
        public void signTransaction(final String unsignedTransaction) {

            final UnsignedW3Transaction transaction;
            try {
                transaction = SofaAdapters.get().unsignedW3TransactionFrom(unsignedTransaction);
            } catch (final IOException e) {
                LogUtil.exception(getClass(), "Unable to parse unsigned transaction. ", e);
                return;
            }
            if (activity == null) return;
            final PaymentConfirmationDialog dialog =
                    PaymentConfirmationDialog
                            .newInstanceWebPayment(
                                    unsignedTransaction,
                                    transaction.getTo(),
                                    transaction.getValue(),
                                    null
                            );
            dialog.show(activity.getSupportFragmentManager(), PaymentConfirmationDialog.TAG);
            dialog.setOnPaymentConfirmationListener(this.confirmationListener);
        }

        private final WebPaymentConfirmationListener confirmationListener = new WebPaymentConfirmationListener() {
            @Override
            public void onWebPaymentApproved(final String unsignedTransaction) {
                final String signedTransaction = getWallet().signTransaction(unsignedTransaction);
                // To Do -- pass the signed transaction back to webview
            }
        };
    }
}
