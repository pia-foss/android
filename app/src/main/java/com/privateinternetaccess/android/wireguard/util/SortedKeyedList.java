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

package com.privateinternetaccess.android.wireguard.util;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * A keyed list where all elements are sorted by the comparator returned by {@code comparator()}
 * applied to their keys.
 */

public interface SortedKeyedList<K, E extends Keyed<? extends K>> extends KeyedList<K, E> {
    Comparator<? super K> comparator();

    @Nullable
    K firstKey();

    Set<K> keySet();

    @Nullable
    K lastKey();

    Collection<E> values();
}
