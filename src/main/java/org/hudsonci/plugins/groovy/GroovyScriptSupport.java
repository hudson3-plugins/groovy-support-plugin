/**
 * *****************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Winston Prakash
 *
 ******************************************************************************
 */
package org.hudsonci.plugins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import java.io.PrintWriter;
import java.util.Map;
import org.eclipse.hudson.script.ScriptSupport;
import org.eclipse.hudson.script.ScriptSupportDescriptor;

/**
 * @author Winston Prakash
 */
public class GroovyScriptSupport extends ScriptSupport {

    @Override
    public boolean hasSupport(String scriptType) {
        return ScriptSupport.SCRIPT_GROOVY.equals(scriptType);
    }

    @Override
    public String getType() {
        return ScriptSupport.SCRIPT_GROOVY;
    }

    @Override
    public Object evaluateExpression(String expression) {
        return evaluateExpression(expression, null);
    }

    @Override
    public Object evaluateExpression(String expression, Map<String, Object> variableMap) {
        Binding binding = new Binding();
        if (variableMap != null) {
            for (Map.Entry<String, Object> e : variableMap.entrySet()) {
                binding.setVariable(e.getKey(), e.getValue());
            }
        }
        GroovyShell shell = new GroovyShell(binding);

        return shell.evaluate(expression);
    }

    @Override
    public void evaluate(String script, PrintWriter printWriter) {
        evaluate(script, null, printWriter);
    }

    @Override
    public void evaluate(String script, Map<String, Object> variableMap, PrintWriter printWriter) {
        evaluate(null, script, variableMap, printWriter);
    }

    @Override
    public void evaluate(ClassLoader parentClassLoader, String script, Map<String, Object> variableMap, PrintWriter printWriter) {

        if (parentClassLoader == null) {
            parentClassLoader = Thread.currentThread().getContextClassLoader();
        }

        GroovyShell shell = new GroovyShell(parentClassLoader);

        if (variableMap != null) {
            for (Map.Entry<String, Object> e : variableMap.entrySet()) {
                shell.setVariable(e.getKey(), e.getValue());
            }
        }

        shell.setVariable("out", printWriter);
        try {

            Object output = shell.evaluate(script);
            if (output != null) {
                printWriter.print(output);
            }
        } catch (Throwable t) {
            t.printStackTrace(printWriter);
        }
    }

    @Extension
    public static class DescriptorImpl extends ScriptSupportDescriptor {

        @Override
        public String getDisplayName() {
            return "Groovy";
        }

        @Override
        public String getConfigPage() {
            return getViewPage(clazz, "scriptConsole.jelly");
        }
    }
}
