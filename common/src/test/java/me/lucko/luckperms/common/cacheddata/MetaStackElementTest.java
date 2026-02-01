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

package io.github.seriumtw.perms.common.cacheddata;

import io.github.seriumtw.perms.common.cacheddata.metastack.StandardStackElements;
import io.github.seriumtw.perms.common.model.HolderType;
import io.github.seriumtw.perms.common.model.InheritanceOrigin;
import io.github.seriumtw.perms.common.model.PermissionHolderIdentifier;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.model.manager.track.StandardTrackManager;
import io.github.seriumtw.perms.common.model.manager.track.TrackManager;
import io.github.seriumtw.perms.common.node.types.Prefix;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.api.metastacking.MetaStackElement;
import io.github.seriumtw.perms.api.model.data.DataType;
import io.github.seriumtw.perms.api.node.ChatMetaType;
import io.github.seriumtw.perms.api.node.metadata.types.InheritanceOriginMetadata;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetaStackElementTest {

    @Test
    public void testHighest() {
        MetaStackElement highest = StandardStackElements.HIGHEST;
        assertTrue(highest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).build(), // node
                null // current
        ));
        assertFalse(highest.shouldAccumulate(
                ChatMetaType.SUFFIX,
                Prefix.builder("foo", 100).build(), // node
                null // current
        ));
        assertTrue(highest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).build(), // node
                Prefix.builder("bar", 50).build()) // current
        );
        assertFalse(highest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 50).build(), // node
                Prefix.builder("bar", 100).build()) // current
        );
    }

    @Test
    public void testLowest() {
        MetaStackElement lowest = StandardStackElements.LOWEST;
        assertTrue(lowest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).build(), // node
                null // current
        ));
        assertFalse(lowest.shouldAccumulate(
                ChatMetaType.SUFFIX,
                Prefix.builder("foo", 100).build(), // node
                null // current
        ));
        assertTrue(lowest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 50).build(), // node
                Prefix.builder("bar", 100).build()) // current
        );
        assertFalse(lowest.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).build(), // node
                Prefix.builder("bar", 50).build()) // current
        );
    }

    @Test
    public void testHighestOwn() {
        InheritanceOrigin userOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.USER, ""), DataType.NORMAL);
        InheritanceOrigin groupOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, ""), DataType.NORMAL);

        MetaStackElement highestOwn = StandardStackElements.HIGHEST_OWN;
        assertTrue(highestOwn.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, userOrigin).build(), // node
                null // current
        ));
        assertFalse(highestOwn.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, groupOrigin).build(), // node
                null // current
        ));
    }

    @Test
    public void testHighestInherited() {
        InheritanceOrigin userOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.USER, ""), DataType.NORMAL);
        InheritanceOrigin groupOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, ""), DataType.NORMAL);

        MetaStackElement highestInherited = StandardStackElements.HIGHEST_INHERITED;
        assertTrue(highestInherited.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, groupOrigin).build(), // node
                null // current
        ));
        assertFalse(highestInherited.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, userOrigin).build(), // node
                null // current
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testHighestFromGroupOnTrack() {
        SRMPermsPlugin plugin = mock(SRMPermsPlugin.class);
        TrackManager<Track> trackManager = new StandardTrackManager(plugin);
        when(plugin.getTrackManager()).thenReturn((TrackManager) trackManager);

        Track track = trackManager.getOrMake("test");
        track.setGroups(ImmutableList.of("foo", "bar"));

        InheritanceOrigin fooOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "foo"), DataType.NORMAL);
        InheritanceOrigin bazOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "baz"), DataType.NORMAL);

        MetaStackElement highestFromGroupOnTrack = StandardStackElements.highestFromGroupOnTrack(plugin, "test");
        assertTrue(highestFromGroupOnTrack.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, fooOrigin).build(), // node
                null // current
        ));
        assertFalse(highestFromGroupOnTrack.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, bazOrigin).build(), // node
                null // current
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testHighestNotFromGroupOnTrack() {
        SRMPermsPlugin plugin = mock(SRMPermsPlugin.class);
        TrackManager<Track> trackManager = new StandardTrackManager(plugin);
        when(plugin.getTrackManager()).thenReturn((TrackManager) trackManager);

        Track track = trackManager.getOrMake("test");
        track.setGroups(ImmutableList.of("foo", "bar"));

        InheritanceOrigin fooOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "foo"), DataType.NORMAL);
        InheritanceOrigin bazOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "baz"), DataType.NORMAL);
        InheritanceOrigin userOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.USER, "foo"), DataType.NORMAL);

        MetaStackElement highestNotFromGroupOnTrack = StandardStackElements.highestNotFromGroupOnTrack(plugin, "test");
        assertTrue(highestNotFromGroupOnTrack.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, userOrigin).build(), // node
                null // current
        ));
        assertTrue(highestNotFromGroupOnTrack.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, bazOrigin).build(), // node
                null // current
        ));
        assertFalse(highestNotFromGroupOnTrack.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, fooOrigin).build(), // node
                null // current
        ));
    }

    @Test
    public void testHighestFromGroup() {
        InheritanceOrigin fooOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "foo"), DataType.NORMAL);
        InheritanceOrigin bazOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "baz"), DataType.NORMAL);
        InheritanceOrigin userOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.USER, "foo"), DataType.NORMAL);

        MetaStackElement highestFromGroup = StandardStackElements.highestFromGroup("foo");
        assertTrue(highestFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, fooOrigin).build(), // node
                null // current
        ));
        assertFalse(highestFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, bazOrigin).build(), // node
                null // current
        ));
        assertFalse(highestFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, userOrigin).build(), // node
                null // current
        ));
    }

    @Test
    public void testHighestNotFromGroup() {
        InheritanceOrigin fooOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "foo"), DataType.NORMAL);
        InheritanceOrigin bazOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.GROUP, "baz"), DataType.NORMAL);
        InheritanceOrigin userOrigin = new InheritanceOrigin(new PermissionHolderIdentifier(HolderType.USER, "foo"), DataType.NORMAL);

        MetaStackElement highestNotFromGroup = StandardStackElements.highestNotFromGroup("foo");
        assertTrue(highestNotFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, userOrigin).build(), // node
                null // current
        ));
        assertTrue(highestNotFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, bazOrigin).build(), // node
                null // current
        ));
        assertFalse(highestNotFromGroup.shouldAccumulate(
                ChatMetaType.PREFIX,
                Prefix.builder("foo", 100).withMetadata(InheritanceOriginMetadata.KEY, fooOrigin).build(), // node
                null // current
        ));
    }

}
