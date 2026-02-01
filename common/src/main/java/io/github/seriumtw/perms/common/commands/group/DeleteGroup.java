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
import io.github.seriumtw.perms.common.bulkupdate.BulkUpdate;
import io.github.seriumtw.perms.common.bulkupdate.BulkUpdateBuilder;
import io.github.seriumtw.perms.common.bulkupdate.BulkUpdateField;
import io.github.seriumtw.perms.common.bulkupdate.DataType;
import io.github.seriumtw.perms.common.bulkupdate.action.DeleteAction;
import io.github.seriumtw.perms.common.command.abstraction.SingleCommand;
import io.github.seriumtw.perms.common.command.access.ArgumentPermissions;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.tabcomplete.CompletionSupplier;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompleter;
import io.github.seriumtw.perms.common.command.tabcomplete.TabCompletions;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.filter.Comparison;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.messaging.InternalMessagingService;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.manager.group.GroupManager;
import io.github.seriumtw.perms.common.node.types.Inheritance;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.storage.misc.DataConstraints;
import io.github.seriumtw.perms.common.util.Predicates;
import io.github.seriumtw.perms.api.actionlog.Action;
import io.github.seriumtw.perms.api.event.cause.DeletionCause;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DeleteGroup extends SingleCommand {
    public DeleteGroup() {
        super(CommandSpec.DELETE_GROUP, "DeleteGroup", CommandPermission.DELETE_GROUP, Predicates.notInRange(1, 2));
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return;
        }

        String groupName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender, groupName);
            return;
        }

        if (groupName.equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
            Message.DELETE_GROUP_ERROR_DEFAULT.send(sender);
            return;
        }

        Group group = plugin.getStorage().loadGroup(groupName).join().orElse(null);
        if (group == null) {
            Message.GROUP_LOAD_ERROR.send(sender);
            return;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), group)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        try {
            plugin.getStorage().deleteGroup(group, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst deleting group", e);
            Message.DELETE_ERROR.send(sender, group.getFormattedDisplayName());
            return;
        }

        Message.DELETE_SUCCESS.send(sender, group.getFormattedDisplayName());

        LoggedAction.build().source(sender).targetName(groupName).targetType(Action.Target.Type.GROUP)
                .description("delete")
                .build().submit(plugin, sender);

        if (!args.remove("--update-parent-lists")) {
            plugin.getSyncTaskBuffer().request();
        } else {
            // the group is now deleted, proceed to remove its representing inheritance nodes
            BulkUpdate operation = BulkUpdateBuilder.create()
                    .trackStatistics(false)
                    .dataType(DataType.ALL)
                    .action(DeleteAction.create())
                    .filter(BulkUpdateField.PERMISSION, Comparison.EQUAL, Inheritance.key(groupName))
                    .build();
            plugin.getStorage().applyBulkUpdate(operation).whenCompleteAsync((v, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                }

                plugin.getSyncTaskBuffer().requestDirectly();   // sync regardless of failure state
                Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
                if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
                    messagingService.get().getUpdateBuffer().request();
                }
            }, plugin.getBootstrap().getScheduler().async());
        }
    }

    @Override
    public List<String> tabComplete(SRMPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .at(1, CompletionSupplier.startsWith("--update-parent-lists"))
                .complete(args);
    }
}
