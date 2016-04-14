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

package org.apache.slider.common.params;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.slider.core.exceptions.BadCommandArgumentsException;
import org.apache.slider.core.exceptions.UsageException;

import java.util.ArrayList;
import java.util.List;

@Parameters(commandNames = {SliderActions.ACTION_RESIZE_CONTAINERS},
    commandDescription = SliderActions.DESCRIBE_ACTION_RESIZE_CONTAINERS)

public class ActionResizeContainerArgs extends AbstractActionArgs {
  @Override
  public String getActionName() {
    return SliderActions.ACTION_RESIZE_CONTAINERS;
  }

  @Parameter(names = {ARG_CONTAINERS}, variableArity = true,
      description = "resize specific containers")
  public List<String> containers = new ArrayList<>(0);

  @Parameter(names = {ARG_COMPONENTS}, variableArity = true,
      description = "resize all containers of specific components")
  public List<String> components = new ArrayList<>(0);

  @Parameter(names = {ARG_CONTAINER_MEM},
      description = "Target memory of the container")
  public int memory;

  @Parameter(names = {ARG_CONTAINER_VCORES},
      description = "Target virtual cores of the container")
  public int vCores;

  @Override
  public void validate() throws BadCommandArgumentsException, UsageException {
    super.validate();
    if (containers.isEmpty() && components.isEmpty()) {
      throw new BadCommandArgumentsException(ARG_CONTAINERS + " and "
          + ARG_COMPONENTS + " cannot be empty at the same time");
    }
    if (memory < 0) {
      throw new BadCommandArgumentsException(ARG_CONTAINER_MEM
          + " cannot be set to a negative number");
    }
    if (vCores < 0) {
      throw new BadCommandArgumentsException(ARG_CONTAINER_VCORES
          + " cannot be set to a negative number");
    }
    if (memory == 0 && vCores == 0) {
      throw new BadCommandArgumentsException(ARG_CONTAINER_MEM + " and "
          + ARG_CONTAINER_VCORES + " cannot be zero at the same time");
    }
  }
}
