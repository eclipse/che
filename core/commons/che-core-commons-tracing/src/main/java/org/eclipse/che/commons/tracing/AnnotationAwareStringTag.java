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
package org.eclipse.che.commons.tracing;

import com.google.common.annotations.Beta;
import io.opentracing.tag.StringTag;
import java.util.function.Supplier;
import org.eclipse.che.commons.annotation.Traced;

/**
 * A specialization of the {@link StringTag} that adds support for setting a tag in a {@link
 * Traced @Traced} method.
 */
@Beta
public class AnnotationAwareStringTag extends StringTag {

  public AnnotationAwareStringTag(String key) {
    super(key);
  }

  /**
   * Sets the value of the tag for the span of the {@link Traced @Traced} method.
   *
   * @param value the value to set
   */
  public void set(String value) {
    set(() -> value);
  }

  /**
   * Sets the value of the tag for the span of the {@link Traced @Traced} method.
   *
   * @param value the supplier of the value to set
   */
  public void set(Supplier<String> value) {
    Traced.Tags.addString(getKey(), value);
  }
}
