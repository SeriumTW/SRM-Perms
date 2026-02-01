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

package io.github.seriumtw.perms.common.api;

import io.github.seriumtw.perms.common.api.implementation.ApiActionFilterFactory;
import io.github.seriumtw.perms.common.api.implementation.ApiActionLogger;
import io.github.seriumtw.perms.common.api.implementation.ApiContextManager;
import io.github.seriumtw.perms.common.api.implementation.ApiGroupManager;
import io.github.seriumtw.perms.common.api.implementation.ApiMessagingService;
import io.github.seriumtw.perms.common.api.implementation.ApiMetaStackFactory;
import io.github.seriumtw.perms.common.api.implementation.ApiNodeBuilderRegistry;
import io.github.seriumtw.perms.common.api.implementation.ApiNodeMatcherFactory;
import io.github.seriumtw.perms.common.api.implementation.ApiPlatform;
import io.github.seriumtw.perms.common.api.implementation.ApiPlayerAdapter;
import io.github.seriumtw.perms.common.api.implementation.ApiQueryOptionsRegistry;
import io.github.seriumtw.perms.common.api.implementation.ApiTrackManager;
import io.github.seriumtw.perms.common.api.implementation.ApiUserManager;
import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.event.AbstractEventBus;
import io.github.seriumtw.perms.common.messaging.SRMPermsMessagingService;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.plugin.bootstrap.BootstrappedWithLoader;
import io.github.seriumtw.perms.common.plugin.bootstrap.SRMPermsBootstrap;
import io.github.seriumtw.perms.common.plugin.logging.PluginLogger;
import io.github.seriumtw.perms.api.SRMPerms;
import io.github.seriumtw.perms.api.SRMPermsProvider;
import io.github.seriumtw.perms.api.actionlog.ActionLogger;
import io.github.seriumtw.perms.api.actionlog.filter.ActionFilterFactory;
import io.github.seriumtw.perms.api.context.ContextManager;
import io.github.seriumtw.perms.api.messaging.MessagingService;
import io.github.seriumtw.perms.api.messenger.MessengerProvider;
import io.github.seriumtw.perms.api.metastacking.MetaStackFactory;
import io.github.seriumtw.perms.api.model.group.GroupManager;
import io.github.seriumtw.perms.api.model.user.UserManager;
import io.github.seriumtw.perms.api.node.NodeBuilderRegistry;
import io.github.seriumtw.perms.api.node.matcher.NodeMatcherFactory;
import io.github.seriumtw.perms.api.platform.Health;
import io.github.seriumtw.perms.api.platform.Platform;
import io.github.seriumtw.perms.api.platform.PlayerAdapter;
import io.github.seriumtw.perms.api.platform.PluginMetadata;
import io.github.seriumtw.perms.api.query.QueryOptionsRegistry;
import io.github.seriumtw.perms.api.track.TrackManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements the SRMPerms API using the plugin instance
 */
public class SRMPermsApiProvider implements SRMPerms {

    private final SRMPermsPlugin plugin;

    private final ApiPlatform platform;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final PlayerAdapter<?> playerAdapter;
    private final ActionLogger actionLogger;
    private final ContextManager contextManager;
    private final MetaStackFactory metaStackFactory;

    public SRMPermsApiProvider(SRMPermsPlugin plugin) {
        this.plugin = plugin;

        this.platform = new ApiPlatform(plugin);
        this.userManager = new ApiUserManager(plugin, plugin.getUserManager());
        this.groupManager = new ApiGroupManager(plugin, plugin.getGroupManager());
        this.trackManager = new ApiTrackManager(plugin, plugin.getTrackManager());
        this.playerAdapter = new ApiPlayerAdapter<>(plugin.getUserManager(), plugin.getContextManager());
        this.actionLogger = new ApiActionLogger(plugin);
        this.contextManager = new ApiContextManager(plugin, plugin.getContextManager());
        this.metaStackFactory = new ApiMetaStackFactory(plugin);
    }

    public void ensureApiWasLoadedByPlugin() {
        SRMPermsBootstrap bootstrap = this.plugin.getBootstrap();
        ClassLoader pluginClassLoader;
        if (bootstrap instanceof BootstrappedWithLoader) {
            pluginClassLoader = ((BootstrappedWithLoader) bootstrap).getLoader().getClass().getClassLoader();
        } else {
            pluginClassLoader = bootstrap.getClass().getClassLoader();
        }

        for (Class<?> apiClass : new Class[]{SRMPerms.class, SRMPermsProvider.class}) {
            ClassLoader apiClassLoader = apiClass.getClassLoader();

            if (!apiClassLoader.equals(pluginClassLoader)) {
                String guilty = "unknown";
                try {
                    guilty = bootstrap.identifyClassLoader(apiClassLoader);
                } catch (Exception e) {
                    // ignore
                }

                PluginLogger logger = this.plugin.getLogger();
                logger.warn("It seems that the SRMPerms API has been (class)loaded by a plugin other than SRMPerms!");
                logger.warn("The API was loaded by " + apiClassLoader + " (" + guilty + ") and the " +
                        "SRMPerms plugin was loaded by " + pluginClassLoader.toString() + ".");
                logger.warn("This indicates that the other plugin has incorrectly \"shaded\" the " +
                        "SRMPerms API into its jar file. This can cause errors at runtime and should be fixed.");
                return;
            }
        }
    }

    @Override
    public @NonNull String getServerName() {
        return this.plugin.getConfiguration().get(ConfigKeys.SERVER);
    }

    @Override
    public @NonNull Platform getPlatform() {
        return this.platform;
    }

    @Override
    public @NonNull PluginMetadata getPluginMetadata() {
        return this.platform;
    }

    @Override
    public @NonNull UserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public @NonNull GroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public @NonNull TrackManager getTrackManager() {
        return this.trackManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @NonNull PlayerAdapter<T> getPlayerAdapter(@NonNull Class<T> playerClass) {
        Objects.requireNonNull(playerClass, "playerClass");
        Class<?> expectedClass = this.plugin.getContextManager().getPlayerClass();
        if (!expectedClass.equals(playerClass)) {
            throw new IllegalArgumentException("Player class " + playerClass.getName() + " does not equal " + expectedClass.getName());
        }
        return (PlayerAdapter<T>) this.playerAdapter;
    }

    @Override
    public @NonNull CompletableFuture<Void> runUpdateTask() {
        return this.plugin.getSyncTaskBuffer().request();
    }

    @Override
    public @NonNull Health runHealthCheck() {
        return this.plugin.runHealthCheck();
    }

    @Override
    public @NonNull AbstractEventBus<?> getEventBus() {
        return this.plugin.getEventDispatcher().getEventBus();
    }

    @Override
    public @NonNull Optional<MessagingService> getMessagingService() {
        return this.plugin.getMessagingService().map(ApiMessagingService::new);
    }

    @Override
    public void registerMessengerProvider(@NonNull MessengerProvider messengerProvider) {
        if (this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).equals("custom")) {
            this.plugin.setMessagingService(new SRMPermsMessagingService(this.plugin, messengerProvider));
        }
    }

    @Override
    public @NonNull ActionLogger getActionLogger() {
        return this.actionLogger;
    }

    @Override
    public @NonNull ContextManager getContextManager() {
        return this.contextManager;
    }

    @Override
    public @NonNull NodeBuilderRegistry getNodeBuilderRegistry() {
        return ApiNodeBuilderRegistry.INSTANCE;
    }

    @Override
    public @NonNull QueryOptionsRegistry getQueryOptionsRegistry() {
        return ApiQueryOptionsRegistry.INSTANCE;
    }

    @Override
    public @NonNull MetaStackFactory getMetaStackFactory() {
        return this.metaStackFactory;
    }

    @Override
    public @NonNull NodeMatcherFactory getNodeMatcherFactory() {
        return ApiNodeMatcherFactory.INSTANCE;
    }

    @Override
    public @NonNull ActionFilterFactory getActionFilterFactory() {
        return ApiActionFilterFactory.INSTANCE;
    }
}
