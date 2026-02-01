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

package io.github.seriumtw.perms.common.tasks;

import io.github.seriumtw.perms.common.cache.BufferedRequest;
import io.github.seriumtw.perms.common.model.manager.group.GroupManager;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.api.event.cause.CreationCause;

import java.util.concurrent.TimeUnit;

/**
 * System wide sync task for SRMPerms.
 *
 * <p>Ensures that all local data is consistent with the storage.</p>
 */
public class SyncTask implements Runnable {
    private final SRMPermsPlugin plugin;

    public SyncTask(SRMPermsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs the update task
     *
     * <p>Called <b>async</b>.</p>
     */
    @Override
    public void run() {
        if (this.plugin.getEventDispatcher().dispatchPreSync(false)) {
            return;
        }

        // Reload all groups
        this.plugin.getStorage().loadAllGroups().join();
        if (!this.plugin.getGroupManager().isLoaded(GroupManager.DEFAULT_GROUP_NAME)) {
            this.plugin.getStorage().createAndLoadGroup(GroupManager.DEFAULT_GROUP_NAME, CreationCause.INTERNAL).join();
        }

        // Reload all tracks
        this.plugin.getStorage().loadAllTracks().join();

        // Reload all online users.
        this.plugin.getUserManager().loadAllUsers().join();

        this.plugin.performPlatformDataSync();

        // Just to be sure...
        this.plugin.getGroupManager().invalidateAllGroupCaches();
        this.plugin.getUserManager().invalidateAllUserCaches();

        this.plugin.getEventDispatcher().dispatchPostSync();
    }

    public static class Buffer extends BufferedRequest<Void> {
        private final SRMPermsPlugin plugin;

        public Buffer(SRMPermsPlugin plugin) {
            super(500L, TimeUnit.MILLISECONDS, plugin.getBootstrap().getScheduler());
            this.plugin = plugin;
        }

        @Override
        protected Void perform() {
            new SyncTask(this.plugin).run();
            return null;
        }
    }
}
