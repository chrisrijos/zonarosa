/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.workers;

import io.dropwizard.servlets.tasks.Task;
import io.zonarosa.server.util.logging.RequestLogManager;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class SetRequestLoggingEnabledTask extends Task {

    public SetRequestLoggingEnabledTask() {
        super("set-request-logging-enabled");
    }

    @Override
    public void execute(final Map<String, List<String>> parameters, final PrintWriter out) {
        if (parameters.containsKey("enabled") && parameters.get("enabled").size() == 1) {
            final boolean enabled = Boolean.parseBoolean(parameters.get("enabled").get(0));

            RequestLogManager.setRequestLoggingEnabled(enabled);

            if (enabled) {
                out.println("Request logging now enabled");
            } else {
                out.println("Request logging now disabled");
            }
        } else {
            out.println("Usage: set-request-logging-enabled?enabled=[true|false]");
        }
    }
}
