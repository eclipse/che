/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.agent.server.model.impl;

import com.google.common.base.MoreObjects;

import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.model.workspace.ServerConf2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Anatoliy Bazko
 */
public class AgentImpl implements Agent {
    private final String              name;
    private final String              version;
    private final List<String>        dependencies;
    private final Map<String, String> properties;
    private final String              script;
    private final List<ServerConf2>   servers;

    public AgentImpl(String name,
                     String version,
                     List<String> dependencies,
                     Map<String, String> properties,
                     String script,
                     List<? extends ServerConf2> servers) {
        this.name = name;
        this.version = version;
        this.dependencies = dependencies;
        this.properties = properties;
        this.script = script;
        this.servers = servers.stream().map(serverConf2 -> serverConf2).collect(Collectors.toList());
    }

    public AgentImpl(Agent agent) {
        this(agent.getName(),
             agent.getVersion(),
             agent.getDependencies(),
             agent.getProperties(),
             agent.getScript(),
             agent.getServers());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<String> getDependencies() {
        return MoreObjects.firstNonNull(dependencies, new ArrayList<>());
    }

    @Override
    public Map<String, String> getProperties() {
        return MoreObjects.firstNonNull(properties, new HashMap<>());
    }

    @Override
    public String getScript() {
        return script;
    }

    @Override
    public List<ServerConf2> getServers() {
        return servers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AgentImpl)) {
            return false;
        }
        final AgentImpl that = (AgentImpl)obj;
        return Objects.equals(name, that.name)
               && Objects.equals(version, that.version)
               && getDependencies().equals(that.getDependencies())
               && getProperties().equals(that.getProperties())
               && Objects.equals(script, that.script)
               && Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(version);
        hash = 31 * hash + getDependencies().hashCode();
        hash = 31 * hash + getProperties().hashCode();
        hash = 31 * hash + Objects.hashCode(script);
        hash = 31 * hash + Objects.hashCode(servers);
        return hash;
    }

    @Override
    public String toString() {
        return "AgentImpl{" +
               "name='" + name + '\'' +
               ", version='" + version + '\'' +
               ", dependencies='" + dependencies + '\'' +
               ", servers='" + servers + '\'' +
               ", properties='" + properties + "\'}";
    }
}

