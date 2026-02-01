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

package io.github.seriumtw.perms.hytale.loader;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import io.github.seriumtw.perms.common.loader.JarInJarClassLoader;
import io.github.seriumtw.perms.common.loader.LoaderBootstrap;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HytaleLoaderPlugin extends JavaPlugin {
    private static final String JAR_NAME = "srmperms-hytale.jarinjar";
    private static final String BOOTSTRAP_CLASS = "io.github.seriumtw.perms.hytale.LPHytaleBootstrap";

    private final LoaderBootstrap plugin;

    public HytaleLoaderPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, JavaPlugin.class, this);
    }

    @Override
    protected void setup() {
        this.plugin.onLoad();
    }

    @Override
    protected void start() {
        this.plugin.onEnable();
    }

    @Override
    protected void shutdown() {
        this.plugin.onDisable();
    }

}
