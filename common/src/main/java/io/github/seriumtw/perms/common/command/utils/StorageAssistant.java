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

package io.github.seriumtw.perms.common.command.utils;

import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.messaging.InternalMessagingService;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.HolderType;
import io.github.seriumtw.perms.common.model.PermissionHolder;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility methods for saving users, groups and tracks.
 */
public final class StorageAssistant {
    private StorageAssistant() {}

    public static Group loadGroup(String target, Sender sender, SRMPermsPlugin plugin, boolean auditTemporary) {
        Group group = plugin.getGroupManager().getByDisplayName(target);
        if (group != null) {
            target = group.getName();
        }

        group = plugin.getStorage().loadGroup(target).join().orElse(null);
        if (group == null) {
            Message.GROUP_NOT_FOUND.send(sender, target);
            return null;
        }

        if (auditTemporary) {
            group.auditTemporaryNodes();
        }

        return group;
    }

    public static Track loadTrack(String target, Sender sender, SRMPermsPlugin plugin) {
        Track track = plugin.getStorage().loadTrack(target).join().orElse(null);
        if (track == null) {
            Message.TRACK_NOT_FOUND.send(sender, target);
            return null;
        }

        return track;
    }

    public static void save(User user, Sender sender, SRMPermsPlugin plugin) {
        try {
            plugin.getStorage().saveUser(user).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst saving user", e);
            Message.USER_SAVE_ERROR.send(sender, user);
            return;
        }

        Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
        if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
            messagingService.get().pushUserUpdate(user);
        }
    }

    public static CompletableFuture<Void> save(Group group, Sender sender, SRMPermsPlugin plugin) {
        try {
            plugin.getStorage().saveGroup(group).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst saving group", e);
            Message.GROUP_SAVE_ERROR.send(sender, group);
            return failedFuture(e);
        }

        return invalidateCachesAndPushUpdates(plugin);
    }

    public static CompletableFuture<Void> save(Track track, Sender sender, SRMPermsPlugin plugin) {
        try {
            plugin.getStorage().saveTrack(track).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst saving track", e);
            Message.TRACK_SAVE_ERROR.send(sender, track.getName());
            return failedFuture(e);
        }

        return invalidateCachesAndPushUpdates(plugin);
    }

    public static void save(PermissionHolder holder, Sender sender, SRMPermsPlugin plugin) {
        if (holder.getType() == HolderType.USER) {
            User user = (User) holder;
            save(user, sender, plugin);
        } else if (holder.getType() == HolderType.GROUP) {
            Group group = (Group) holder;
            save(group, sender, plugin);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static CompletableFuture<Void> invalidateCachesAndPushUpdates(SRMPermsPlugin plugin) {
        plugin.getGroupManager().invalidateAllGroupCaches();
        plugin.getUserManager().invalidateAllUserCaches();

        Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
        if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
            return messagingService.get().getUpdateBuffer().request();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }
}
