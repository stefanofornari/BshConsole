/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package ste.beanshell.jline;

import bsh.Parser;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jline.reader.Completer;
import org.jline.reader.Expander;
import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;

import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class BshLineReaderBuilder {

    public static BshLineReaderBuilder builder() {
        return new BshLineReaderBuilder();
    }

    Terminal terminal;
    String appName;
    Map<String, Object> variables = new HashMap<>();
    Map<LineReader.Option, Boolean> options = new HashMap<>();
    History history;
    Completer completer;
    History memoryHistory;
    Highlighter highlighter;
    Parser parser;
    Expander expander;

    private BshLineReaderBuilder() {
    }

    public BshLineReaderBuilder terminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }

    public BshLineReaderBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public BshLineReaderBuilder variables(Map<String, Object> variables) {
        Map<String, Object> old = this.variables;
        this.variables = Objects.requireNonNull(variables);
        this.variables.putAll(old);
        return this;
    }

    public BshLineReaderBuilder variable(String name, Object value) {
        this.variables.put(name, value);
        return this;
    }

    public BshLineReaderBuilder option(LineReader.Option option, boolean value) {
        this.options.put(option, value);
        return this;
    }

    public BshLineReaderBuilder history(History history) {
        this.history = history;
        return this;
    }

    public BshLineReaderBuilder completer(Completer completer) {
        this.completer = completer;
        return this;
    }

    public BshLineReaderBuilder highlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
        return this;
    }

    public BshLineReaderBuilder expander(Expander expander) {
        this.expander = expander;
        return this;
    }

    public BshLineReader build() {
        Terminal terminal = this.terminal;
        if (terminal == null) {
            try {
                terminal = TerminalBuilder.terminal();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        BshLineReader reader = new BshLineReader(terminal, appName, variables);
        if (history != null) {
            reader.setHistory(history);
        } else {
            if (memoryHistory == null) {
                memoryHistory = new DefaultHistory();
            }
            reader.setHistory(memoryHistory);
        }
        if (completer != null) {
            reader.setCompleter(completer);
        }
        if (highlighter != null) {
            reader.setHighlighter(highlighter);
        }
        if (expander != null) {
            reader.setExpander(expander);
        }
        for (Map.Entry<LineReader.Option, Boolean> e : options.entrySet()) {
            reader.option(e.getKey(), e.getValue());
        }
        return reader;
    }

}
