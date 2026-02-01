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

package io.github.seriumtw.perms.common.commands.user;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import io.github.seriumtw.perms.common.command.abstraction.Command;
import io.github.seriumtw.perms.common.command.abstraction.ParentCommand;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.commands.generic.meta.CommandMeta;
import io.github.seriumtw.perms.common.commands.generic.other.HolderClear;
import io.github.seriumtw.perms.common.commands.generic.other.HolderEditor;
import io.github.seriumtw.perms.common.commands.generic.other.HolderShowTracks;
import io.github.seriumtw.perms.common.commands.generic.parent.CommandParent;
import io.github.seriumtw.perms.common.commands.generic.permission.CommandPermission;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.HolderType;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.model.UserIdentifier;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.util.CaffeineFactory;
import io.github.seriumtw.perms.common.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class UserParentCommand extends ParentCommand<User, UserIdentifier> {

    // we use a lock per unique user
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<UUID, ReentrantLock> locks = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public UserParentCommand() {
        super(CommandSpec.USER, "User", Type.TARGETED, ImmutableList.<Command<User>>builder()
                .add(new UserInfo())
                .add(new CommandPermission<>(HolderType.USER))
                .add(new CommandParent<>(HolderType.USER))
                .add(new CommandMeta<>(HolderType.USER))
                .add(new HolderEditor<>(HolderType.USER))
                .add(new UserPromote())
                .add(new UserDemote())
                .add(new HolderShowTracks<>(HolderType.USER))
                .add(new HolderClear<>(HolderType.USER))
                .add(new UserClone())
                .build()
        );
    }

    public static UUID parseTargetUniqueId(String target, SRMPermsPlugin plugin, Sender sender) {
        UUID parsed = Uuids.parse(target);
        if (parsed != null) {
            return parsed;
        }

        if (!plugin.testUsernameValidity(target)) {
            Message.USER_INVALID_ENTRY.send(sender, target);
            return null;
        }

        UUID lookup = plugin.lookupUniqueId(target).orElse(null);
        if (lookup == null) {
            Message.USER_NOT_FOUND.send(sender, target);
            return null;
        }

        return lookup;
    }

    @Override
    protected UserIdentifier parseTarget(String target, SRMPermsPlugin plugin, Sender sender) {
        UUID uniqueId = parseTargetUniqueId(target, plugin, sender);
        if (uniqueId == null) {
            return null;
        }

        String name = plugin.getStorage().getPlayerName(uniqueId).join();
        return UserIdentifier.of(uniqueId, name);
    }

    @Override
    protected User getTarget(UserIdentifier target, SRMPermsPlugin plugin, Sender sender) {
        User user = plugin.getStorage().loadUser(target.getUniqueId(), target.getUsername().orElse(null)).join();
        user.auditTemporaryNodes();
        return user;
    }

    @Override
    protected ReentrantLock getLockForTarget(UserIdentifier target) {
        return this.locks.get(target.getUniqueId());
    }

    @Override
    protected void cleanup(User user, SRMPermsPlugin plugin) {
        plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
    }

    @Override
    protected List<String> getTargets(SRMPermsPlugin plugin) {
        return new ArrayList<>(plugin.getBootstrap().getPlayerList());
    }
}