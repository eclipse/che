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
package org.eclipse.che.api.devfile.server;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.fabric8.kubernetes.client.utils.Serialization.asYaml;
import static io.fabric8.kubernetes.client.utils.Serialization.unmarshal;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.eclipse.che.api.devfile.server.Constants.KUBERNETES_TOOL_TYPE;
import static org.eclipse.che.api.devfile.server.Constants.OPENSHIFT_TOOL_TYPE;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.List;
import java.util.Optional;
import javax.inject.Singleton;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.devfile.model.Tool;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.RecipeImpl;

/**
 * Creates {@link EnvironmentImpl} from specific tool in devfile if any.
 *
 * @author Max Shaposhnyk
 */
@Singleton
public class DevfileEnvironmentFactory {

  static final String DEFAULT_RECIPE_CONTENT_TYPE = "application/x-yaml";

  /**
   * Consumes an recipe-type tool (openshift or kubernetes) from devfile and tries to create {@link
   * EnvironmentImpl} from it (including filtering of list items using selectors, if necessary). An
   * {@link RecipeFileContentProvider} MUST be provided in order to fetch recipe content.
   *
   * @param recipeTool the recipe-type tool
   * @param recipeFileContentProvider service-specific provider of recipe file content
   * @return optional of constructed environment from recipe type tool
   * @throws BadRequestException when there is no content provider for recipe-type tool
   * @throws BadRequestException when recipe-type tool content is unreachable or empty
   */
  public Optional<EnvironmentImpl> createEnvironment(
      Tool recipeTool, RecipeFileContentProvider recipeFileContentProvider)
      throws BadRequestException {
    final String type = recipeTool.getType();
    if (!KUBERNETES_TOOL_TYPE.equals(type) && !OPENSHIFT_TOOL_TYPE.equals(type)) {
      throw new BadRequestException("Environment cannot be created from such type of tool.");
    }
    if (recipeFileContentProvider == null) {
      throw new BadRequestException(
          format("There is no content provider registered for '%s' type tools.", type));
    }

    String recipeFileContent = recipeFileContentProvider.fetchContent(recipeTool.getLocal());
    if (isNullOrEmpty(recipeFileContent)) {
      throw new BadRequestException(
          format(
              "The local file '%s' defined in tool  '%s' is unreachable or empty.",
              recipeTool.getLocal(), recipeTool.getName()));
    }
    try {
      final KubernetesList list = unmarshal(recipeFileContent, KubernetesList.class);

      if (recipeTool.getSelector() != null && !recipeTool.getSelector().isEmpty()) {
        List<HasMetadata> itemsList = list.getItems();
        itemsList.removeIf(
            e ->
                !e.getMetadata()
                    .getLabels()
                    .entrySet()
                    .containsAll(recipeTool.getSelector().entrySet()));
        list.setItems(itemsList);
      }
      RecipeImpl recipe = new RecipeImpl(type, DEFAULT_RECIPE_CONTENT_TYPE, asYaml(list), null);
      return Optional.of(new EnvironmentImpl(recipe, emptyMap()));
    } catch (KubernetesClientException ex) {
      throw new BadRequestException(
          format(
              "Unable to serialize or deserialize specified local file content for tool '%s'",
              recipeTool.getName()));
    }
  }
}
