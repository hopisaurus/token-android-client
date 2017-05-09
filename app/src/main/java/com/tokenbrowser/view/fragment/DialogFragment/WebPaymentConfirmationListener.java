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

package com.tokenbrowser.view.fragment.DialogFragment;


import com.tokenbrowser.model.sofa.Payment;
import com.tokenbrowser.util.LogUtil;

public abstract class WebPaymentConfirmationListener implements PaymentConfirmationDialog.OnPaymentConfirmationListener {
    @Override
    public void onPaymentRejected() {
        LogUtil.i(getClass(), "Payment rejected");
    }

    @Override
    public void onTokenPaymentApproved(final String tokenId, final Payment payment) {
        LogUtil.e(getClass(), "Token payment approved but it's not expected; nor is it being handled.");
    }

    @Override
    public void onExternalPaymentApproved(final Payment payment) {
        LogUtil.e(getClass(), "External payment approved but it's not expected; nor is it being handled.");
    }
}
