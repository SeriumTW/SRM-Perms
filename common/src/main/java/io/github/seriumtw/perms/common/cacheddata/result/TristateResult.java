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

package io.github.seriumtw.perms.common.cacheddata.result;

import io.github.seriumtw.perms.common.calculator.PermissionCalculator;
import io.github.seriumtw.perms.common.calculator.processor.PermissionProcessor;
import io.github.seriumtw.perms.api.node.Node;
import io.github.seriumtw.perms.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the result of a {@link PermissionCalculator} lookup.
 */
public final class TristateResult extends AbstractResult<Tristate, Node, TristateResult> {

    /** The result */
    private final Tristate result;
    /** The permission processor that provided the result */
    private final Class<? extends PermissionProcessor> processorClass;

    private TristateResult(Tristate result, Node node, Class<? extends PermissionProcessor> processorClass) {
        super(node, null);
        this.result = result;
        this.processorClass = processorClass;
    }

    @Override
    public @NonNull Tristate result() {
        return this.result;
    }

    public @Nullable Class<? extends PermissionProcessor> processorClass() {
        return this.processorClass;
    }

    public @Nullable String processorClassFriendly() {
        if (this.processorClass == null) {
            return null;
        } else if (this.processorClass.getName().startsWith("io.github.seriumtw.perms.")) {
            String simpleName = this.processorClass.getSimpleName();
            String platform = this.processorClass.getName().split("\\.")[3];
            return platform + "." + simpleName;
        } else {
            return this.processorClass.getName();
        }
    }

    @Override
    public String toString() {
        return "TristateResult(" +
                "result=" + this.result + ", " +
                "node=" + this.node + ", " +
                "processorClass=" + this.processorClass + ", " +
                "overriddenResult=" + this.overriddenResult + ')';
    }

    private static final TristateResult TRUE = new TristateResult(Tristate.TRUE, null,null);
    private static final TristateResult FALSE = new TristateResult(Tristate.FALSE, null,null);
    public static final TristateResult UNDEFINED = new TristateResult(Tristate.UNDEFINED, null,null);

    public static TristateResult forMonitoredResult(Tristate result) {
        switch (result) {
            case TRUE:
                return TRUE;
            case FALSE:
                return FALSE;
            case UNDEFINED:
                return UNDEFINED;
            default:
                throw new AssertionError();
        }
    }

    public static final class Factory {
        private final Class<? extends PermissionProcessor> processorClass;

        public Factory(Class<? extends PermissionProcessor> processorClass) {
            this.processorClass = processorClass;
        }

        public TristateResult result(Tristate result) {
            switch (result) {
                case TRUE:
                case FALSE:
                    return new TristateResult(result, null, this.processorClass);
                case UNDEFINED:
                    return UNDEFINED;
                default:
                    throw new AssertionError();
            }
        }

        public TristateResult result(@Nullable Node node) {
            if (node == null) {
                return UNDEFINED;
            }
            return new TristateResult(Tristate.of(node.getValue()), node, this.processorClass);
        }

        public TristateResult resultWithOverride(@Nullable Node node, @NonNull Tristate result) {
            if (result == Tristate.UNDEFINED) {
                return UNDEFINED;
            }
            return new TristateResult(result, node, this.processorClass);
        }
    }
}
