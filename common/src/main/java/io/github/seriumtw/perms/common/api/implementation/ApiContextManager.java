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

import io.github.seriumtw.perms.common.context.manager.ContextManager;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.query.QueryOptionsBuilderImpl;
import io.github.seriumtw.perms.api.context.ContextCalculator;
import io.github.seriumtw.perms.api.context.ContextSetFactory;
import io.github.seriumtw.perms.api.context.ImmutableContextSet;
import io.github.seriumtw.perms.api.model.user.User;
import io.github.seriumtw.perms.api.query.QueryMode;
import io.github.seriumtw.perms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ApiContextManager implements io.github.seriumtw.perms.api.context.ContextManager {
    private final SRMPermsPlugin plugin;
    private final ContextManager handle;

    public ApiContextManager(SRMPermsPlugin plugin, ContextManager<?, ?> handle) {
        this.plugin = plugin;
        this.handle = handle;
    }

    private Object checkType(Object subject) {
        if (!this.handle.getSubjectClass().isAssignableFrom(subject.getClass())) {
            throw new IllegalStateException("Subject class " + subject.getClass() + " is not assignable from " + this.handle.getSubjectClass());
        }
        return subject;
    }

    @Override
    public @NonNull ImmutableContextSet getContext(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getContext(checkType(subject));
    }

    @Override
    public @NonNull Optional<ImmutableContextSet> getContext(@NonNull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getQueryOptionsForUser(ApiUser.cast(user)).map(QueryOptions::context);
    }

    @Override
    public @NonNull ImmutableContextSet getStaticContext() {
        return this.handle.getStaticContext();
    }

    @Override
    public QueryOptions.@NonNull Builder queryOptionsBuilder(@NonNull QueryMode mode) {
        Objects.requireNonNull(mode, "mode");
        return new QueryOptionsBuilderImpl(mode);
    }

    @Override
    public @NonNull QueryOptions getQueryOptions(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getQueryOptions(subject);
    }

    @Override
    public @NonNull Optional<QueryOptions> getQueryOptions(@NonNull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getQueryOptionsForUser(ApiUser.cast(user));
    }

    @Override
    public @NonNull QueryOptions getStaticQueryOptions() {
        return this.handle.getStaticQueryOptions();
    }

    @Override
    public void registerCalculator(@NonNull ContextCalculator<?> calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.registerCalculator(calculator);
    }

    @Override
    public void unregisterCalculator(@NonNull ContextCalculator<?> calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.unregisterCalculator(calculator);
    }

    @Override
    public @NonNull ContextSetFactory getContextSetFactory() {
        return ApiContextSetFactory.INSTANCE;
    }

    @Override
    public void signalContextUpdate(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        this.handle.signalContextUpdate(checkType(subject));
    }
}
