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

package io.github.seriumtw.perms.common.cacheddata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.seriumtw.perms.common.cacheddata.result.StringResult;
import io.github.seriumtw.perms.common.cacheddata.type.SimpleMetaValueSelector;
import io.github.seriumtw.perms.common.cacheddata.type.SimpleMetaValueSelector.Strategy;
import io.github.seriumtw.perms.api.cacheddata.Result;
import io.github.seriumtw.perms.api.node.types.MetaNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetaValueSelectorTest {

    @Test
    public void testStrategies() {
        Map<String, Strategy> strategies = ImmutableMap.of(
                "foo", Strategy.HIGHEST_NUMBER,
                "bar", Strategy.LOWEST_NUMBER
        );
        SimpleMetaValueSelector selector = new SimpleMetaValueSelector(strategies, Strategy.INHERITANCE);

        // empty
        assertThrows(IllegalArgumentException.class, () -> selector.selectValue("hello", ImmutableList.of()));

        Result<String, MetaNode> foo = StringResult.of("foo");

        // single value
        Result<String, MetaNode> value = selector.selectValue("abc", ImmutableList.of(foo));
        assertSame(foo, value);

        // fallback to default when values are not numbers
        value = selector.selectValue("foo", ImmutableList.of(foo));
        assertSame(foo, value);

        Result<String, MetaNode> one = StringResult.of("1");
        Result<String, MetaNode> two = StringResult.of("2");
        Result<String, MetaNode> three = StringResult.of("3");
        ImmutableList<Result<String, MetaNode>> values = ImmutableList.of(two, one, three);

        // first value
        assertSame(two, selector.selectValue("abc", values));

        // highest value
        assertSame(three, selector.selectValue("foo", values));

        // lowest value
        assertSame(one, selector.selectValue("bar", values));

    }

}
