# GenCG
## Usage
- Make sure you have Java 8 installed. Java 9+ does not work, as `rt.jar` is removed.
- Download the latest build artifact `averroes-gencg-0.0.1-jar-with-dependencies.jar` from the [releases](https://github.com/linghuiluo/averroes-gencg/releases) page. 
### 1. Generate Model For Android Apps
Run the following command:
```
java -jar averroes-gencg-0.0.1-jar-with-dependencies.jar -f ANDROID -j system
-a $APK -l $ANDROID_PLATFORMS c $CONFIG -o $MODEL_OUTPUT_DIR
```
- `$APK`: path to your Android APK file
- `$ANDROID_PLATFORMS`: path to android platform jars
- `$CONFIG`: path to the config folder in this repository
- `$MODEL_OUTPUT_DIR`: path to output the model

### 2. Generate Model For Spring Apps
```
java -jar averroes-gencg-0.0.1-jar-with-dependencies.jar -f SPRING -j system
-a $APP -c $CONFIG -o $MODEL_OUTPUT_DIR
```
- `$APP`: path to your Spring app JAR file
- `$CONFIG`: path to the config folder in this repository
- `$MODEL_OUTPUT_DIR`: path to output the model

## Build 
Make sure you have Maven (3.8.5 worked for me) installed. 
1. Build soot and install
```
git clone https://github.com/soot-oss/soot.git
cd soot
git checkout fe86c3e73d9b7bcd810dadd2aa81351d4288642d
mvn install -DskipTests
```
2. Build GenCG and install 
```
https://github.com/linghuiluo/averroes-gencg.git
cd averroes-gencg
cd libs
./install.sh (linux)  
install.bat (windows)
cd ..
mvn install -DskipTests
```

### Auto-format Code
```
mvn com.coveo:fmt-maven-plugin:format 
```
# Publication 
**Long-term Static Analysis Rule Quality Monitoring Using True Negatives**, 14/05/2023 - 20/05/2023, 45th International Conference on Software Engineering, Software Engineering in Practice (SEIP) Track, Melbourne, Australia.

Linghui Luo, Rajdeep Mukherjee, Omer Tripp, Martin Schäf, Qiang Zhou, Daniel Sanchez
