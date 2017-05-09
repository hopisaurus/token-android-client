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

package com.tokenbrowser.view.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.tokenbrowser.model.network.Currency;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;

public class CurrencyViewHolder extends RecyclerView.ViewHolder {
    private TextView currency;

    public CurrencyViewHolder(View itemView) {
        super(itemView);
        this.currency = (TextView) itemView;
    }

    public CurrencyViewHolder setCurrency(final Currency currency) {
        this.currency.setText(String.format("%s (%s)", currency.getName(), currency.getId()));
        return this;
    }

    public CurrencyViewHolder setOnClickListener(final OnItemClickListener listener,
                                                 final Currency currency) {
        this.itemView.setOnClickListener(__ -> listener.onItemClick(currency));
        return this;
    }
}
