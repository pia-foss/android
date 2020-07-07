/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.ui.connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GraphFragmentHandlerTest {

    final String[] graphUnits = new String[8];

    @Before
    public void setup(){
        graphUnits[0] = "1";
        graphUnits[1] = "8";
        graphUnits[2] = "1000";
        graphUnits[3] = "8192";

        graphUnits[4] = "1000000";
        graphUnits[5] = "8388608";
        graphUnits[6] = "1000000000";
        graphUnits[7] = "8589934592";
    }

    @Test
    public void getFormattedString_zerobit() {
        String result = GraphFragmentHandler.getFormattedString(0, graphUnits[2], graphUnits);
        Assert.assertTrue(result.equals("0 kbit"));
    }

    @Test
    public void getFormattedString_zeroB() {
        String result = GraphFragmentHandler.getFormattedString(0, graphUnits[3], graphUnits);
        Assert.assertTrue(result.equals("0 kB"));
    }

    @Test
    public void getFormattedString_zeroMB() {
        String result = GraphFragmentHandler.getFormattedString(0, graphUnits[5], graphUnits);
        Assert.assertTrue(result.equals("0 MB"));
    }

    @Test
    public void getFormattedString_zeroGbit() {
        String result = GraphFragmentHandler.getFormattedString(0, graphUnits[6], graphUnits);
        Assert.assertTrue(result.equals("0 Gbit"));
    }

    @Test
    public void getPrefix_kilo1000() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[2]).equals("k"));
    }
    @Test
    public void getPrefix_kilo1024() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[3]).equals("k"));
    }
    @Test
    public void getPrefix_kilo999999() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix("999999").equals("k"));
    }

    @Test
    public void getPrefix_mega1000000() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[4]).equals("M"));
    }
    @Test
    public void getPrefix_mega8388608() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[5]).equals("M"));
    }
    @Test
    public void getPrefix_notMega1000001() {
        Assert.assertFalse(GraphFragmentHandler.getPrefix("1000001").equals("k"));
    }

    @Test
    public void getPrefix_giga1000000000() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[6]).equals("G"));
    }
    @Test
    public void getPrefix_giga8589934592() {
        Assert.assertTrue(GraphFragmentHandler.getPrefix(graphUnits[7]).equals("G"));
    }
    @Test
    public void getPrefix_notGiga999999999() {
        Assert.assertFalse(GraphFragmentHandler.getPrefix("99999999").equals("G"));
    }

    @Test
    public void cleanBytes_zero() {
        Assert.assertTrue(GraphFragmentHandler.cleanBytes(0, graphUnits[0]) == 0f);
    }

    @Test
    public void cleanBytes_thousand() {
        float result = GraphFragmentHandler.cleanBytes(1000, graphUnits[0]);
        Assert.assertTrue(result == 4000f);
    }

    @Test
    public void cleanBytes_thousand_fail() {
        float result = GraphFragmentHandler.cleanBytes(1024, graphUnits[0]);
        Assert.assertFalse(result == 4000f);
    }
}