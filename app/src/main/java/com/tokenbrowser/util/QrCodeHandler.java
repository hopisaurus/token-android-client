package com.tokenbrowser.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tokenbrowser.R;
import com.tokenbrowser.exception.InvalidQrCode;
import com.tokenbrowser.exception.InvalidQrCodePayment;
import com.tokenbrowser.model.local.QrCodePayment;
import com.tokenbrowser.model.local.User;
import com.tokenbrowser.model.sofa.Payment;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.ChatActivity;
import com.tokenbrowser.view.activity.ViewUserActivity;
import com.tokenbrowser.view.fragment.DialogFragment.PaymentConfirmationDialog;

import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class QrCodeHandler implements PaymentConfirmationDialog.OnPaymentConfirmationListener {

    public interface QrCodeHandlerListener {
        void onInvalidQrCode();
    }

    private static final String WEB_SIGNIN = "web-signin:";

    private CompositeSubscription subscriptions;
    private AppCompatActivity activity;
    private QrCodeHandlerListener listener;

    public QrCodeHandler(final AppCompatActivity activity) {
        this.activity = activity;
        this.subscriptions = new CompositeSubscription();
    }

    public void setOnQrCodeHandlerListener(final QrCodeHandlerListener listener) {
        this.listener = listener;
    }

    public void handleResult(final String result) {
        final QrCode qrCode = new QrCode(result);
        final @QrCodeType.Type int qrCodeType = qrCode.getQrCodeType();

        if (qrCodeType == QrCodeType.EXTERNAL_PAY) {
            handleExternalPayment(qrCode);
        } else if (qrCodeType == QrCodeType.ADD) {
            handleAddQrCode(qrCode);
        } else if (qrCodeType == QrCodeType.PAY) {
            handlePaymentQrCode(qrCode);
        } else if (qrCodeType == QrCodeType.PAYMENT_ADDRESS) {
            handlePaymentAddressQrCode(qrCode);
        } else if (result.startsWith(WEB_SIGNIN)) {
            handleWebLogin(result);
        } else {
            handleInvalidQrCode();
        }
    }

    private void handleExternalPayment(final QrCode qrCode)  {
        try {
            final QrCodePayment payment = qrCode.getExternalPayment();
            final Subscription sub =
                    getUserFromPaymentAddress(payment.getAddress())
                    .subscribe(
                            user -> showTokenPaymentConfirmationDialog(user.getTokenId(), payment),
                            __ -> showExternalPaymentConfirmationDialog(payment)
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private Single<User> getUserFromPaymentAddress(final String paymentAddress) {
        return BaseApplication
                .get()
                .getTokenManager()
                .getUserManager()
                .getUserFromPaymentAddress(paymentAddress)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void showTokenPaymentConfirmationDialog(final String tokenId, final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationDialog dialog =
                    PaymentConfirmationDialog
                            .newInstanceTokenPayment(
                                    tokenId,
                                    payment.getValue(),
                                    payment.getMemo()
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationDialog.TAG);
            dialog.setOnPaymentConfirmationListener(this);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private void showExternalPaymentConfirmationDialog(final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationDialog dialog =
                    PaymentConfirmationDialog
                            .newInstanceExternalPayment(
                                    payment.getAddress(),
                                    payment.getValue(),
                                    payment.getMemo()
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationDialog.TAG);
            dialog.setOnPaymentConfirmationListener(this);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private void handleAddQrCode(final QrCode qrCode) {
        try {
            final Subscription sub =
                    getUserByUsername(qrCode.getUsername())
                    .doOnSuccess(__ -> playScanSound())
                    .subscribe(
                            user -> goToProfileView(user.getTokenId()),
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCode e) {
            handleInvalidQrCode();
        }
    }

    private void goToProfileView(final String tokenId) {
        if (this.activity == null) return;
        final Intent intent = new Intent(this.activity, ViewUserActivity.class)
                .putExtra(ViewUserActivity.EXTRA__USER_ADDRESS, tokenId)
                .putExtra(ViewUserActivity.EXTRA__PLAY_SCAN_SOUNDS, true);
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    private void handlePaymentQrCode(final QrCode qrCode) {
        try {
            final QrCodePayment payment = qrCode.getPayment();
            final Subscription sub =
                    getUserByUsername(payment.getUsername())
                    .doOnSuccess(__ -> playScanSound())
                    .subscribe(
                            user -> showTokenPaymentConfirmationDialog(user.getTokenId(), payment),
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private Single<User> getUserByUsername(final String username) {
        return BaseApplication
                .get()
                .getTokenManager()
                .getUserManager()
                .getUserByUsername(username)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void handlePaymentAddressQrCode(final QrCode qrCode) {
        if (this.activity == null) return;
        final ClipboardManager clipboard = (ClipboardManager) this.activity.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(this.activity.getString(R.string.payment_address), qrCode.getUrl());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(
                this.activity,
                this.activity.getString(R.string.copied_payment_address_to_clipboard, qrCode.getUrl()),
                Toast.LENGTH_LONG
        ).show();

        this.activity.finish();
    }

    private void handleWebLogin(final String result) {
        final String token = result.substring(WEB_SIGNIN.length());
        final Subscription sub =
                loginWithToken(token)
                .doOnSuccess(__ -> playScanSound())
                .subscribe(
                        __ -> handleLoginSuccess(),
                        this::handleLoginFailure
                );

        this.subscriptions.add(sub);
    }

    private Single<Void> loginWithToken(final String token) {
        return BaseApplication
                .get()
                .getTokenManager()
                .getUserManager()
                .webLogin(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void playScanSound() {
        SoundManager.getInstance().playSound(SoundManager.SCAN);
    }

    private void handleLoginSuccess() {
        Toast.makeText(BaseApplication.get(), R.string.web_signin, Toast.LENGTH_LONG).show();
        if (this.activity == null) return;
        this.activity.finish();
    }

    private void handleLoginFailure(final Throwable throwable) {
        LogUtil.exception(getClass(), "Login failure", throwable);
        Toast.makeText(BaseApplication.get(), R.string.error__web_signin, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPaymentRejected() {
        this.activity.finish();
    }

    @Override
    public void onExternalPaymentApproved(final Payment payment) {
        try {
            sendExternalPayment(payment);
        } catch (InvalidQrCodePayment invalidQrCodePayment) {
            handleInvalidQrCode();
        }
    }

    private void sendExternalPayment(final Payment payment) throws InvalidQrCodePayment {
        BaseApplication
                .get()
                .getTokenManager()
                .getTransactionManager()
                .sendExternalPayment(payment.getToAddress(), payment.getValue());
    }

    @Override
    public void onTokenPaymentApproved(final String tokenId, final Payment payment) {
        try {
            goToChatActivityWithPayment(tokenId, payment);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    @Override
    public void onWebPaymentApproved(final String unsignedTransaction) {
        LogUtil.e(getClass(), "Unexpected onWebPaymentApproved call.");
    }

    private void goToChatActivityWithPayment(final String tokenId, final Payment payment) throws InvalidQrCodePayment {
        final Intent intent = new Intent(this.activity, ChatActivity.class)
                .putExtra(ChatActivity.EXTRA__REMOTE_USER_ADDRESS, tokenId)
                .putExtra(ChatActivity.EXTRA__PAYMENT_ACTION, PaymentType.TYPE_SEND)
                .putExtra(ChatActivity.EXTRA__ETH_AMOUNT, payment.getValue())
                .putExtra(ChatActivity.EXTRA__PLAY_SCAN_SOUNDS, true);

        this.activity.startActivity(intent);
        this.activity.finish();
    }

    private void handleInvalidQrCode() {
        SoundManager.getInstance().playSound(SoundManager.SCAN_ERROR);
        this.listener.onInvalidQrCode();
    }

    public void clear() {
        this.subscriptions.clear();
        this.activity = null;
    }
}
