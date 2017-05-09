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

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.tokenbrowser.R;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;

public class CurrencyViewHolder extends RecyclerView.ViewHolder {
    private TextView textView;
    private float headerTextSize;
    private float itemTextSize;
    private int headerTextColor;
    private int itemTextColor;

    public CurrencyViewHolder(View itemView) {
        super(itemView);
        this.textView = (TextView) itemView;
        this.itemTextSize = BaseApplication.get().getResources().getDimension(R.dimen.list_item_text_size);
        this.headerTextSize = BaseApplication.get().getResources().getDimension(R.dimen.list_header_text_size);
        this.headerTextColor = ContextCompat.getColor(BaseApplication.get(), R.color.textColorSecondary);
        this.itemTextColor = ContextCompat.getColor(BaseApplication.get(), R.color.textColorPrimary);
    }

    public CurrencyViewHolder setCurrency(final String value) {
        this.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.itemTextSize);
        this.textView.setTextColor(this.itemTextColor);
        this.textView.setText(value);
        return this;
    }

    public CurrencyViewHolder setHeaderText(final String value) {
        this.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.headerTextSize);
        this.textView.setTextColor(this.headerTextColor);
        this.textView.setText(value);
        this.textView.setBackground(null);
        return this;
    }

    public CurrencyViewHolder setOnClickListener(final OnItemClickListener listener,
                                                 final String currency) {
        this.itemView.setOnClickListener(__ -> listener.onItemClick(currency));
        return this;
    }
}
