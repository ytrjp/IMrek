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
1. http://pecl.php.net/package/sam/1.1.0
2. https://www.ibm.com/developerworks/community/groups/service/html/communityview?communityUuid=d5bedadd-e46f-4c97-af89-22d65ffee070
3. http://www-01.ibm.com/support/docview.wss?rs=171&uid=swg24006006
4. https://github.com/tokudu/AndroidPushNotificationsDemo
5. https://github.com/tokudu/PhpMQTTClient