FOLDERS
=======
1. jad/ - decompiled class files form wmqtt.jar that might be useful for implementing usernames and passwords
2. resources/ - raw resources, not to be packaged with the application
3. web/ - The server-side code, excluding the broker

HOW TO EDIT wmqtt.jar
=====================
1. Create a new **java** project in eclipse to be used for recompiling .java files we decompile
2. Right click project, configure build path, and add wmqtt.jar as an external jar
3. Decompile the .class file with jad: ./jad /path/to/file.class (Ignore warnings)
4. Add resulting .jad file to the eclipse project as a .java file
5. In the file, hover over "package com.ibm.mqtt;" and accept the quickfix "move to package com.ibm.mqtt"
6. Go to Source -> Organize Imports (This will add the necessary imports for the .java class you decompiled from wmqtt.jar)
7. Ignore Any remaining warnings in the file
8. Right click on the com.ibm.mqtt package in the package explorer and select "Export…"
9. Select Java -> Jar file and click next
10. check ON export generated class files and resources
11. check OFF export all output folders for checked projects
12. check ON export java source files and resources
13. check OFF export refactorings for checked projects
14. Select an ambiguous jar name and location, doesn't matter
15. check OFF compress the contents of this jar
16. check ON add directory entries
17. check ON overwrite existing files without warning
18. Hit finish, and ignore compile warnings
19. Extract the .class file from the jar you just compiled
20. Create a new directory, with the following structure:
----/newdir/wmqtt.jar
----/newdir/com/ibm/mqtt/someclass.class
21. At the command line, cd into newdir
22. Run: jar -uf wmqtt.jar com/ibm/mqtt/someclass.class
23. wmqtt.jar has now been updated with the new modified class. Refresh any projects using the jar


Resources
==========
1. http://pecl.php.net/package/sam/1.1.0 - PHP SAM library
2. http://mosquitto.org/ - Mosquitto
3. http://www-01.ibm.com/support/docview.wss?rs=171&uid=swg24006006 - wmqtt.jar
4. https://github.com/tokudu/AndroidPushNotificationsDemo
5. https://github.com/tokudu/PhpMQTTClient
6. http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connect - MQTT v3.1 Protocol Specification
7. http://www.varaneckas.com/jad - Java Decompiler
8. http://www.ibm.com/developerworks/websphere/library/techarticles/0508_oconnell/0508_oconnell.html - IBM article on using wmqtt