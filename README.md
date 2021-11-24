# Build 
1. Install probe.jar to local .m2 repositry
-  `cd libs`
-  `./install.sh` (linux) , `install.bat` (windows)
2. Build with maven
-  `cd ..`
-  `mvn install -DskipTests`

# Analyze Spring Apps
~~~
-f "SPRING" 
-a <path to application jar file>  
-o <path to output folder> 
-r <application package name in regular expression> 
~~~

# Analyze Android Apks
~~~
-f "ANDROID" 
-a <path to apk file> 
-o <path to ouput folder> 
-l <path to android platform jars> 
~~~