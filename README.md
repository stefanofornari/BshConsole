# BshConsole
A BeanShell console with history, vars/commands completion and more...

# Relationship with Beanshell
This project is not a beanshell fork. Beanshell is the best Java interpreter out there, but it missed a modern command line console with arrows handling, history, auto-completion etc. BshConsole wishes to fill this gap.

Please note that BshConsole uses a specialization of the standard bsh.Interpreter, but it should be completely compatible. If you notice any difference in parsing/execution behaviour between Beanshell and BsgConsole please file an issue.

# Distribution
The easiest way to get BshConsole is to download the distribution package from maven central at:

# Building from source
You can build BshConsole from source as well, with the following caveat: BshConsole is based on the latest (HEAD/SNAPHOT) Beanshell 2.1.0; this has not been released yet and is currently (Aug 4th, 2018) available only building from source as a maven snapshot. This prevented to release a version of BshConsole. To overcome this problem, BshConsole uses a released version of a Beanshell snapshot from the fork https://github.com/stefanofornari/beanshell. You should be able to replace a fresh build of the Beanshell jar into <bshconsole>/lib without any problem. If you encouter any issues, please open a ticket.
