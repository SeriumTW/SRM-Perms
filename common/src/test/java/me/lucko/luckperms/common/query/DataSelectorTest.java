/*
 * This file is part of SRMPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.seriumtw.perms.common.query;

import io.github.seriumtw.perms.common.model.HolderType;
import io.github.seriumtw.perms.common.model.PermissionHolderIdentifier;
import io.github.seriumtw.perms.api.model.data.DataType;
import io.github.seriumtw.perms.api.query.QueryMode;
import io.github.seriumtw.perms.api.query.QueryOptions;
import io.github.seriumtw.perms.api.query.dataorder.DataQueryOrder;
import io.github.seriumtw.perms.api.query.dataorder.DataQueryOrderFunction;
import io.github.seriumtw.perms.api.query.dataorder.DataTypeFilter;
import io.github.seriumtw.perms.api.query.dataorder.DataTypeFilterFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DataSelectorTest {
    private static final PermissionHolderIdentifier IDENTIFIER = new PermissionHolderIdentifier(HolderType.USER, "Notch");

    @Test
    public void testDefault() {
        DataType[] types = DataSelector.selectOrder(QueryOptionsImpl.DEFAULT_CONTEXTUAL, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT, DataType.NORMAL}, types);
    }

    @Test
    public void testOrdering() {
        QueryOptions transientFirst = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataQueryOrderFunction.KEY, DataQueryOrderFunction.always(DataQueryOrder.TRANSIENT_FIRST))
                .build();

        QueryOptions transientLast = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataQueryOrderFunction.KEY, DataQueryOrderFunction.always(DataQueryOrder.TRANSIENT_LAST))
                .build();

        DataType[] types = DataSelector.selectOrder(transientFirst, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT, DataType.NORMAL}, types);

        types = DataSelector.selectOrder(transientLast, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.NORMAL, DataType.TRANSIENT}, types);
    }

    @Test
    public void testSelection() {
        QueryOptions normalOnly = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataTypeFilterFunction.KEY, DataTypeFilterFunction.always(DataTypeFilter.NORMAL_ONLY))
                .build();

        QueryOptions transientOnly = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataTypeFilterFunction.KEY, DataTypeFilterFunction.always(DataTypeFilter.TRANSIENT_ONLY))
                .build();

        DataType[] types = DataSelector.selectOrder(normalOnly, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.NORMAL}, types);

        types = DataSelector.selectOrder(transientOnly, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT}, types);
    }

}
