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

package io.github.seriumtw.perms.common.webeditor;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import io.github.seriumtw.perms.common.context.ImmutableContextSetImpl;
import io.github.seriumtw.perms.common.context.serializer.ContextSetJsonSerializer;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.PermissionHolder;
import io.github.seriumtw.perms.common.model.PermissionHolderIdentifier;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.node.matcher.ConstraintNodeMatcher;
import io.github.seriumtw.perms.common.node.utils.NodeJsonSerializer;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.storage.misc.NodeEntry;
import io.github.seriumtw.perms.common.util.ImmutableCollectors;
import io.github.seriumtw.perms.common.util.gson.GsonProvider;
import io.github.seriumtw.perms.common.util.gson.JArray;
import io.github.seriumtw.perms.common.util.gson.JObject;
import io.github.seriumtw.perms.common.verbose.event.CheckOrigin;
import io.github.seriumtw.perms.api.context.ImmutableContextSet;
import io.github.seriumtw.perms.api.node.Node;
import io.github.seriumtw.perms.api.query.QueryOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Encapsulates a request to the web editor.
 */
public class WebEditorRequest {

    public static final int MAX_USERS = 500;

    /**
     * The encoded json object this payload is made up of
     */
    private final JsonObject payload;

    private final Map<PermissionHolderIdentifier, List<Node>> holders;
    private final Map<String, List<String>> tracks;

    private WebEditorRequest(JsonObject payload, Map<PermissionHolder, List<Node>> holders, Map<Track, List<String>> tracks) {
        this.payload = payload;
        this.holders = holders.entrySet().stream().collect(ImmutableCollectors.toMap(
                e -> e.getKey().getIdentifier(),
                Map.Entry::getValue
        ));
        this.tracks = tracks.entrySet().stream().collect(ImmutableCollectors.toMap(
                e -> e.getKey().getName(),
                Map.Entry::getValue
        ));
    }

    public JsonObject getPayload() {
        return this.payload;
    }

    public byte[] encode() {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytesOut), StandardCharsets.UTF_8)) {
            GsonProvider.normal().toJson(this.payload, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesOut.toByteArray();
    }

    public Map<PermissionHolderIdentifier, List<Node>> getHolders() {
        return this.holders;
    }

    public Map<String, List<String>> getTracks() {
        return this.tracks;
    }

    /**
     * Generates a web editor request payload.
     *
     * @param holders the holders to edit
     * @param tracks the tracks to edit
     * @param sender the sender who is creating the session
     * @param cmdLabel the command label used by SRMPerms
     * @param plugin the plugin
     * @return a payload
     */
    public static WebEditorRequest generate(List<PermissionHolder> holders, List<Track> tracks, Sender sender, String cmdLabel, SRMPermsPlugin plugin) {
        Preconditions.checkArgument(!holders.isEmpty(), "holders is empty");

        ImmutableContextSet.Builder potentialContexts = new ImmutableContextSetImpl.BuilderImpl();
        potentialContexts.addAll(plugin.getContextManager().getPotentialContexts());
        for (PermissionHolder holder : holders) {
            holder.normalData().forEach(node -> potentialContexts.addAll(node.getContexts()));
        }

        // form the payload data
        Map<PermissionHolder, List<Node>> holdersMap = holders.stream().collect(ImmutableCollectors.toMap(
                Function.identity(),
                holder -> holder.normalData().asList()
        ));

        Map<Track, List<String>> tracksMap = tracks.stream().collect(ImmutableCollectors.toMap(
                Function.identity(),
                Track::getGroups
        ));

        JsonObject json = createJsonPayload(holdersMap, tracksMap, sender, cmdLabel, potentialContexts.build(), plugin).toJson();
        return new WebEditorRequest(json, holdersMap, tracksMap);
    }

    private static JObject createJsonPayload(Map<PermissionHolder, List<Node>> holders, Map<Track, List<String>> tracks, Sender sender, String cmdLabel, ImmutableContextSet potentialContexts, SRMPermsPlugin plugin) {
        return new JObject()
                .add("metadata", formMetadata(sender, cmdLabel, plugin.getBootstrap().getVersion(), plugin.getBootstrap().getType().getFriendlyName()))
                .add("permissionHolders", new JArray().consume(arr ->
                        holders.forEach((holder, data) ->
                                arr.add(formPermissionHolder(holder, data))
                        )
                ))
                .add("tracks", new JArray().consume(arr ->
                        tracks.forEach((track, data) ->
                                arr.add(formTrack(track, data))
                        )
                ))
                .add("knownPermissions", new JArray().addAll(plugin.getPermissionRegistry().rootAsList()))
                .add("potentialContexts", ContextSetJsonSerializer.serialize(potentialContexts));
    }

    private static JObject formMetadata(Sender sender, String cmdLabel, String pluginVersion, String platform) {
        return new JObject()
                .add("commandAlias", cmdLabel)
                .add("uploader", new JObject()
                        .add("name", sender.getNameWithLocation())
                        .add("uuid", sender.getUniqueId().toString())
                )
                .add("time", System.currentTimeMillis())
                .add("pluginVersion", pluginVersion)
                .add("platform", platform);
    }

    private static JObject formPermissionHolder(PermissionHolder holder, List<Node> data) {
        return new JObject()
                .add("type", holder.getType().toString())
                .add("id", holder.getIdentifier().getName())
                .add("displayName", holder.getPlainDisplayName())
                .add("nodes", NodeJsonSerializer.serializeNodes(data));
    }

    private static JObject formTrack(Track track, List<String> data) {
        return new JObject()
                .add("type", "track")
                .add("id", track.getName())
                .add("groups", new JArray().addAll(data));
    }

    public static void includeMatchingGroups(List<? super Group> holders, Predicate<? super Group> filter, SRMPermsPlugin plugin) {
        plugin.getGroupManager().getAll().values().stream()
                .filter(filter)
                .sorted(Comparator
                        .<Group>comparingInt(g -> g.getWeight().orElse(0)).reversed()
                        .thenComparing(Group::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .forEach(holders::add);
    }

    public static void includeMatchingUsers(List<? super User> holders, ConstraintNodeMatcher<Node> matcher, boolean includeOffline, SRMPermsPlugin plugin) {
        includeMatchingUsers(holders, matcher == null ? Collections.emptyList() : Collections.singleton(matcher), includeOffline, plugin);
    }

    public static void includeMatchingUsers(List<? super User> holders, Collection<ConstraintNodeMatcher<Node>> matchers, boolean includeOffline, SRMPermsPlugin plugin) {
        Map<UUID, User> users = new LinkedHashMap<>(plugin.getUserManager().getAll());

        if (!matchers.isEmpty()) {
            users.values().removeIf(user -> {
                for (ConstraintNodeMatcher<Node> matcher : matchers) {
                    if (user.normalData().asList().stream().anyMatch(matcher)) {
                        return false;
                    }
                }
                return true;
            });
        }

        if (includeOffline && users.size() < MAX_USERS) {
            if (matchers.isEmpty()) {
                findMatchingOfflineUsers(users, null, plugin);
            } else {
                for (ConstraintNodeMatcher<Node> matcher : matchers) {
                    if (users.size() < MAX_USERS) {
                        findMatchingOfflineUsers(users, matcher, plugin);
                    } else {
                        break;
                    }
                }
            }
        }

        users.values().stream()
                .sorted(Comparator
                        // sort firstly by the users relative weight (depends on the groups they inherit)
                        .<User>comparingInt(u -> u.getCachedData().getMetaData(QueryOptions.nonContextual()).getWeight(CheckOrigin.INTERNAL).intResult()).reversed()
                        // then, prioritise users we actually have a username for
                        .thenComparing(u -> u.getUsername().isPresent(), ((Comparator<Boolean>) Boolean::compare).reversed())
                        // then sort according to their username
                        .thenComparing(User::getPlainDisplayName, String.CASE_INSENSITIVE_ORDER)
                )
                .forEach(holders::add);
    }

    private static void findMatchingOfflineUsers(Map<UUID, User> users, ConstraintNodeMatcher<Node> matcher, SRMPermsPlugin plugin) {
        Stream<UUID> stream;
        if (matcher == null) {
            stream = plugin.getStorage().getUniqueUsers().join().stream();
        } else {
            stream = plugin.getStorage().searchUserNodes(matcher).join().stream()
                    .map(NodeEntry::getHolder)
                    .distinct();
        }

        Set<UUID> uuids = stream
                .filter(uuid -> !users.containsKey(uuid))
                .sorted()
                .limit(MAX_USERS - users.size())
                .collect(Collectors.toSet());

        if (uuids.isEmpty()) {
            return;
        }

        // load users in bulk from storage
        Map<UUID, User> loadedUsers = plugin.getStorage().loadUsers(uuids).join();
        users.putAll(loadedUsers);

        // schedule cleanup
        for (UUID uniqueId : loadedUsers.keySet()) {
            plugin.getUserManager().getHouseKeeper().cleanup(uniqueId);
        }
    }

}
