/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package ste.bshell.jline;

import ste.bshell.jline.BshLineReader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.jline.reader.Candidate;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

public class TestLineReader extends BshLineReader {

    public boolean list = false;
    public boolean menu = false;
    public EofPipedInputStream in;

    public TestLineReader(Terminal terminal, String appName, Map<String, Object> variables, EofPipedInputStream in) {
        super(terminal, appName, variables);
        this.in = in;
    }

    public TestLineReader(Terminal terminal, String appName, Map<String, Object> variables, InputStream in) {
        super(terminal, appName, variables);
        this.in = new EofPipedInputStream(in);
    }

    @Override
    protected boolean doList(List<Candidate> possible, String completed, boolean runLoop, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
        list = true;
        return super.doList(possible, completed, runLoop, escaper);
    }

    @Override
    protected boolean doMenu(List<Candidate> possible, String completed, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
        menu = true;
        return super.doMenu(possible, completed, escaper);
    }

    public AttributedString getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = (prompt == null) ? new AttributedString(""): new AttributedString(prompt);
    }
}
