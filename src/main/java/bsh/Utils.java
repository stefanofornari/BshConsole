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
package bsh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 */
public class Utils {
    public static Object source(Interpreter bsh, File file, NameSpace ns)
    throws FileNotFoundException, IOException, EvalError {
        Interpreter.debug("Sourcing file: ", file);
        Reader r = new FileReader(file);

        try {
            return bsh.eval(r, ns, file.getAbsolutePath());
        } finally {
            r.close();
        }
    }

    public static String getSourceFile(CallStack callstack) {
        final String COMPILED_CODE = "<input>";
        if (callstack == null) {
            return COMPILED_CODE;
        }

        if (callstack.node == null) {
            return COMPILED_CODE;
        }
        return callstack.node.getSourceFile();
    }
}
