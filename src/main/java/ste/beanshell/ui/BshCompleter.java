/*
 * Copyright (C) 2018 Stefano Fornari.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.beanshell.ui;

import bsh.EvalError;
import bsh.Interpreter;
import java.util.List;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedString;

/**
 *
 * @author ste
 */
public class BshCompleter implements Completer {
    
    private final Interpreter bsh;
    
    /**
     * 
     * @param bsh the beanshell interpreter - NOT NULL
     */
    public BshCompleter(Interpreter bsh) {
        if (bsh == null) {
            throw new IllegalArgumentException("bsh can not be null");
        }
        this.bsh = bsh;
    }

    @Override
    public void complete(LineReader reader, ParsedLine pl, List<Candidate> candidates) {
        try {
            for (String v : (String[])bsh.get("this.variables")) {
                candidates.add(new Candidate(AttributedString.stripAnsi(v), v, null, null, null, null, true));
            }
            
            for (String v : (String[])bsh.get("this.methods")) {
                candidates.add(new Candidate(AttributedString.stripAnsi(v) + "(", v, null, null, null, null, true));
            }
            
            String completes = (String)bsh.get("COMPLETES");
            if (completes != null) {
                for (String v : completes.split(",")) {
                    v = v.trim();
                    candidates.add(new Candidate(AttributedString.stripAnsi(v), v, null, null, null, null, true));
                }
            }
            
        } catch (EvalError x) {
            //
            // it should not happen...
            //
        }
    }
    
}
