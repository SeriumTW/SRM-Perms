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
import io.github.seriumtw.perms.common.model.HolderType;
import io.github.seriumtw.perms.common.model.PermissionHolder;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.model.manager.group.GroupManager;
import io.github.seriumtw.perms.common.node.types.Inheritance;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.storage.misc.DataConstraints;
import io.github.seriumtw.perms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import io.github.seriumtw.perms.api.context.MutableContextSet;
import io.github.seriumtw.perms.api.model.data.DataMutateResult;
import io.github.seriumtw.perms.api.model.data.DataType;

import java.util.List;

public class ParentRemove extends GenericChildCommand {
    public ParentRemove() {
        super(CommandSpec.PARENT_REMOVE, "remove", CommandPermission.USER_PARENT_REMOVE, CommandPermission.GROUP_PARENT_REMOVE, Predicates.is(0));
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String groupName = args.getLowercase(0, DataConstraints.GROUP_NAME_TEST_ALLOW_SPACE);
        MutableContextSet context = args.getContextOrDefault(1, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, groupName, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, groupName)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (target.getType() == HolderType.USER) {
            User user = (User) target;

            boolean shouldPrevent = plugin.getConfiguration().get(ConfigKeys.PREVENT_PRIMARY_GROUP_REMOVAL) &&
                    context.isEmpty() &&
                    plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD).equals("stored") &&
                    user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME).equalsIgnoreCase(groupName);

            if (shouldPrevent) {
                Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
                return;
            }
        }

        DataMutateResult result = target.unsetNode(DataType.NORMAL, Inheritance.builder(groupName).withContext(context).build());
        if (result.wasSuccessful()) {
            Message.UNSET_INHERIT_SUCCESS.send(sender, target, Component.text(groupName), context);

            LoggedAction.build().source(sender).target(target)
                    .description("parent", "remove", groupName, context)
                    .build().submit(plugin, sender);

            if (target.getType() == HolderType.USER) {
                plugin.getUserManager().giveDefaultIfNeeded((User) target);
            }

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.DOES_NOT_INHERIT.send(sender, target, Component.text(groupName), context);
        }
    }

    @Override
    public List<String> tabComplete(SRMPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
