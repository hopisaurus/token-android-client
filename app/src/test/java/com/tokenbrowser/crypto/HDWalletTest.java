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

package com.tokenbrowser.crypto;


import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HDWalletTest {

    private final String expectedMasterSeed = "easy cart aunt march drive half winner notice wide wagon move drift";
    // These are at path `m/44'/60'/0'/0'
    private final String expectedAddress = "0x0a7dad553f6d31f6e57b51380ebb6fa58ae0af22";

    // Mocks
    private SharedPreferences sharedPreferencesMock;

    @Before
    public void setup() {
        this.sharedPreferencesMock = Mockito.mock(SharedPreferences.class);
        Mockito
                .when(this.sharedPreferencesMock.getString(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(this.expectedMasterSeed);
    }

    @Test
    public void walletCreatedFromSeedUsesThatSeed() {
        final HDWallet wallet =
                new HDWallet(this.sharedPreferencesMock)
                .getExistingWallet()
                .toBlocking()
                .value();
        assertThat(wallet.getMasterSeed(), is(this.expectedMasterSeed));
    }

    @Test
    public void walletCreatedFromSeedDerivesCorrectAddress() {
        final HDWallet wallet =
                new HDWallet(this.sharedPreferencesMock)
                .getExistingWallet()
                .toBlocking()
                .value();
        assertThat(wallet.getOwnerAddress(), is(this.expectedAddress));
    }
}
