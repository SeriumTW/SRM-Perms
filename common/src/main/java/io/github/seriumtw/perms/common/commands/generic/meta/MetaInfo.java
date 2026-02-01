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

package io.github.seriumtw.perms.common.commands.generic.meta;

import com.google.common.collect.Maps;
import io.github.seriumtw.perms.common.command.abstraction.GenericChildCommand;
import io.github.seriumtw.perms.common.command.access.ArgumentPermissions;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.PermissionHolder;
import io.github.seriumtw.perms.common.node.comparator.NodeWithContextComparator;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.query.QueryOptionsImpl;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.util.Predicates;
import io.github.seriumtw.perms.api.node.Node;
import io.github.seriumtw.perms.api.node.NodeType;
import io.github.seriumtw.perms.api.node.types.ChatMetaNode;
import io.github.seriumtw.perms.api.node.types.MetaNode;
import io.github.seriumtw.perms.api.node.types.PrefixNode;
import io.github.seriumtw.perms.api.node.types.SuffixNode;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MetaInfo extends GenericChildCommand {
    public MetaInfo() {
        super(CommandSpec.META_INFO, "info", CommandPermission.USER_META_INFO, CommandPermission.GROUP_META_INFO, Predicates.alwaysFalse());
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        SortedSet<Map.Entry<Integer, PrefixNode>> prefixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        SortedSet<Map.Entry<Integer, SuffixNode>> suffixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        Set<MetaNode> meta = new LinkedHashSet<>();

        // Collect data
        for (Node node : target.resolveInheritedNodes(NodeType.META_OR_CHAT_META, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL)) {
            if (node instanceof PrefixNode) {
                PrefixNode pn = (PrefixNode) node;
                prefixes.add(Maps.immutableEntry(pn.getPriority(), pn));
            } else if (node instanceof SuffixNode) {
                SuffixNode sn = (SuffixNode) node;
                suffixes.add(Maps.immutableEntry(sn.getPriority(), sn));
            } else if (node instanceof MetaNode) {
                meta.add((MetaNode) node);
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, target);
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, target);
            for (Map.Entry<Integer, PrefixNode> e : prefixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getValue(), target, label);
            }
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, target);
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, target);
            for (Map.Entry<Integer, SuffixNode> e : suffixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getValue(), target, label);
            }
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, target);
        } else {
            Message.META_HEADER.send(sender, target);
            for (MetaNode node : meta) {
                Message.META_ENTRY.send(sender, node, target, label);
            }
        }
    }

    private static final class MetaComparator implements Comparator<Map.Entry<Integer, ? extends ChatMetaNode<?, ?>>> {
        public static final MetaComparator INSTANCE = new MetaComparator();

        @Override
        public int compare(Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o1, Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            if (result != 0) {
                return result;
            }
            return NodeWithContextComparator.normal().compare(o1.getValue(), o2.getValue());
        }
    }
}
