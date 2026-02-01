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

package io.github.seriumtw.perms.hytale;

import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import io.github.seriumtw.perms.common.api.SRMPermsApiProvider;
import io.github.seriumtw.perms.common.calculator.CalculatorFactory;
import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.config.generic.adapter.ConfigurationAdapter;
import io.github.seriumtw.perms.common.dependencies.Dependency;
import io.github.seriumtw.perms.common.dependencies.DependencyManager;
import io.github.seriumtw.perms.common.dependencies.DependencyManagerImpl;
import io.github.seriumtw.perms.common.dependencies.DependencyRepository;
import io.github.seriumtw.perms.common.event.AbstractEventBus;
import io.github.seriumtw.perms.common.messaging.MessagingFactory;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.model.manager.group.StandardGroupManager;
import io.github.seriumtw.perms.common.model.manager.track.StandardTrackManager;
import io.github.seriumtw.perms.common.model.manager.user.StandardUserManager;
import io.github.seriumtw.perms.common.plugin.AbstractSRMPermsPlugin;
import io.github.seriumtw.perms.common.plugin.util.AbstractConnectionListener;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.hytale.calculator.HytaleCalculatorFactory;
import io.github.seriumtw.perms.hytale.calculator.virtualgroups.VirtualGroupsLookupProvider;
import io.github.seriumtw.perms.hytale.chat.SRMPermsChatFormatter;
import io.github.seriumtw.perms.hytale.context.HytaleContextManager;
import io.github.seriumtw.perms.hytale.context.HytalePlayerCalculator;
import io.github.seriumtw.perms.hytale.listeners.HytaleConnectionListener;
import io.github.seriumtw.perms.hytale.listeners.HytalePlatformListener;
import io.github.seriumtw.perms.hytale.service.SRMPermsPermissionProvider;
import io.github.seriumtw.perms.hytale.service.PlayerVirtualGroupsMap;
import io.github.seriumtw.perms.api.SRMPerms;
import io.github.seriumtw.perms.api.query.QueryOptions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * SRMPerms implementation for Hytale.
 */
public class LPHytalePlugin extends AbstractSRMPermsPlugin {
    private final LPHytaleBootstrap bootstrap;

    private HytaleSenderFactory senderFactory;
    private HytaleConnectionListener connectionListener;
    private HytaleCommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private HytaleContextManager contextManager;

    private PlayerVirtualGroupsMap playerVirtualGroupsMap;
    private VirtualGroupsLookupProvider virtualGroupsLookupProvider;
    private SRMPermsPermissionProvider luckPermsPermissionProvider;

    public LPHytalePlugin(LPHytaleBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPHytaleBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public JavaPlugin getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected DependencyManager createDependencyManager() {
        boolean loadDepsFromJar = false;

        JavaPlugin loader = this.bootstrap.getLoader();
        try {
            Field loadDepsFromJarField = loader.getClass().getField("LOAD_DEPS_FROM_JAR");
            loadDepsFromJar = loadDepsFromJarField.getBoolean(loader);
        } catch (Exception e) {
            // ignore
        }

        if (loadDepsFromJar) {
            getLogger().info("Will load dependencies from local jar (jar-in-jar)");
            return new DependencyManagerImpl(this, List.of(DependencyRepository.JAR_IN_JAR));
        }

        return super.createDependencyManager();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new HytaleSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        // required for loading the LP config
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new HytaleConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new HytaleConnectionListener(this);
        this.connectionListener.register(this.bootstrap.getLoader().getEventRegistry());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new HytaleCommandManager(this);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        this.virtualGroupsLookupProvider = new VirtualGroupsLookupProvider();
        return new HytaleCalculatorFactory(this, this.virtualGroupsLookupProvider);
    }

    @Override
    protected void setupContextManager() {
        this.playerVirtualGroupsMap = new PlayerVirtualGroupsMap();
        this.contextManager = new HytaleContextManager(this, this.playerVirtualGroupsMap);

        HytalePlayerCalculator playerCalculator = new HytalePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        playerCalculator.registerEvents(this.bootstrap.getLoader().getEventRegistry());
        playerCalculator.registerSystems(this.bootstrap.getLoader().getEntityStoreRegistry());
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        // permissions
        PermissionsModule permissionsModule = PermissionsModule.get();

        // find the hytale provider
        HytalePermissionsProvider hytaleProvider = null;
        for (PermissionProvider provider : permissionsModule.getProviders()) {
            if (provider instanceof HytalePermissionsProvider hpp) {
                hytaleProvider = hpp;
                break;
            }
        }

        // register our provider
        this.luckPermsPermissionProvider = new SRMPermsPermissionProvider(this, hytaleProvider, this.playerVirtualGroupsMap);
        permissionsModule.addProvider(this.luckPermsPermissionProvider);

        // remove all other providers
        for (PermissionProvider provider : permissionsModule.getProviders()) {
            if (provider != this.luckPermsPermissionProvider) {
                permissionsModule.removeProvider(provider);
            }
        }

        // chat
        if (getConfiguration().get(ConfigKeys.CHAT_FORMATTER_ENABLED)) {
            SRMPermsChatFormatter chatFormatter = new SRMPermsChatFormatter(this);
            chatFormatter.register(this.bootstrap.getLoader().getEventRegistry());
        }

        // general
        HytalePlatformListener platformListener = new HytalePlatformListener(this);
        platformListener.register(this.bootstrap.getLoader().getEventRegistry());
    }

    @Override
    protected void removePlatformHooks() {
        PermissionsModule permissionsModule = PermissionsModule.get();
        HytalePermissionsProvider hytaleProvider = this.luckPermsPermissionProvider.getHytaleProvider();
        if (hytaleProvider != null) {
            permissionsModule.addProvider(hytaleProvider);
        }
        permissionsModule.removeProvider(this.luckPermsPermissionProvider);
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(SRMPermsApiProvider apiProvider) {
        return new HytaleEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(SRMPerms api) {

    }

    @Override
    protected void performFinalSetup() {

    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                Universe.get().getPlayers().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(ConsoleSender.INSTANCE);
    }

    public HytaleSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    public VirtualGroupsLookupProvider getVirtualGroupsLookupProvider() {
        return this.virtualGroupsLookupProvider;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public HytaleCommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public HytaleContextManager getContextManager() {
        return this.contextManager;
    }

}
