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

package com.tokenbrowser.presenter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.tokenbrowser.model.network.Currencies;
import com.tokenbrowser.model.network.Currency;
import com.tokenbrowser.util.LogUtil;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.CurrencyActivity;
import com.tokenbrowser.view.adapter.CurrencyAdapter;

import java.util.ArrayList;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class CurrencyPresenter implements Presenter<CurrencyActivity> {

    private CurrencyActivity activity;
    private CompositeSubscription subscriptions;
    private boolean firstTimeAttaching = true;

    @Override
    public void onViewAttached(CurrencyActivity view) {
        this.activity = view;

        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        initShortLivingObjects();
    }

    private void initLongLivingObjects() {
        this.subscriptions = new CompositeSubscription();
    }

    private void initShortLivingObjects() {
        initRecyclerView();
        initClickListeners();
        getCurrencies();
    }

    private void initRecyclerView() {
        final RecyclerView recyclerView = this.activity.getBinding().recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity));
        final CurrencyAdapter adapter = new CurrencyAdapter(new ArrayList<>())
                .setOnClickListener(this::handleCurrencyClicked);
        recyclerView.setAdapter(adapter);
    }

    private void initClickListeners() {
        this.activity.getBinding().closeButton.setOnClickListener(__ -> this.activity.finish());
    }

    private void handleCurrencyClicked(final Currency currency) {}

    private void getCurrencies() {
        final Subscription sub =
                BaseApplication
                .get()
                .getTokenManager()
                .getBalanceManager()
                .getCurrencies()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleCurrencies,
                        this::handleError
                );

        this.subscriptions.add(sub);
    }

    private void handleCurrencies(final Currencies currencies) {
        if (this.activity == null) return;
        final CurrencyAdapter adapter = (CurrencyAdapter) this.activity.getBinding().recyclerView.getAdapter();
        adapter.addItems(currencies.getData());
    }

    private void handleError(final Throwable throwable) {
        LogUtil.exception(getClass(), throwable);
    }

    @Override
    public void onViewDetached() {
        this.subscriptions.clear();
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.subscriptions = null;
        this.activity = null;
    }
}
