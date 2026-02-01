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

package io.github.seriumtw.perms.common.commands.generic.parent;

import io.github.seriumtw.perms.common.actionlog.LoggedAction;
import io.github.seriumtw.perms.common.command.abstraction.CommandException;
import io.github.seriumtw.perms.common.command.abstraction.GenericChildCommand;
import io.github.seriumtw.perms.common.command.access.ArgumentPermissions;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompleter;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompletions;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.command.utils.StorageAssistant;
import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.PermissionHolder;
import io.github.seriumtw.perms.common.node.types.Inheritance;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.storage.misc.DataConstraints;
import io.github.seriumtw.perms.common.util.Predicates;
import io.github.seriumtw.perms.api.context.MutableContextSet;
import io.github.seriumtw.perms.api.model.data.DataMutateResult;
import io.github.seriumtw.perms.api.model.data.DataType;
import io.github.seriumtw.perms.api.model.data.TemporaryNodeMergeStrategy;

import java.time.Duration;
import java.util.List;

public class ParentAddTemp extends GenericChildCommand {
    public ParentAddTemp() {
        super(CommandSpec.PARENT_ADD_TEMP, "addtemp", CommandPermission.USER_PARENT_ADD_TEMP, CommandPermission.GROUP_PARENT_ADD_TEMP, Predicates.inRange(0, 1));
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String groupName = args.getLowercase(0, DataConstraints.GROUP_NAME_TEST);
        Duration duration = args.getDuration(1);
        TemporaryNodeMergeStrategy modifier = args.getTemporaryModifierAndRemove(2).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = args.getContextOrDefault(2, plugin);

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, group, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, group.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (group.getName().equalsIgnoreCase(target.getIdentifier().getName())) {
            Message.ALREADY_TEMP_INHERITS.send(sender, target, group, context);
            return;
        }

        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, Inheritance.builder(group.getName()).expiry(duration).withContext(context).build(), modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiryDuration();
            Message.SET_TEMP_INHERIT_SUCCESS.send(sender, target, group, duration, context);

            LoggedAction.build().source(sender).target(target)
                    .description("parent", "addtemp", group.getName(), duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.ALREADY_TEMP_INHERITS.send(sender, target, group, context);
        }
    }

    @Override
    public List<String> tabComplete(SRMPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
