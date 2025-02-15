/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tyron.builder.plugin.use.resolve.internal;

import com.tyron.builder.api.internal.initialization.ClassLoaderScope;
import com.tyron.builder.api.internal.plugins.PluginDescriptorLocator;
import com.tyron.builder.api.internal.plugins.PluginInspector;
import com.tyron.builder.api.internal.plugins.PluginRegistry;
import com.tyron.builder.plugin.management.internal.InvalidPluginRequestException;
import com.tyron.builder.plugin.management.internal.PluginRequestInternal;
import com.tyron.builder.plugin.management.internal.autoapply.AutoAppliedGradleEnterprisePlugin;
import com.tyron.builder.plugin.use.PluginId;

public class AlreadyOnClasspathPluginResolver implements PluginResolver {
    private final PluginResolver delegate;
    private final PluginRegistry corePluginRegistry;
    private final PluginDescriptorLocator pluginDescriptorLocator;
    private final ClassLoaderScope parentLoaderScope;
    private final PluginInspector pluginInspector;

    public AlreadyOnClasspathPluginResolver(PluginResolver delegate, PluginRegistry corePluginRegistry, ClassLoaderScope parentLoaderScope, PluginDescriptorLocator pluginDescriptorLocator, PluginInspector pluginInspector) {
        this.delegate = delegate;
        this.corePluginRegistry = corePluginRegistry;
        this.pluginDescriptorLocator = pluginDescriptorLocator;
        this.parentLoaderScope = parentLoaderScope;
        this.pluginInspector = pluginInspector;
    }

    @Override
    public void resolve(PluginRequestInternal pluginRequest, PluginResolutionResult result) {
        PluginId pluginId = pluginRequest.getId();
        if (isCorePlugin(pluginId) || !isPresentOnClasspath(pluginId)) {
            delegate.resolve(pluginRequest, result);
        } else if (pluginRequest.getOriginalRequest().getVersion() != null) {
            if (pluginRequest.getId().equals(AutoAppliedGradleEnterprisePlugin.BUILD_SCAN_PLUGIN_ID)) {
                if (isPresentOnClasspath(AutoAppliedGradleEnterprisePlugin.ID)) {
                    // The JAR that contains the enterprise plugin also contains the build scan plugin.
                    // If the user is in the process of migrating to Gradle 6 and has not yet moved away from the scan plugin,
                    // they might hit this scenario when running with --scan as that will have auto applied the new plugin.
                    // Instead of a generic failure, we provide more specific feedback to help people upgrade.
                    // We use the same message the user would have seen if they didn't use --scan and trigger the auto apply.
                    throw new InvalidPluginRequestException(pluginRequest,
                        "The build scan plugin is not compatible with this version of Gradle.\n"
                            + "Please see https://gradle.com/help/gradle-6-build-scan-plugin for more information."
                    );
                }
            }
            throw new InvalidPluginRequestException(pluginRequest, "Plugin request for plugin already on the classpath must not include a version");
        } else {
            resolveAlreadyOnClasspath(pluginId, result);
        }
    }

    private void resolveAlreadyOnClasspath(PluginId pluginId, PluginResolutionResult result) {
        PluginResolution pluginResolution = new ClassPathPluginResolution(pluginId, parentLoaderScope, pluginInspector);
        result.found("Already on classpath", pluginResolution);
    }

    private boolean isPresentOnClasspath(PluginId pluginId) {
        return pluginDescriptorLocator.findPluginDescriptor(pluginId.toString()) != null;
    }

    private boolean isCorePlugin(PluginId pluginId) {
        return corePluginRegistry.lookup(pluginId) != null;
    }
}
