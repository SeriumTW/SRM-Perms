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

package io.github.seriumtw.perms.hytale.context;

import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.github.seriumtw.perms.common.context.manager.SimpleContextManager;
import io.github.seriumtw.perms.hytale.LPHytalePlugin;
import io.github.seriumtw.perms.hytale.service.PlayerVirtualGroupsMap;
import io.github.seriumtw.perms.hytale.service.VirtualGroups;
import io.github.seriumtw.perms.api.query.OptionKey;
import io.github.seriumtw.perms.api.query.QueryOptions;

import java.util.Set;
import java.util.UUID;

public class HytaleContextManager extends SimpleContextManager<PlayerRef, PlayerRef> {
    public static final OptionKey<Boolean> INTEGRATED_SERVER_OWNER = OptionKey.of("integrated_server_owner", Boolean.class);

    private final PlayerVirtualGroupsMap playerVirtualGroupsMap;

    public HytaleContextManager(LPHytalePlugin plugin, PlayerVirtualGroupsMap playerVirtualGroupsMap) {
        super(plugin, PlayerRef.class, PlayerRef.class);
        this.playerVirtualGroupsMap = playerVirtualGroupsMap;
    }

    @Override
    public UUID getUniqueId(PlayerRef player) {
        return player.getUuid();
    }

    @Override
    public void customizeQueryOptions(PlayerRef subject, QueryOptions.Builder builder) {
        if (Constants.SINGLEPLAYER && SingleplayerModule.isOwner(subject)) {
            builder.option(INTEGRATED_SERVER_OWNER, true);
        }

        Set<String> groups = this.playerVirtualGroupsMap.getPlayerGroups(subject.getUuid());
        builder.option(VirtualGroups.KEY, new VirtualGroups(groups));
    }
}
