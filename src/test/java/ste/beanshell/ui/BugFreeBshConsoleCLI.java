/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ste.beanshell.ui;

import java.io.File;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import ste.xtest.cli.BugFreeCLI;


/**
 *
 * @author ste
 */
public class BugFreeBshConsoleCLI extends BugFreeCLI {
    @Before
    public void before() throws Exception {
        STDOUT.clearLog();
    }
    
    @Test(timeout = 500)
    public void static_invocation() throws Exception {
        BshConsoleCLI.main("--help");
        thenSTDOUTContains("Usage: ste.beanshell.ui.BshConsoleCLI");
    }
    
    @Test(timeout = 500)
    public void show_help_if_comand_is_help() throws Exception {
        new BshConsoleCLI().launch("--help");
        
        thenSTDOUTContains("Usage: ste.beanshell.ui.BshConsoleCLI");
    }
    
    @Test(timeout = 500)
    public void read_init_script() throws Exception {
        new BshConsoleCLI().launch("--welcome", "--init", "src/test/scripts/init1.bsh");
        
        thenSTDOUTContains("Welcome to BshConsole v1");
        
        STDOUT.clearLog();
        new BshConsoleCLI().launch("--welcome", "--init", "src/test/scripts/init2.bsh");
        
        thenSTDOUTContains("Welcome to BshConsole v2");
    }
    
    @Test(timeout = 1000)
    public void error_if_init_script_not_found() throws Exception {
        final String FILE = "noscript";
        new BshConsoleCLI().launch("--init", FILE);
        
        then(STDOUT.getLog()).contains(
            "error: invalid initialization script " +
            new File(FILE).getAbsolutePath() 
        );
    }
    
    // --------------------------------------------------------- private methods
    
    private void thenSTDOUTContains(String s) throws InterruptedException {
        //
        // waiting for the thread to complete...
        //
        int i = 0;
        while(true) {
            System.out.println(">> " + STDOUT.getLog());
            if (STDOUT.getLog().contains(s) || (++i < 20)) {
                break;
            }
            Thread.sleep(50);
        }
        
        if (i >= 20) {
            then(STDOUT.getLog()).contains(s);
        }
    }
}
