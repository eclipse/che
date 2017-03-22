/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.agent.server.filters;

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CheJsonProvider;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ExtendedMachineDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.everrest.assured.EverrestJetty;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.dto.server.DtoFactory.cloneDto;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Garagatyi
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AddExecAgentInWorkspaceFilterTest {
    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private static final ApiExceptionMapper MAPPER = new ApiExceptionMapper();

    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private CheJsonProvider jsonProvider = new CheJsonProvider(new HashSet<>());

    @Mock
    private WorkspaceService                   workspaceService;
    @SuppressWarnings("unused")
    @Spy
    private AddExecAgentInWorkspaceFilter      filter;
    @Captor
    private ArgumentCaptor<WorkspaceConfigDto> workspaceConfigCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        when(workspaceService.create(any(WorkspaceConfigDto.class), anyListOf(String.class), anyBoolean(), anyString()))
                .thenReturn(javax.ws.rs.core.Response.status(201).build());
        when(workspaceService.startFromConfig(any(WorkspaceConfigDto.class), anyBoolean(), anyString()))
                .thenReturn(newDto(WorkspaceDto.class));
    }

    @Test(dataProvider = "environmentsProvider")
    public void shouldAddExecAgentIfNeededOnCreateWS(Map<String, EnvironmentDto> inputEnv,
                                                     Map<String, EnvironmentDto> expectedEnv) throws Exception {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newDto(WorkspaceConfigDto.class).withEnvironments(inputEnv))
                                         .when()
                                         .post(SECURE_PATH + "/workspace");

        assertEquals(response.getStatusCode(), 201);
        verify(workspaceService).create(workspaceConfigCaptor.capture(), any(), anyBoolean(), anyString());
        Map<String, EnvironmentDto> actualEnv = workspaceConfigCaptor.getValue().getEnvironments();
        assertEquals(actualEnv, expectedEnv);
    }

    @Test(dataProvider = "environmentsProvider")
    public void shouldAddExecAgentIfNeededOnStartWSFromConfig(Map<String, EnvironmentDto> inputEnv,
                                                              Map<String, EnvironmentDto> expectedEnv)
            throws Exception {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newDto(WorkspaceConfigDto.class).withEnvironments(inputEnv))
                                         .when()
                                         .post(SECURE_PATH + "/workspace/runtime");

        assertEquals(response.getStatusCode(), 200);
        verify(workspaceService).startFromConfig(workspaceConfigCaptor.capture(), anyBoolean(), anyString());
        Map<String, EnvironmentDto> actualEnv = workspaceConfigCaptor.getValue().getEnvironments();
        assertEquals(actualEnv, expectedEnv);
    }

    @DataProvider(name = "environmentsProvider")
    public static Object[][] environmentsProvider() {
        EnvironmentDto environment = newDto(EnvironmentDto.class);
        ExtendedMachineDto machine = newDto(ExtendedMachineDto.class);
        return new Object[][] {
                {singletonMap("e1", cloneDto(environment)),
                 singletonMap("e1", cloneDto(environment))},

                {emptyMap(), emptyMap()},

                {singletonMap("e1", cloneDto(environment).withMachines(singletonMap("m1", cloneDto(machine)))),
                 singletonMap("e1", cloneDto(environment).withMachines(singletonMap("m1", cloneDto(machine))))},

                {singletonMap("e1", cloneDto(environment)
                        .withMachines(singletonMap("m1", cloneDto(machine).withAgents(emptyList())))),
                 singletonMap("e1", cloneDto(environment)
                         .withMachines(singletonMap("m1", cloneDto(machine).withAgents(emptyList()))))},

                {singletonMap("e1", cloneDto(environment).withMachines(
                        singletonMap("m1", cloneDto(machine).withAgents(singletonList("org.eclipse.che.terminal1"))))),
                 singletonMap("e1", cloneDto(environment).withMachines(singletonMap("m1", cloneDto(machine)
                         .withAgents(singletonList("org.eclipse.che.terminal1")))))},

                {singletonMap("e1", cloneDto(environment).withMachines(
                        singletonMap("m1", cloneDto(machine).withAgents(singletonList("org.eclipse.che.terminal"))))),
                 singletonMap("e1", cloneDto(environment).withMachines(
                         singletonMap("m1", cloneDto(machine).withAgents(asList("org.eclipse.che.terminal",
                                                                                "org.eclipse.che.exec")))))},
        };
    }
}
