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

package io.github.seriumtw.perms.common.inheritance;

import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.query.QueryOptionsImpl;
import io.github.seriumtw.perms.api.query.QueryOptions;

/**
 * Provides {@link InheritanceGraph}s.
 */
public class InheritanceGraphFactory {
    private final SRMPermsPlugin plugin;

    private final InheritanceGraph nonContextualGraph;
    private final InheritanceGraph defaultContextualGraph;

    public InheritanceGraphFactory(SRMPermsPlugin plugin) {
        this.plugin = plugin;
        this.nonContextualGraph = new InheritanceGraph(plugin, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);
        this.defaultContextualGraph = new InheritanceGraph(plugin, QueryOptionsImpl.DEFAULT_CONTEXTUAL);
    }

    public InheritanceGraph getGraph(QueryOptions queryOptions) {
        if (queryOptions == QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL) {
            return this.nonContextualGraph;
        } else if (queryOptions == QueryOptionsImpl.DEFAULT_CONTEXTUAL) {
            return this.defaultContextualGraph;
        } else {
            return new InheritanceGraph(this.plugin, queryOptions);
        }
    }

}
