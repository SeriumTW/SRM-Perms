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

package io.github.seriumtw.perms.common.commands.log;

import io.github.seriumtw.perms.common.command.abstraction.ChildCommand;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.context.ImmutableContextSetImpl;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.node.types.Permission;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.util.Predicates;
import io.github.seriumtw.perms.api.model.data.DataType;
import io.github.seriumtw.perms.api.node.Node;

import java.util.Optional;
import java.util.UUID;

public class LogNotify extends ChildCommand<Void> {
    private static final String IGNORE_NODE = "srmperms.log.notify.ignoring";

    public LogNotify() {
        super(CommandSpec.LOG_NOTIFY, "notify", CommandPermission.LOG_NOTIFY, Predicates.notInRange(0, 1));
    }

    public static boolean isIgnoring(SRMPermsPlugin plugin, UUID uuid) {
        User user = plugin.getUserManager().getIfLoaded(uuid);
        if (user == null) {
            return false;
        }

        Optional<? extends Node> node = user.normalData().nodesInContext(ImmutableContextSetImpl.EMPTY).stream()
                .filter(n -> n.getKey().equalsIgnoreCase(IGNORE_NODE))
                .findFirst();

        // if they don't have the perm, they're not ignoring
        // if set to false, ignore it, return false
        return node.map(Node::getValue).orElse(false);
    }

    private static void setIgnoring(SRMPermsPlugin plugin, UUID uuid, boolean state) {
        User user = plugin.getUserManager().getIfLoaded(uuid);
        if (user == null) {
            return;
        }

        if (state) {
            // add the perm
            user.setNode(DataType.NORMAL, Permission.builder().permission(IGNORE_NODE).build(), true);
        } else {
            // remove the perm
            user.removeIf(DataType.NORMAL, ImmutableContextSetImpl.EMPTY, n -> n.getKey().equalsIgnoreCase(IGNORE_NODE), false);
        }

        plugin.getStorage().saveUser(user).join();
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, Void ignored, ArgumentList args, String label) {
        if (sender.isConsole()) {
            Message.LOG_NOTIFY_CONSOLE.send(sender);
            return;
        }

        final UUID uuid = sender.getUniqueId();
        if (args.isEmpty()) {
            if (isIgnoring(plugin, uuid)) {
                // toggle on
                setIgnoring(plugin, uuid, false);
                Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
                return;
            }
            // toggle off
            setIgnoring(plugin, uuid, true);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return;
        }

        if (args.get(0).equalsIgnoreCase("on")) {
            if (!isIgnoring(plugin, uuid)) {
                // already on
                Message.LOG_NOTIFY_ALREADY_ON.send(sender);
                return;
            }

            // toggle on
            setIgnoring(plugin, uuid, false);
            Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
            return;
        }

        if (args.get(0).equalsIgnoreCase("off")) {
            if (isIgnoring(plugin, uuid)) {
                // already off
                Message.LOG_NOTIFY_ALREADY_OFF.send(sender);
                return;
            }

            // toggle off
            setIgnoring(plugin, uuid, true);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return;
        }

        // not recognised
        Message.LOG_NOTIFY_UNKNOWN.send(sender);
    }
}
