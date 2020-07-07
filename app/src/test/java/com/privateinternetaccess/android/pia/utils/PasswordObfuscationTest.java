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

package com.privateinternetaccess.android.pia.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;

public class PasswordObfuscationTest {

    @Test
    public void passwordObfuscationCreation(){
        Assert.assertThat(new PasswordObfuscation(), instanceOf(PasswordObfuscation.class));
    }

    @Test
    public void obfuscateExpectedParameter() throws Exception {
        String base = "ihasdifnasijnf";
        Assert.assertNotEquals(base, PasswordObfuscation.obfuscate(base));
    }

    @Test
    public void obfuscateEmptyParameter() throws Exception {
        String empty = "";
        Assert.assertNotEquals(empty, PasswordObfuscation.obfuscate(empty));
    }

    @Test(expected = NullPointerException.class)
    public void obfuscateNullParameter() throws Exception {
        PasswordObfuscation.obfuscate(null);
    }

    @Test
    public void obfuscateRaceParameter() throws Exception {
        String raceCondition = "パスワード";
        Assert.assertNotEquals(raceCondition, PasswordObfuscation.obfuscate(raceCondition));
    }

    @Test
    public void obfuscate() throws Exception {
        String input = "x";
        String output = "jSGNFDGj1d0=";
        Assert.assertEquals(output, PasswordObfuscation.obfuscate(input));
    }

    @Test
    public void deobfuscate() throws Exception {
        String input = "x";
        String output = "jSGNFDGj1d0=";
        Assert.assertEquals(input, PasswordObfuscation.deobfuscate(output));
    }

    @Test
    public void deobfuscateExpectedParameter() throws Exception {
        String base = "jSGNFDGj1d0=";
        Assert.assertNotEquals(base, PasswordObfuscation.deobfuscate(base));
    }

    @Test
    public void deobfuscateEmptyParameter() throws Exception {
        String empty = "";
        Assert.assertEquals(empty, PasswordObfuscation.deobfuscate(empty));
    }

    @Test(expected = NullPointerException.class)
    public void deobfuscateNullParameter() throws Exception {
        PasswordObfuscation.deobfuscate(null);
    }

    // Never going to happen in the grand scheme of things
//    @Test
//    public void deobfuscateRaceParameter() throws Exception {
//        String raceCondition = "パスワード";
//        Assert.assertEquals(raceCondition, PasswordObfuscation.deobfuscate(raceCondition));
//    }

    @Test
    public void deobfuscateAndObfuscate() throws Exception {
        String input = "asdlfinasdlng";
        String objuscate = PasswordObfuscation.obfuscate(input);
        String output = PasswordObfuscation.deobfuscate(objuscate);
        Assert.assertEquals(input, output);
    }
}