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

package io.github.seriumtw.perms.common.plugin;

import io.github.seriumtw.perms.common.actionlog.LogDispatcher;
import io.github.seriumtw.perms.common.api.SRMPermsApiProvider;
import io.github.seriumtw.perms.common.calculator.CalculatorFactory;
import io.github.seriumtw.perms.common.command.CommandManager;
import io.github.seriumtw.perms.common.command.abstraction.Command;
import io.github.seriumtw.perms.common.config.SRMPermsConfiguration;
import io.github.seriumtw.perms.common.context.manager.ContextManager;
import io.github.seriumtw.perms.common.dependencies.DependencyManager;
import io.github.seriumtw.perms.common.event.EventDispatcher;
import io.github.seriumtw.perms.common.extension.SimpleExtensionManager;
import io.github.seriumtw.perms.common.http.BytebinClient;
import io.github.seriumtw.perms.common.http.BytesocksClient;
import io.github.seriumtw.perms.common.inheritance.InheritanceGraphFactory;
import io.github.seriumtw.perms.common.locale.TranslationManager;
import io.github.seriumtw.perms.common.locale.TranslationRepository;
import io.github.seriumtw.perms.common.messaging.InternalMessagingService;
import io.github.seriumtw.perms.common.model.Group;
import io.github.seriumtw.perms.common.model.Track;
import io.github.seriumtw.perms.common.model.User;
import io.github.seriumtw.perms.common.model.manager.group.GroupManager;
import io.github.seriumtw.perms.common.model.manager.track.TrackManager;
import io.github.seriumtw.perms.common.model.manager.user.UserManager;
import io.github.seriumtw.perms.common.plugin.bootstrap.SRMPermsBootstrap;
import io.github.seriumtw.perms.common.plugin.logging.PluginLogger;
import io.github.seriumtw.perms.common.plugin.util.AbstractConnectionListener;
import io.github.seriumtw.perms.common.sender.Sender;
import io.github.seriumtw.perms.common.storage.Storage;
import io.github.seriumtw.perms.common.storage.implementation.file.watcher.FileWatcher;
import io.github.seriumtw.perms.common.tasks.SyncTask;
import io.github.seriumtw.perms.common.treeview.PermissionRegistry;
import io.github.seriumtw.perms.common.verbose.VerboseHandler;
import io.github.seriumtw.perms.common.webeditor.store.WebEditorStore;
import io.github.seriumtw.perms.api.platform.Health;
import io.github.seriumtw.perms.api.query.QueryOptions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Main internal interface for SRMPerms plugins, providing the base for
 * abstraction throughout the project.
 *
 * All plugin platforms implement this interface.
 */
public interface SRMPermsPlugin {

    /**
     * Gets the bootstrap plugin instance
     *
     * @return the bootstrap plugin
     */
    SRMPermsBootstrap getBootstrap();

    /**
     * Gets the user manager instance for the platform
     *
     * @return the user manager
     */
    UserManager<? extends User> getUserManager();

    /**
     * Gets the group manager instance for the platform
     *
     * @return the group manager
     */
    GroupManager<? extends Group> getGroupManager();

    /**
     * Gets the track manager instance for the platform
     *
     * @return the track manager
     */
    TrackManager<? extends Track> getTrackManager();

    /**
     * Gets the plugin's configuration
     *
     * @return the plugin config
     */
    SRMPermsConfiguration getConfiguration();

    /**
     * Gets the primary data storage instance. This is likely to be wrapped with extra layers for caching, etc.
     *
     * @return the storage handler instance
     */
    Storage getStorage();

    /**
     * Gets the messaging service.
     *
     * @return the messaging service
     */
    Optional<InternalMessagingService> getMessagingService();

    /**
     * Sets the messaging service.
     *
     * @param service the service
     */
    void setMessagingService(InternalMessagingService service);

    /**
     * Gets a wrapped logger instance for the platform.
     *
     * @return the plugin's logger
     */
    PluginLogger getLogger();

    /**
     * Gets the event dispatcher
     *
     * @return the event dispatcher
     */
    EventDispatcher getEventDispatcher();

    /**
     * Returns the class implementing the SRMPermsAPI on this platform.
     *
     * @return the api
     */
    SRMPermsApiProvider getApiProvider();

    /**
     * Gets the extension manager.
     *
     * @return the extension manager
     */
    SimpleExtensionManager getExtensionManager();

    /**
     * Gets the command manager
     *
     * @return the command manager
     */
    CommandManager getCommandManager();

    /**
     * Gets the connection listener.
     *
     * @return the connection listener
     */
    AbstractConnectionListener getConnectionListener();

    /**
     * Gets the instance providing locale translations for the plugin
     *
     * @return the translation manager
     */
    TranslationManager getTranslationManager();

    /**
     * Gets the translation repository
     *
     * @return the translation repository
     */
    TranslationRepository getTranslationRepository();

    /**
     * Gets the dependency manager for the plugin
     *
     * @return the dependency manager
     */
    DependencyManager getDependencyManager();

    /**
     * Gets the context manager.
     * This object handles context accumulation for all players on the platform.
     *
     * @return the context manager
     */
    ContextManager<?, ?> getContextManager();

    /**
     * Gets the inheritance handler
     *
     * @return the inheritance handler
     */
    InheritanceGraphFactory getInheritanceGraphFactory();

    /**
     * Gets the class responsible for constructing PermissionCalculators on this platform.
     *
     * @return the permission calculator factory
     */
    CalculatorFactory getCalculatorFactory();

    /**
     * Gets the verbose debug handler instance.
     *
     * @return the debug handler instance
     */
    VerboseHandler getVerboseHandler();

    /**
     * Gets the permission registry for the platform.
     *
     * @return the permission registry
     */
    PermissionRegistry getPermissionRegistry();

    /**
     * Gets the log dispatcher running on the platform
     *
     * @return the log dispatcher
     */
    LogDispatcher getLogDispatcher();

    /**
     * Gets the file watcher running on the platform
     *
     * @return the file watcher
     */
    Optional<FileWatcher> getFileWatcher();

    /**
     * Gets the bytebin instance in use by platform.
     *
     * @return the bytebin instance
     */
    BytebinClient getBytebin();

    /**
     * Gets the bytesocks instance in use by platform.
     *
     * @return the bytesocks instance
     */
    BytesocksClient getBytesocks();

    /**
     * Gets the web editor store
     *
     * @return the web editor store
     */
    WebEditorStore getWebEditorStore();

    /**
     * Runs a health check for the plugin.
     *
     * @return the result of the healthcheck
     */
    Health runHealthCheck();

    /**
     * Gets a calculated context instance for the user using the rules of the platform.
     *
     * @param user the user instance
     * @return a contexts object, or null if one couldn't be generated
     */
    Optional<QueryOptions> getQueryOptionsForUser(User user);

    /**
     * Lookup a uuid from a username.
     *
     * @param username the username to lookup
     * @return an optional uuid, if found
     */
    Optional<UUID> lookupUniqueId(String username);

    /**
     * Lookup a username from a uuid.
     *
     * @param uniqueId the uuid to lookup
     * @return an optional username, if found
     */
    Optional<String> lookupUsername(UUID uniqueId);

    /**
     * Tests whether the given username is valid.
     *
     * @param username the username
     * @return true if valid
     */
    boolean testUsernameValidity(String username);

    /**
     * Gets a list of online Senders on the platform
     *
     * @return a {@link List} of senders online on the platform
     */
    Stream<Sender> getOnlineSenders();

    /**
     * Gets the console.
     *
     * @return the console sender of the instance
     */
    Sender getConsoleSender();

    default List<Command<?>> getExtraCommands() {
        return Collections.emptyList();
    }

    /**
     * Gets the sync task buffer of the platform, used for scheduling and running sync tasks.
     *
     * @return the sync task buffer instance
     */
    SyncTask.Buffer getSyncTaskBuffer();

    /**
     * Called at the end of the sync task.
     */
    default void performPlatformDataSync() {

    }

}
