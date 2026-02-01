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

package io.github.seriumtw.perms.common.commands.group;

import io.github.seriumtw.perms.common.actionlog.LoggedAction;
import io.github.seriumtw.perms.common.command.abstraction.ChildCommand;
import io.github.seriumtw.perms.common.command.abstraction.CommandException;
import io.github.seriumtw.perms.common.command.access.ArgumentPermissions;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompleter;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompletions;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.command.utils.StorageAssistant;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.node.types.DisplayName;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.util.Predicates;
import io.github.seriumtw.perms.api.context.ImmutableContextSet;
import io.github.seriumtw.perms.api.model.data.DataType;
import io.github.seriumtw.perms.api.node.NodeType;
import io.github.seriumtw.perms.api.node.types.DisplayNameNode;

import java.util.List;

public class GroupSetDisplayName extends ChildCommand<Group> {
    public GroupSetDisplayName() {
        super(CommandSpec.GROUP_SET_DISPLAY_NAME, "setdisplayname", CommandPermission.GROUP_SET_DISPLAY_NAME, Predicates.is(0));
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, Group target, ArgumentList args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String name = args.get(0);
        ImmutableContextSet context = args.getContextOrDefault(1, plugin).immutableCopy();

        if (name.isEmpty()) {
            Message.INVALID_DISPLAY_NAME_EMPTY.send(sender);
            return;
        }

        String previousName = target.normalData().nodesInContext(context).stream()
                .filter(NodeType.DISPLAY_NAME::matches)
                .map(NodeType.DISPLAY_NAME::cast)
                .findFirst()
                .map(DisplayNameNode::getDisplayName)
                .orElse(null);

        if (previousName == null && name.equals(target.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_DOESNT_HAVE.send(sender, target.getName());
            return;
        }

        if (name.equals(previousName)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_HAS.send(sender, target.getName(), name);
            return;
        }

        Group existing = plugin.getGroupManager().getByDisplayName(name);
        if (existing != null && !target.equals(existing)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_IN_USE.send(sender, name, existing.getName());
            return;
        }

        target.removeIf(DataType.NORMAL, context, NodeType.DISPLAY_NAME::matches, false);

        if (name.equals(target.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_REMOVED.send(sender, target.getName(), context);

            LoggedAction.build().source(sender).target(target)
                    .description("setdisplayname", name, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
            return;
        }

        target.setNode(DataType.NORMAL, DisplayName.builder(name).withContext(context).build(), true);

        Message.GROUP_SET_DISPLAY_NAME.send(sender, name, target.getName(), context);

        LoggedAction.build().source(sender).target(target)
                .description("setdisplayname", name, context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(SRMPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
