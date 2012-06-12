/*
 * The MIT License
 * 
 * Copyright (c) 2012, Oracle Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
 * BIRT Chart implementation for Hudson Graph Support
 *
 * @author Kohsuke Kawaguchi, Winston Prakash
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
