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
package org.eclipse.che.plugin.svn.ide.sw;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.extension.machine.client.processes.panel.ProcessesPanelPresenter;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.plugin.svn.ide.SubversionClientService;
import org.eclipse.che.plugin.svn.ide.SubversionExtensionLocalizationConstants;
import org.eclipse.che.plugin.svn.ide.common.StatusColors;
import org.eclipse.che.plugin.svn.ide.common.SubversionActionPresenter;
import org.eclipse.che.plugin.svn.ide.common.SubversionOutputConsoleFactory;
import org.eclipse.che.plugin.svn.shared.CLIOutputResponse;
import org.eclipse.che.plugin.svn.shared.CLIOutputWithRevisionResponse;
import org.eclipse.che.plugin.svn.shared.InfoResponse;
import org.eclipse.che.plugin.svn.shared.SubversionItem;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Handler for the {@link org.eclipse.che.plugin.svn.ide.action.SwitchAction} action.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class SwitchPresenter extends SubversionActionPresenter implements SwitchView.ActionDelegate,
                                                                          LocationSelectorView.ActionDelegate {

    private final NotificationManager                      notificationManager;
    private final SubversionExtensionLocalizationConstants constants;
    private final SubversionClientService                  service;

    private final SwitchView           switchView;
    private final LocationSelectorView selectorView;
    private final NodesResources       resources;

    // Keep list of branches and tags to avoid unnecessary requests
    private List<String> branches;
    private List<String> tags;
    private String       projectUri;

    @Inject
    public SwitchPresenter(AppContext appContext,
                           NotificationManager notificationManager,
                           SubversionExtensionLocalizationConstants constants,
                           SwitchView switchView,
                           LocationSelectorView selectorView,
                           SubversionClientService service,
                           SubversionOutputConsoleFactory consoleFactory,
                           ProcessesPanelPresenter processesPanelPresenter,
                           StatusColors statusColors,
                           NodesResources resources) {
        super(appContext, consoleFactory, processesPanelPresenter, statusColors);

        this.notificationManager = notificationManager;
        this.constants = constants;

        this.service = service;
        this.switchView = switchView;
        this.resources = resources;
        this.switchView.setDelegate(this);
        this.selectorView = selectorView;
        this.selectorView.setDelegate(this);
    }

    public void showWindow() {
        final Project project = appContext.getRootProject();
        checkState(project != null);

        invalidateLoadedData();

        service.info(appContext.getRootProject().getLocation(), Path.valueOf("."), "HEAD", false).then(new Operation<InfoResponse>() {
            @Override
            public void apply(InfoResponse response) throws OperationException {
                if (!response.getItems().isEmpty()) {
                    SubversionItem subversionItem = response.getItems().get(0);
                    projectUri = subversionItem.getProjectUri();
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                notificationManager.notify("Error retrieving svn info", error.getMessage(), FAIL, EMERGE_MODE);
            }
        });

        switchView.showWindow();
        switchView.setSwitchRevisionEnabled(switchView.isSwitchToRevision());
        switchView.setLocationEnabled(switchView.isSwitchToOtherLocation());
        switchView.setLocation(composeSwitchLocation());
        switchView.setSelectOtherLocationButtonEnabled(switchView.isSwitchToOtherLocation());
        switchView.setWorkingCopyDepthEnabled(switchView.getDepth().isEmpty());
        switchView.setDepthEnabled(switchView.getWorkingCopyDepth().isEmpty());

        handleSwitchButton();
    }

    @Override
    public void onCancelClicked() {
        switchView.close();
        invalidateLoadedData();
    }

    @Override
    public void onSwitchClicked() {
        service.doSwitch(switchView.getLocation(),
                         appContext.getRootProject().getLocation(),
                         switchView.isSwitchToHeadRevision() ? "HEAD" : switchView.getRevision(),
                         switchView.getDepth(),
                         switchView.getWorkingCopyDepth(),
                         switchView.getAccept(),
                         switchView.isIgnoreExternals(),
                         switchView.isIgnoreAncestry(),
                         false,
                         switchView.isForce()).then(new Operation<CLIOutputWithRevisionResponse>() {
            @Override
            public void apply(CLIOutputWithRevisionResponse response) throws OperationException {
                printResponse(response.getCommand(), response.getOutput(), response.getErrOutput(), constants.commandSwitch());
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                notificationManager.notify("Error execution svn switch", error.getMessage(), FAIL, EMERGE_MODE);
            }
        });
    }

    @Override
    public void onSwitchToTrunkChanged() {
        switchView.setSwitchToLocationEnabled(false);
        switchView.setLocationEnabled(false);
        switchView.setPredefinedLocations(Collections.<String>emptyList());
        switchView.setLocation(composeSwitchLocation());
        switchView.setSelectOtherLocationButtonEnabled(false);

        handleSwitchButton();
    }

    @Override
    public void onSwitchToBranchChanged() {
        switchView.setLocationEnabled(false);
        switchView.setSwitchToLocationEnabled(true);
        switchView.setSelectOtherLocationButtonEnabled(false);

        if (branches != null) {
            switchView.setPredefinedLocations(branches);
            switchView.setLocation(composeSwitchLocation());
            handleSwitchButton();
            return;
        }

        service.listBranches(appContext.getRootProject().getLocation()).then(new Operation<CLIOutputResponse>() {
            @Override
            public void apply(CLIOutputResponse response) throws OperationException {
                branches = response.getOutput();
                switchView.setPredefinedLocations(branches);
                switchView.setLocation(composeSwitchLocation());
                handleSwitchButton();
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                branches = Collections.<String>emptyList();
                switchView.setPredefinedLocations(branches);
                switchView.setLocation(composeSwitchLocation());
                handleSwitchButton();

                notificationManager.notify("Error retrieving list of branches.", error.getMessage(), FAIL, EMERGE_MODE);
            }
        });
    }

    @Override
    public void onSwitchToTagChanged() {
        switchView.setLocationEnabled(false);
        switchView.setSwitchToLocationEnabled(true);
        switchView.setSelectOtherLocationButtonEnabled(false);

        if (tags != null) {
            switchView.setPredefinedLocations(tags);
            switchView.setLocation(composeSwitchLocation());
            handleSwitchButton();
            return;
        }

        service.listTags(appContext.getRootProject().getLocation()).then(new Operation<CLIOutputResponse>() {
            @Override
            public void apply(CLIOutputResponse arg) throws OperationException {
                tags = arg.getOutput();
                switchView.setPredefinedLocations(tags);
                switchView.setLocation(composeSwitchLocation());
                handleSwitchButton();
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                tags = Collections.<String>emptyList();
                switchView.setPredefinedLocations(tags);
                switchView.setLocation(composeSwitchLocation());
                handleSwitchButton();

                notificationManager.notify("Error retrieving list of tags.", error.getMessage(), FAIL, EMERGE_MODE);
            }
        });
    }

    @Override
    public void onSwitchToOtherLocationChanged() {
        switchView.setSwitchToLocationEnabled(false);
        switchView.setLocationEnabled(true);
        switchView.setSelectOtherLocationButtonEnabled(true);
        switchView.setPredefinedLocations(Collections.<String>emptyList());

        handleSwitchButton();
    }

    @Override
    public void onSwitchToHeadRevisionChanged() {
        switchView.setSwitchRevisionEnabled(false);

        handleSwitchButton();
    }

    @Override
    public void onSwitchToRevisionChanged() {
        switchView.setSwitchRevisionEnabled(true);

        handleSwitchButton();
    }

    @Override
    public void onRevisionUpdated() {
        handleSwitchButton();
    }

    @Override
    public void onSwitchLocationChanged() {
        switchView.setLocation(composeSwitchLocation());
    }

    @Override
    public void onSelectOtherLocationClicked() {
        selectorView.showWindow();

        SvnNode rootNode = new SvnNode(appContext.getRootProject().getLocation(),
                                       projectUri == null ? "^" : projectUri,
                                       service,
                                       notificationManager,
                                       resources);
        selectorView.setRootNode(rootNode);
    }

    @Override
    public void onDepthChanged() {
        switchView.setWorkingCopyDepthEnabled(switchView.getDepth().isEmpty());
    }

    @Override
    public void onWorkingCopyDepthChanged() {
        switchView.setDepthEnabled(switchView.getWorkingCopyDepth().isEmpty());
    }

    private String composeSwitchLocation() {
        if (switchView.isSwitchToTrunk()) {
            return projectUri == null ? "^/trunk" : (projectUri + "/trunk");

        } else if (switchView.isSwitchToBranch()) {
            return (projectUri == null ? "^/branches/"
                                       : (projectUri + "/branches/")) + switchView.getSwitchToLocation();

        } else if (switchView.isSwitchToTag()) {
            return (projectUri == null ? "^/tags/"
                                       : (projectUri + "/tags/")) + switchView.getSwitchToLocation();
        } else {
            return switchView.getLocation();
        }
    }

    private void invalidateLoadedData() {
        tags = null;
        branches = null;
        projectUri = null;
    }

    private void handleSwitchButton() {
        if (switchView.getLocation().isEmpty()) {
            switchView.setSwitchButtonEnabled(false);

        } else if (switchView.isSwitchToRevision() && switchView.getRevision().isEmpty()) {
            switchView.setSwitchButtonEnabled(false);

        } else {
            switchView.setSwitchButtonEnabled(true);
        }
    }

    @Override
    public void setSelectedNode(SvnNode node) {
        switchView.setLocation(node.getLocation());
    }
}
