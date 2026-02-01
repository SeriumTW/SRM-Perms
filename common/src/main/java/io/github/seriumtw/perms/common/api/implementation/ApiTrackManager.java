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

package io.github.seriumtw.perms.common.api.implementation;

import io.github.seriumtw.perms.common.api.ApiUtils;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.model.manager.track.TrackManager;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.util.ImmutableCollectors;
import io.github.seriumtw.perms.api.event.cause.CreationCause;
import io.github.seriumtw.perms.api.event.cause.DeletionCause;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiTrackManager extends ApiAbstractManager<Track, io.github.seriumtw.perms.api.track.Track, TrackManager<?>> implements io.github.seriumtw.perms.api.track.TrackManager {
    public ApiTrackManager(SRMPermsPlugin plugin, TrackManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected io.github.seriumtw.perms.api.track.Track proxy(Track internal) {
        return internal == null ? null : internal.getApiProxy();
    }

    @Override
    public @NonNull CompletableFuture<io.github.seriumtw.perms.api.track.Track> createAndLoadTrack(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().createAndLoadTrack(name, CreationCause.API)
                .thenApply(this::proxy);
    }

    @Override
    public @NonNull CompletableFuture<Optional<io.github.seriumtw.perms.api.track.Track>> loadTrack(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().loadTrack(name).thenApply(opt -> opt.map(this::proxy));
    }

    @Override
    public @NonNull CompletableFuture<Void> saveTrack(io.github.seriumtw.perms.api.track.@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.plugin.getStorage().saveTrack(ApiTrack.cast(track));
    }

    @Override
    public @NonNull CompletableFuture<Void> deleteTrack(io.github.seriumtw.perms.api.track.@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.plugin.getStorage().deleteTrack(ApiTrack.cast(track), DeletionCause.API);
    }

    @Override
    public @NonNull CompletableFuture<Void> modifyTrack(@NonNull String name, @NonNull Consumer<? super io.github.seriumtw.perms.api.track.Track> action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");

        return this.plugin.getStorage().createAndLoadTrack(name, CreationCause.API)
                .thenApplyAsync(track -> {
                    action.accept(track.getApiProxy());
                    return track;
                }, this.plugin.getBootstrap().getScheduler().async())
                .thenCompose(track -> this.plugin.getStorage().saveTrack(track));
    }

    @Override
    public @NonNull CompletableFuture<Void> loadAllTracks() {
        return this.plugin.getStorage().loadAllTracks();
    }

    @Override
    public io.github.seriumtw.perms.api.track.Track getTrack(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return proxy(this.handle.getIfLoaded(name));
    }

    @Override
    public @NonNull Set<io.github.seriumtw.perms.api.track.Track> getLoadedTracks() {
        return this.handle.getAll().values().stream()
                .map(this::proxy)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
