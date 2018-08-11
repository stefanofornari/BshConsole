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

/**
 *

 */
public class InterpreterEvent {

    public static final String READY = "READY";
    public static final String BUSY  = "BUSY" ;
    public static final String DONE  = "DONE" ;

    public String type;
    public BshConsoleInterpreter source;
    public Object data;

    public InterpreterEvent(BshConsoleInterpreter source, String type, Object data) {
        this.source = source;
        this.type = type;
        this.data = data;
    }

    public InterpreterEvent(BshConsoleInterpreter source, String type) {
        this(source, type, null);
    }
};
