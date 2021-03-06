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

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Patterns;
import android.view.View;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.tokenbrowser.model.local.Dapp;
import com.tokenbrowser.model.network.App;
import com.tokenbrowser.util.LogUtil;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.ViewUserActivity;
import com.tokenbrowser.view.activity.WebViewActivity;
import com.tokenbrowser.view.adapter.RecommendedAppsAdapter;
import com.tokenbrowser.view.adapter.SearchAppAdapter;
import com.tokenbrowser.view.fragment.toplevel.AppsFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class AppsPresenter implements Presenter<AppsFragment>{

    private List<App> apps;
    private AppsFragment fragment;
    private CompositeSubscription subscriptions;
    private boolean firstTimeAttaching = true;
    private SearchAppAdapter searchAppAdapter;

    @Override
    public void onViewAttached(AppsFragment view) {
        this.fragment = view;

        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        initView();
        checkIfAppsRequestIsNeeded();
    }

    private void initLongLivingObjects() {
        this.subscriptions = new CompositeSubscription();
          initAdapter();
    }

    private void initAdapter() {
        this.searchAppAdapter = new SearchAppAdapter(new ArrayList<>());
        this.searchAppAdapter.setOnItemClickListener(this::handleAppClicked);
        this.searchAppAdapter.setOnDappLaunchListener(this::handleDappLaunch);

        tryRerunQuery();
    }

    private void tryRerunQuery() {
        final String searchQuery = this.fragment.getBinding().search.getText().toString();
        if (searchQuery.length() > 0) {
            runSearchQuery(searchQuery);
        }
    }

    private void initView() {
        initRecyclerViews();
        initSearchView();
    }

    private void checkIfAppsRequestIsNeeded() {
        if (this.apps != null) {
            addAppsData(this.apps);
        } else {
            requestAppData();
        }
    }

    private void initRecyclerViews() {
        final RecyclerView recommendedApps = this.fragment.getBinding().recyclerViewRecommendedApps;
        recommendedApps.setLayoutManager(new LinearLayoutManager(this.fragment.getContext(), LinearLayoutManager.HORIZONTAL, false));
        final RecommendedAppsAdapter recommendedAppsAdapter = new RecommendedAppsAdapter(new ArrayList<>());
        recommendedApps.setAdapter(recommendedAppsAdapter);
        recommendedApps.setNestedScrollingEnabled(false);
        recommendedAppsAdapter.setOnItemClickListener(this::handleAppClicked);

        final RecyclerView filteredApps = this.fragment.getBinding().searchList;
        filteredApps.setLayoutManager(new LinearLayoutManager(this.fragment.getContext()));
        filteredApps.setAdapter(this.searchAppAdapter);
    }

    private void handleAppClicked(final App app) {
        final Intent intent = new Intent(this.fragment.getContext(), ViewUserActivity.class)
                .putExtra(ViewUserActivity.EXTRA__USER_ADDRESS, app.getTokenId());
        this.fragment.getContext().startActivity(intent);
    }

    private void handleDappLaunch(final Dapp dapp) {
        final Intent intent = new Intent(this.fragment.getContext(), WebViewActivity.class)
                .putExtra(WebViewActivity.EXTRA__ADDRESS, dapp.getAddress());
        this.fragment.getContext().startActivity(intent);
    }

    private void initSearchView() {
        final Subscription searchSub =
                RxTextView.textChanges(this.fragment.getBinding().search)
                .skip(1)
                .debounce(400, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .subscribe(this::runSearchQuery);

        final Subscription enterSub =
                RxTextView.editorActions(this.fragment.getBinding().search)
                        .filter(event -> event == IME_ACTION_DONE)
                        .subscribe(
                                __ -> this.handleSearchPressed(),
                                t -> LogUtil.e(getClass(), t.toString()));

        updateViewState();

        this.subscriptions.add(searchSub);
        this.subscriptions.add(enterSub);
    }

    private void runSearchQuery(final String query) {
        final Subscription sub =
            Observable.just(query)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(searchString -> updateViewState())
                .doOnNext(this::tryRenderDappLink)
                .observeOn(Schedulers.io())
                .flatMap(this::searchApps)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleAppSearchResponse,
                        this::handleAppSearchError
                );
        this.subscriptions.add(sub);
    }

    private void handleSearchPressed() {
        if (this.searchAppAdapter == null || this.searchAppAdapter.getNumberOfApps() != 1) return;
        final App appToLaunch = this.searchAppAdapter.getFirstApp();
        if (appToLaunch == null) return;
        if (appToLaunch instanceof Dapp) {
            this.handleDappLaunch((Dapp) appToLaunch);
        }

    }

    private Observable<List<App>> searchApps(final String searchString) {
        return BaseApplication
                .get()
                .getTokenManager()
                .getAppsManager()
                .searchApps(searchString);
    }

    private void handleAppSearchError(final Throwable throwable) {
        LogUtil.exception(getClass(), "Error while searching for app", throwable);
    }

    private void updateViewState() {
        final boolean shouldShowSearchResult = this.fragment.getBinding().search.getText().toString().length() > 0;

        if (shouldShowSearchResult) {
            this.fragment.getBinding().searchList.setVisibility(View.VISIBLE);
            this.fragment.getBinding().scrollView.setVisibility(View.GONE);
        } else {
            this.fragment.getBinding().searchList.setVisibility(View.GONE);
            this.fragment.getBinding().scrollView.setVisibility(View.VISIBLE);
        }
    }

    private void tryRenderDappLink(final String searchString) {
        if (!Patterns.WEB_URL.matcher(searchString.trim()).matches()) {
            this.searchAppAdapter.removeDapp();
            return;
        }

        this.searchAppAdapter.addDapp(searchString);
    }

    private void handleAppSearchResponse(final List<App> apps) {
        this.searchAppAdapter.addItems(apps);
    }

    private void requestAppData() {
        final Subscription sub =
                BaseApplication
                .get()
                .getTokenManager()
                .getAppsManager()
                .getFeaturedApps()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::addAppsData,
                        this::handleRecommendedAppsErrorResponse
                );

        this.subscriptions.add(sub);
    }

    private void handleRecommendedAppsErrorResponse(final Throwable throwable) {
        LogUtil.exception(getClass(), "Error while fetching recommended apps", throwable);
    }

    private void addAppsData(final List<App> apps) {
        this.apps = apps;
        final RecommendedAppsAdapter adapter = (RecommendedAppsAdapter) this.fragment.getBinding().recyclerViewRecommendedApps.getAdapter();
        adapter.setItems(apps);
    }

    @Override
    public void onViewDetached() {
        this.fragment = null;
        this.subscriptions.clear();
    }

    @Override
    public void onDestroyed() {
        this.fragment = null;
    }
}
