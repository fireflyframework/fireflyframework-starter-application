/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.application.plugin.exception;

/**
 * Exception thrown when a process plugin cannot be found in the registry.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>A process mapping references a process ID that is not registered</li>
 *   <li>A specific version of a process is requested but not available</li>
 *   <li>A process was unloaded or failed to initialize</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
public class ProcessNotFoundException extends RuntimeException {
    
    private final String processId;
    private final String processVersion;
    
    /**
     * Creates an exception for a missing process.
     * 
     * @param processId the process ID that was not found
     */
    public ProcessNotFoundException(String processId) {
        super("Process not found: " + processId);
        this.processId = processId;
        this.processVersion = null;
    }
    
    /**
     * Creates an exception for a missing process version.
     * 
     * @param processId the process ID
     * @param processVersion the version that was not found
     */
    public ProcessNotFoundException(String processId, String processVersion) {
        super("Process not found: " + processId + " (version: " + processVersion + ")");
        this.processId = processId;
        this.processVersion = processVersion;
    }
    
    /**
     * Creates an exception with a custom message.
     * 
     * @param processId the process ID
     * @param message the custom message
     */
    public ProcessNotFoundException(String processId, String message, boolean customMessage) {
        super(message);
        this.processId = processId;
        this.processVersion = null;
    }
    
    /**
     * Gets the process ID that was not found.
     * 
     * @return the process ID
     */
    public String getProcessId() {
        return processId;
    }
    
    /**
     * Gets the process version that was requested.
     * 
     * @return the process version, or null if no specific version was requested
     */
    public String getProcessVersion() {
        return processVersion;
    }
}
