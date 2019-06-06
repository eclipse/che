/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server.model.impl.devfile;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.eclipse.che.api.core.model.workspace.devfile.Metadata;

@Embeddable
public class MetadataImpl implements Metadata {

  @Column(name = "meta_name")
  private String name;

  public MetadataImpl() {}

  public MetadataImpl(String name) {
    this.name = name;
  }

  public MetadataImpl(Metadata metadata) {
    this.name = metadata.getName();
  }

  @PrePersist
  @PreUpdate
  protected void validate() {
    if (this.name == null || this.name.isEmpty()) {
      throw new IllegalStateException("Devfile name must be non-empty.");
    }
  }

  @Override
  public String getName() {
    return null;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataImpl metadata = (MetadataImpl) o;
    return name.equals(metadata.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "MetadataImpl{" + "name='" + name + '\'' + '}';
  }
}
