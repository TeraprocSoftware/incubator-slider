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

package org.apache.slider.core.exceptions;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.hadoop.fs.PathAccessDeniedException;
import org.apache.hadoop.fs.PathIOException;
import org.apache.hadoop.yarn.webapp.*;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * static methods to convert exceptions into different types, including
 * extraction of details and finer-grained conversions.
 */
public class ExceptionConverter {

  /**
   * Uprate error codes 400 and up into faults; 
   * 404 is converted to a {@link FileNotFoundException},
   * 401 to {@link ForbiddenException}
   * FileNotFoundException for an unknown resource
   * PathAccessDeniedException for access denied
   * PathIOException for anything else
   * @param verb HTTP Verb used
   * @param targetURL URL being targeted 
   * @param exception original exception
   * @return a new exception, the original one nested as a cause
   */
  public static IOException convertJerseyException(String verb,
      String targetURL,
      UniformInterfaceException exception) {

    IOException ioe = null;
    ClientResponse response = exception.getResponse();
    if (response != null) {
      int status = response.getStatus();
      if (status == HttpServletResponse.SC_UNAUTHORIZED
          || status == HttpServletResponse.SC_FORBIDDEN) {
        ioe = new PathAccessDeniedException(targetURL);
      }
      if (status >= 400 && status < 500) {
        ioe =  new FileNotFoundException(targetURL);
      }
    }

    if (ioe == null) {
      ioe = new PathIOException(targetURL, 
          verb + " " + targetURL + " failed: " + exception);
    }
    ioe.initCause(exception);
    return ioe; 
  }
}
