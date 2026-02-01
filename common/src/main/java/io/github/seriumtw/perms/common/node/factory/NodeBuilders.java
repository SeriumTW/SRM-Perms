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

package io.github.seriumtw.perms.common.node.factory;

import io.github.seriumtw.perms.common.node.types.DisplayName;
import io.github.seriumtw.perms.common.node.types.Inheritance;
import io.github.seriumtw.perms.common.node.types.Meta;
import io.github.seriumtw.perms.common.node.types.Permission;
import io.github.seriumtw.perms.common.node.types.Prefix;
import io.github.seriumtw.perms.common.node.types.RegexPermission;
import io.github.seriumtw.perms.common.node.types.Suffix;
import io.github.seriumtw.perms.common.node.types.Weight;
import io.github.seriumtw.perms.api.node.NodeBuilder;
import io.github.seriumtw.perms.api.node.types.DisplayNameNode;
import io.github.seriumtw.perms.api.node.types.InheritanceNode;
import io.github.seriumtw.perms.api.node.types.MetaNode;
import io.github.seriumtw.perms.api.node.types.PrefixNode;
import io.github.seriumtw.perms.api.node.types.RegexPermissionNode;
import io.github.seriumtw.perms.api.node.types.SuffixNode;
import io.github.seriumtw.perms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public final class NodeBuilders {
    private NodeBuilders() {}

    private static final Parser<InheritanceNode.Builder> INHERITANCE = Inheritance::parse;
    private static final Parser<PrefixNode.Builder> PREFIX = Prefix::parse;
    private static final Parser<SuffixNode.Builder> SUFFIX = Suffix::parse;
    private static final Parser<MetaNode.Builder> META = Meta::parse;
    private static final Parser<WeightNode.Builder> WEIGHT = Weight::parse;
    private static final Parser<DisplayNameNode.Builder> DISPLAY_NAME = DisplayName::parse;
    private static final Parser<RegexPermissionNode.Builder> REGEX_PERMISSION = RegexPermission::parse;

    private static final Parser<?>[] PARSERS = new Parser[]{INHERITANCE, PREFIX, SUFFIX, META, WEIGHT, DISPLAY_NAME, REGEX_PERMISSION};

    public static @NonNull NodeBuilder<?, ?> determineMostApplicable(String key) {
        Objects.requireNonNull(key, "key");
        for (Parser<?> parser : PARSERS) {
            NodeBuilder<?, ?> builder = parser.parse(key);
            if (builder != null) {
                return builder;
            }
        }
        return Permission.builder().permission(key);
    }

    private interface Parser<B extends NodeBuilder<?, B>> {
        @Nullable B parse(String s);
    }

}
