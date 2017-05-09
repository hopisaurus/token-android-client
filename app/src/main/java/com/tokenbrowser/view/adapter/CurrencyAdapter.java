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
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;
import com.tokenbrowser.view.adapter.viewholder.CurrencyViewHolder;

import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<String> currencies;
    private OnItemClickListener<String> listener;

    public CurrencyAdapter(final List<String> currencies) {
        this.currencies = currencies;
    }

    public CurrencyAdapter setOnClickListener(final OnItemClickListener<String> listener) {
        this.listener = listener;
        return this;
    }

    public void addItems(final List<String> currencies) {
        this.currencies.clear();
        this.currencies.add(""); //Add header
        this.currencies.addAll(currencies);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__currency, parent, false);
        return new CurrencyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final String currency = this.currencies.get(position);
        renderItemView(currency, holder);
    }

    private void renderItemView(final String currency, final RecyclerView.ViewHolder holder) {
        final CurrencyViewHolder vh = (CurrencyViewHolder) holder;
        if (currency.length() == 0) {
            final String headerString = BaseApplication.get().getString(R.string.currencies);
            vh.setHeaderText(headerString);
        } else {
            vh
                    .setCurrency(currency)
                    .setOnClickListener(this.listener, currency);
        }
    }

    @Override
    public int getItemCount() {
        return this.currencies.size();
    }
}
