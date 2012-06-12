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

import hudson.Extension;
import hudson.model.Hudson;
import hudson.remoting.ChannelClosedException;
import groovy.lang.Binding;
import groovy.lang.Closure;
import hudson.cli.CLICommand;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.codehaus.groovy.tools.shell.Shell;
import org.codehaus.groovy.tools.shell.util.XmlCommandRegistrar;

import java.util.List;
import java.util.Locale;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.PrintWriter;

import jline.UnsupportedTerminal;
import jline.Terminal;

/**
 * Executes Groovy shell.
 *
 * @author Kohsuke Kawaguchi, Winston Prakash
 */
@Extension
public class GroovyshCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Runs an interactive groovy shell";
    }

    @Override
    public int main(List<String> args, Locale locale, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        // this allows the caller to manipulate the JVM state, so require the admin privilege.
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        // TODO: ^as this class overrides main() (which has authentication stuff),
        // how to get ADMIN permission for this command?

        // this being remote means no jline capability is available
        System.setProperty("jline.terminal", UnsupportedTerminal.class.getName());
        Terminal.resetTerminal();

        Groovysh shell = createShell(stdin, stdout, stderr);
        return shell.run(args.toArray(new String[args.size()]));
    }

    protected Groovysh createShell(InputStream stdin, PrintStream stdout,
            PrintStream stderr) {

        Binding binding = new Binding();
        // redirect "println" to the CLI
        binding.setProperty("out", new PrintWriter(stdout, true));
        binding.setProperty("hudson", hudson.model.Hudson.getInstance());

        IO io = new IO(new BufferedInputStream(stdin), stdout, stderr);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Closure registrar = new Closure(null, null) {

            public Object doCall(Object[] args) {
                assert (args.length == 1);
                assert (args[0] instanceof Shell);

                Shell shell = (Shell) args[0];
                XmlCommandRegistrar r = new XmlCommandRegistrar(shell, cl);
                r.register(GroovyshCommand.class.getResource("commands.xml"));

                return null;
            }
        };
        Groovysh shell = new Groovysh(cl, binding, io, registrar);
        shell.getImports().add("import hudson.model.*");

        // defaultErrorHook doesn't re-throw IOException, so ShellRunner in
        // Groovysh will keep looping forever if we don't terminate when the
        // channel is closed
        final Closure originalErrorHook = shell.getErrorHook();
        shell.setErrorHook(new Closure(shell, shell) {

            public Object doCall(Object[] args) throws ChannelClosedException {
                if (args.length == 1 && args[0] instanceof ChannelClosedException) {
                    throw (ChannelClosedException) args[0];
                }

                return originalErrorHook.call(args);
            }
        });

        return shell;
    }

    protected int run() {
        throw new UnsupportedOperationException();
    }
}
