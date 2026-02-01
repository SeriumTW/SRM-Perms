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

package io.github.seriumtw.perms.common.context.calculator;

import io.github.seriumtw.perms.common.config.ConfigKeys;
import io.github.seriumtw.perms.common.config.SRMPermsConfiguration;
import io.github.seriumtw.perms.common.context.ImmutableContextSetImpl;
import io.github.seriumtw.perms.api.context.ContextConsumer;
import io.github.seriumtw.perms.api.context.ContextSet;
import io.github.seriumtw.perms.api.context.DefaultContextKeys;
import io.github.seriumtw.perms.api.context.ImmutableContextSet;
import io.github.seriumtw.perms.api.context.StaticContextCalculator;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ConfigurationContextCalculator implements StaticContextCalculator {
    private final SRMPermsConfiguration config;

    public ConfigurationContextCalculator(SRMPermsConfiguration config) {
        this.config = config;
    }

    @Override
    public void calculate(@NonNull ContextConsumer consumer) {
        String server = this.config.get(ConfigKeys.SERVER);
        if (!server.equals("global")) {
            consumer.accept(DefaultContextKeys.SERVER_KEY, server);
        }
        consumer.accept(this.config.getContextsFile().getStaticContexts());
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        calculate(builder::add);
        return builder.build();
    }
}
