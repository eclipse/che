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

import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.workspace.server.DtoConverter.asDto;
import static org.eclipse.che.api.workspace.server.WorkspaceKeyValidator.validateKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.devfile.model.Devfile;
import org.eclipse.che.api.workspace.server.WorkspaceLinksGenerator;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.commons.env.EnvironmentContext;

@Api(value = "/devfile", description = "Devfile REST API")
@Path("/devfile")
public class DevfileService extends Service {

  private WorkspaceLinksGenerator linksGenerator;
  private DevfileSchemaValidator schemaValidator;
  private DevfileIntegrityValidator integrityValidator;
  private DevfileSchemaProvider schemaCachedProvider;
  private WorkspaceManager workspaceManager;
  private ObjectMapper objectMapper;
  private DevfileConverter devfileConverter;

  @Inject
  public DevfileService(
      WorkspaceLinksGenerator linksGenerator,
      DevfileSchemaValidator schemaValidator,
      DevfileIntegrityValidator integrityValidator,
      DevfileSchemaProvider schemaCachedProvider,
      WorkspaceManager workspaceManager) {
    this.linksGenerator = linksGenerator;
    this.schemaValidator = schemaValidator;
    this.integrityValidator = integrityValidator;
    this.schemaCachedProvider = schemaCachedProvider;
    this.workspaceManager = workspaceManager;
    this.objectMapper = new ObjectMapper(new YAMLFactory());
    this.devfileConverter = new DevfileConverter();
  }

  /**
   * Retrieves the json schema.
   *
   * @return json schema
   */
  @GET
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves current version of devfile JSON schema")
  @ApiResponses({
    @ApiResponse(code = 200, message = "The schema successfully retrieved"),
    @ApiResponse(code = 500, message = "Internal server error occurred")
  })
  public Response getSchema() throws ServerException {
    try {
      return Response.ok(schemaCachedProvider.getSchemaContent()).build();
    } catch (IOException e) {
      throw new ServerException(e);
    }
  }

  /**
   * Creates workspace from provided devfile
   *
   * @param data devfile content
   * @param verbose return more explained validation error messages if any
   * @return created workspace configuration
   */
  @POST
  @Consumes({"text/yaml", "text/x-yaml", "application/yaml", "application/json"})
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Create a new workspace based on provided devfile",
      notes =
          "This operation can be performed only by authorized user,"
              + "this user will be the owner of the created workspace",
      response = WorkspaceDto.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The workspace successfully created"),
    @ApiResponse(
        code = 400,
        message =
            "Provided devfile syntactically incorrect, doesn't match with actual schema or has integrity violations"),
    @ApiResponse(code = 403, message = "The user does not have access to create a new workspace"),
    @ApiResponse(code = 500, message = "Internal server error occurred")
  })
  public Response createFromYaml(
      String data,
      @ApiParam(value = "Provide extended validation messages")
          @DefaultValue("false")
          @QueryParam("verbose")
          boolean verbose)
      throws ServerException, ConflictException, NotFoundException, ValidationException,
          BadRequestException {

    Devfile devFile;
    WorkspaceConfigImpl workspaceConfig;
    try {
      JsonNode parsed = schemaValidator.validateBySchema(data, verbose);
      devFile = objectMapper.treeToValue(parsed, Devfile.class);
      integrityValidator.validateDevfile(devFile);
      workspaceConfig = devfileConverter.devFileToWorkspaceConfig(devFile);
    } catch (IOException e) {
      throw new ServerException(e.getMessage());
    } catch (DevfileFormatException e) {
      throw new BadRequestException(e.getMessage());
    }

    final String namespace = EnvironmentContext.getCurrent().getSubject().getUserName();
    WorkspaceImpl workspace =
        workspaceManager.createWorkspace(findAvailableName(workspaceConfig), namespace, emptyMap());
    return Response.status(201)
        .entity(asDto(workspace).withLinks(linksGenerator.genLinks(workspace, getServiceContext())))
        .build();
  }

  /**
   * Generates the devfile based on an existing workspace. Key is workspace id or
   * namespace/workspace_name
   *
   * @see WorkspaceManager#getByKey(String)
   */
  @GET
  @Path("/{key:.*}")
  @Produces("text/yml")
  @ApiOperation(
      value = "Generates the devfile from given workspace",
      notes =
          "This operation can be performed only by authorized user,"
              + "this user must be the owner of the exported workspace")
  @ApiResponses({
    @ApiResponse(code = 200, message = "The workspace successfully exported"),
    @ApiResponse(code = 403, message = "The user does not have access to create a new workspace"),
    @ApiResponse(code = 500, message = "Internal server error occurred")
  })
  public Response createFromWorkspace(
      @ApiParam(
              value = "Composite key",
              examples =
                  @Example({
                    @ExampleProperty("workspace12345678"),
                    @ExampleProperty("namespace/workspace_name"),
                    @ExampleProperty("namespace_part_1/namespace_part_2/workspace_name")
                  }))
          @PathParam("key")
          String key)
      throws NotFoundException, ServerException, BadRequestException, ConflictException {
    validateKey(key);
    WorkspaceImpl workspace = workspaceManager.getWorkspace(key);
    try {
      Devfile workspaceDevFile = devfileConverter.workspaceToDevFile(workspace.getConfig());
      // Write object as YAML
      return Response.ok().entity(objectMapper.writeValueAsString(workspaceDevFile)).build();
    } catch (JsonProcessingException e) {
      throw new ServerException(e.getMessage(), e);
    } catch (WorkspaceExportException e) {
      throw new ConflictException(e.getMessage());
    }
  }

  private WorkspaceConfigImpl findAvailableName(WorkspaceConfigImpl config) throws ServerException {
    String nameCandidate = config.getName();
    String namespace = EnvironmentContext.getCurrent().getSubject().getUserName();
    int counter = 0;
    while (true) {
      try {
        workspaceManager.getWorkspace(nameCandidate, namespace);
        nameCandidate = config.getName() + "_" + ++counter;
      } catch (NotFoundException nf) {
        config.setName(nameCandidate);
        break;
      }
    }
    return config;
  }
}
