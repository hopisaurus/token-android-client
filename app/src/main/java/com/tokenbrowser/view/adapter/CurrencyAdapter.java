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

package com.tokenbrowser.view.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tokenbrowser.R;
import com.tokenbrowser.model.network.Currency;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;
import com.tokenbrowser.view.adapter.viewholder.CurrencyHeaderViewHolder;
import com.tokenbrowser.view.adapter.viewholder.CurrencyViewHolder;

import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM = 1;
    private static final int HEADER = 2;

    public List<Currency> currencies;
    private OnItemClickListener<Currency> listener;

    public CurrencyAdapter(final List<Currency> currencies) {
        this.currencies = currencies;
    }

    public CurrencyAdapter setOnClickListener(final OnItemClickListener<Currency> listener) {
        this.listener = listener;
        return this;
    }

    public void addItems(final List<Currency> currencies) {
        this.currencies.clear();
        this.currencies.addAll(currencies);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case HEADER: {
                final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__currency_header, parent, false);
                return new CurrencyHeaderViewHolder(v);
            }
            case ITEM:
            default: {
                final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__currency, parent, false);
                return new CurrencyViewHolder(v);
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case HEADER: {
                renderHeaderView(holder);
                break;
            }
            case ITEM:
            default: {
                final Currency currency = this.currencies.get(position - 1); // - 1 because of the header
                renderItemView(currency, holder);
                break;
            }
        }
    }

    private void renderHeaderView(final RecyclerView.ViewHolder holder) {
        final CurrencyHeaderViewHolder vh = (CurrencyHeaderViewHolder) holder;
        final String headerText = BaseApplication.get().getString(R.string.currencies);
        vh.setText(headerText);
    }

    private void renderItemView(final Currency currency, final RecyclerView.ViewHolder holder) {
        final CurrencyViewHolder vh = (CurrencyViewHolder) holder;
        vh
                .setCurrency(currency)
                .setOnClickListener(this.listener, currency);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER : ITEM;
    }

    @Override
    public int getItemCount() {
        return this.currencies.size() + 1; // + 1 because of the header
    }
}
