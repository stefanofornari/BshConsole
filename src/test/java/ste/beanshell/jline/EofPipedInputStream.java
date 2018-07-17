/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package ste.beanshell.jline;

import java.io.IOException;
import java.io.InputStream;

public class EofPipedInputStream extends InputStream {

    private InputStream in;

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in != null ? in.read() : -1;
    }

    @Override
    public int available() throws IOException {
        return in != null ? in.available() : 0;
    }
}
