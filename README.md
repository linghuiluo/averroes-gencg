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

# TODOS
- implement more precise mapping of beans based on the key values
- implement random if statement around the calls to providers in Library.main
- (done) implement ObjectProvidersDetector
- (done) resolve dependencies @Bean @Provides

# Format

mvn com.coveo:fmt-maven-plugin:format 