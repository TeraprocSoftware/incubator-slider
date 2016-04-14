/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.slider.server.appmaster.actions;

import com.google.common.base.Preconditions;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.util.resource.Resources;
import org.apache.slider.server.appmaster.SliderAppMaster;
import org.apache.slider.server.appmaster.operations.AbstractRMOperation;
import org.apache.slider.server.appmaster.operations
    .ContainerChangeRequestOperation;
import org.apache.slider.server.appmaster.operations.RMOperationHandlerActions;
import org.apache.slider.server.appmaster.state.AppState;
import org.apache.slider.server.appmaster.state.RoleInstance;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ActionRequestContainersResize extends AsyncAction {

  private final Resource targetResource;
  private final Set<String> containerIds = new HashSet<>();
  private final Set<String> components = new HashSet<>();
  private final RMOperationHandlerActions operationHandler;

  /**
   * Change resource of a container
   * @param containerIds a list of container Ids
   * @param components a list of components
   * @param targetResource resource to change to
   * @param operationHandler handler for the operation
   */
  public ActionRequestContainersResize(
      Resource targetResource,
      List<String> containerIds,
      List<String> components,
      long delay,
      TimeUnit timeUnit,
      RMOperationHandlerActions operationHandler) {
    super("request container resize", delay, timeUnit);
    this.targetResource = targetResource;
    this.operationHandler = operationHandler;
    this.containerIds.addAll(containerIds);
    this.components.addAll(components);
  }

  private boolean verifyContainerResourceChangeRequest(RoleInstance instance) {
    ContainerId containerId = instance.getContainerId();
    Resource currentResource = instance.getContainerResource();
    if (currentResource == null) {
      SliderAppMaster.getLog().warn("Skip resizing container {} whose"
          + " current resource allocation is unknown.", containerId);
      return false;
    }
    if (!Resources.fitsIn(currentResource, targetResource) &&
        !Resources.fitsIn(targetResource, currentResource)) {
      SliderAppMaster.getLog().warn("Skip resizing container {} from {} to {}. "
          + "Not all indices of the target resource are larger or smaller than "
          + "the original resource.", containerId, currentResource,
          targetResource);
      return false;
    }
    // It is ok to have target resource equal to original resource. This may
    // be needed to cancel a resource increase reservation.
    return true;
  }

  @Override
  public void execute(
      SliderAppMaster appMaster, QueueAccess queueService, AppState appState)
      throws Exception {
    Resource maxResource = appState.getMaxContainerResource();
    Resource minResource = appState.getMinContainerResource();
    if (!Resources.fitsIn(minResource, targetResource) ||
        !Resources.fitsIn(targetResource, maxResource)) {
      SliderAppMaster.getLog().error("Failed to resize containers. The target "
          + "resource {} is beyond container resource limit. "
          + "The minimum resource is {}. The maximum resource is {}.",
          targetResource, minResource, maxResource);
      return;
    }
    // Get all containers that we are interested in
    if (!components.isEmpty()) {
      Map<String, List<String>> roleToInstanceMap =
          appState.createRoleToInstanceMap();
      for (String component : components) {
        List<String> roleContainers = roleToInstanceMap.get(component);
        if (roleContainers != null) {
          containerIds.addAll(roleContainers);
        }
      }
    }
    List<RoleInstance> instances =
        appState.getLiveInstancesByContainerIDs(containerIds);
    if (instances.isEmpty()) {
      SliderAppMaster.getLog().error("Failed to resize containers. The "
          + "containers {} and components {} cannot be found.",
          containerIds, components);
      return;
    }
    List<AbstractRMOperation> opsList = new LinkedList<>();
    for (RoleInstance instance : instances) {
      if (!verifyContainerResourceChangeRequest(instance)) {
        continue;
      }
      ContainerChangeRequestOperation resize =
          new ContainerChangeRequestOperation(
              instance.getContainer(), targetResource);
      opsList.add(resize);
    }
    //now apply the operations
    operationHandler.execute(opsList);
  }
}
