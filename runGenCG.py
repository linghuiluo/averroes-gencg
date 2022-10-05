import os
import subprocess
import sys
import glob
import shutil
import time

# Run FlowDroid GenCG on CGBench
dg = "/Users/llinghui/Projects/Dragonglass/src/DgTypeStateAnalyzer/build/lib/DgTypeStateAnalyzer-1.0.jar"
averroes = "/Users/llinghui/Projects/GitHub/averroes-gencg/target/averroes-gencg-0.0.1-jar-with-dependencies.jar"
averroesConfig ="/Users/llinghui/Projects/GitHub/averroes-gencg/config"
guice = "/Users/llinghui/.m2/repository/com/google/inject/guice/4.1.0/guice-4.1.0.jar"
outputDir = "/Users/llinghui/Projects/GitHub/dg-GenCG-output"

logFileName = outputDir + "/logdg-GenCGcg.txt"
if os.path.exists(outputDir):
    shutil.rmtree(outputDir)
os.mkdir(outputDir) 
logfile = open(logFileName,'a')

apps= "/Users/llinghui/Projects/GitHub/packagecrawler/jars/internal"
               
def printOutput(process, logfile):
     while True:
            nextline = process.stderr.readline()
            if nextline == '' and process.poll() is not None:
                break
            logfile.write(nextline)
            logfile.flush()
            sys.stdout.write(nextline)
            sys.stdout.flush()
            
def executeAverroesGencg(app,averroesOutputDir):          
        cmd=["java", "-jar", averroes,
                 "-a", app, 
                 "-o", averroesOutputDir,
                 "-l", guice,
                 "-c", averroesConfig]
        print("Executing Averroes-GenCG on "+app)
        print(cmd)
        process =  subprocess.Popen(cmd, stderr=subprocess.PIPE, universal_newlines=True, encoding='utf-8')
        printOutput(process, logfile)

def executeDragonglass(app, appName, averroesOutputDir):
        instrumentedApp = averroesOutputDir + "/instrumented-app.jar"
        library = averroesOutputDir + "/averroes-lib-class.jar"
        library += os.pathsep + averroesOutputDir+"/placeholder-lib.jar"
        outputXML=outputDir+"/"+appName+"_result.xml"
        sourcessinks=os.path.dirname(app)+"/"+appName+"_SourcesAndSinks.txt"
        cmd=["java", "-Xmx8g", "-jar", dg]
        print("Executing Dragonglass on "+app)
        print(cmd)
        process =  subprocess.Popen(cmd, stderr=subprocess.PIPE, universal_newlines=True, encoding='utf-8')
        printOutput(process,logfile)
        
def collectSerializedCallGraph():
    for dirname, _, filenames in os.walk(apps):
        for f in filenames:
             if f.endswith(".json"):
                fromPath = os.path.join(apps, f)
                toPath = os.path.join(outputDir, os.path.basename(dirname))
                if not os.path.exists(toPath):
                    os.mkdir(toPath) 
                toPath = os.path.join(toPath , f)
                shutil.copyfile(fromPath, toPath)
                    
def run(apps, outputDir):
    count = 0
    with open(os.path.join(outputDir, "time.csv"), 'a') as f:  
        timeSpent = "Benchmark App; Analysis Time (s)\n"
        f.write(timeSpent)
        sum = 0
        for dirname, _, filenames in os.walk(apps):
            for app in filenames:
                if app.endswith(".jar"):
                    count+=1
                    print(str(count)+": start analyzing "+app)
                    appName = os.path.splitext(os.path.basename(app))[0]                  
                    averroesOutputDir = os.path.join(outputDir,appName)
                    if not os.path.exists(averroesOutputDir):
                        os.mkdir(averroesOutputDir)
                    executeAverroesGencg(os.path.join(apps,app),averroesOutputDir)    
                    # startTime = round(time.time(), 3)
                    # print("StartTime: "+str(startTime))
                    # executeDragonglass(os.path.join(apps,app),appName, averroesOutputDir)               
                    # endTime = round(time.time(), 3)
                    # print("EndTime: "+str(endTime))
                    # elapsedTime = endTime - startTime
                    # print("ElapsedTime: "+str(elapsedTime))
                    # timeSpent = appName + ";" + str(round(elapsedTime,2))+"\n"
                    # sum += round(elapsedTime, 2)
                    # f.write(timeSpent)
                    print()            
        #f.write("Sum;"+str(sum))
        
run(apps,outputDir)
#collectSerializedCallGraph()    