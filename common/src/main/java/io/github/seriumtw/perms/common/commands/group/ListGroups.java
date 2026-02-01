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

import io.github.seriumtw.perms.common.command.abstraction.SingleCommand;
import io.github.seriumtw.perms.common.command.access.CommandPermission;
import io.github.seriumtw.perms.common.command.spec.CommandSpec;
import io.github.seriumtw.perms.common.command.utils.ArgumentList;
import io.github.seriumtw.perms.common.locale.Message;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.util.Iterators;
import io.github.seriumtw.perms.common.util.Predicates;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ListGroups extends SingleCommand {
    public ListGroups() {
        super(CommandSpec.LIST_GROUPS, "ListGroups", CommandPermission.LIST_GROUPS, Predicates.notInRange(0, 1));
    }

    @Override
    public void execute(SRMPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        try {
            plugin.getStorage().loadAllGroups().get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst loading groups", e);
            Message.GROUPS_LOAD_ERROR.send(sender);
            return;
        }

        int page = args.getIntOrDefault(0, 1);
        int pageIndex = page - 1;

        List<Group> groups = plugin.getGroupManager().getAll().values().stream().sorted((o1, o2) -> {
                    int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                    return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
                }).collect(Collectors.toList());

        List<List<Group>> pages = Iterators.divideIterable(groups, 8);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        Message.SEARCH_SHOWING_GROUPS.send(sender, page, pages.size(), groups.size());
        Message.GROUPS_LIST.send(sender);

        Collection<? extends Track> allTracks = plugin.getTrackManager().getAll().values();

        for (Group group : pages.get(pageIndex)) {
            List<String> tracks = allTracks.stream().filter(t -> t.containsGroup(group)).map(Track::getName).collect(Collectors.toList());
            Message.GROUPS_LIST_ENTRY.send(sender, group, group.getWeight().orElse(0), tracks);
        }
    }
}
