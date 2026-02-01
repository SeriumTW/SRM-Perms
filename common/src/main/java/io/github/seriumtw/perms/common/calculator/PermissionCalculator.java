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

package io.github.seriumtw.perms.common.calculator;

import io.github.seriumtw.perms.common.cacheddata.CacheMetadata;
import io.github.seriumtw.perms.common.cacheddata.result.TristateResult;
import io.github.seriumtw.perms.common.calculator.processor.PermissionProcessor;
import io.github.seriumtw.perms.common.plugin.SRMPermsPlugin;
import io.github.seriumtw.perms.common.verbose.event.CheckOrigin;

import java.util.Collection;

/**
 * Calculates and caches permissions
 */
public class PermissionCalculator extends PermissionLookupCache {

    /** The plugin instance */
    private final SRMPermsPlugin plugin;

    /** Info about the nature of this calculator. */
    private final CacheMetadata metadata;

    public PermissionCalculator(SRMPermsPlugin plugin, CacheMetadata metadata, Collection<PermissionProcessor> processors) {
        super(processors);
        this.plugin = plugin;
        this.metadata = metadata;
    }

    @Override
    public TristateResult checkPermission(String permission, CheckOrigin origin) {
        TristateResult result = super.checkPermission(permission, origin);
        this.plugin.getVerboseHandler().offerPermissionCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), permission, result);
        return result;
    }

    @Override
    protected void observePermission(String permission) {
        // offer the permission to the permission vault
        // we only need to do this once per permission, so it doesn't matter
        // that this call is behind the cache.
        this.plugin.getPermissionRegistry().offer(permission);
    }
}
