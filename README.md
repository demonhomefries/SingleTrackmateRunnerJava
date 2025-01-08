## Getting Started

Install [JDK8](https://alstherapydevelopmentinstitute.box.com/s/rski5xqwfbq1ae0v728cs9y7pj58cbyy) and make sure that the path variable is set correctly. Test this by typing `java -version` into your terminal. It should respond with something like 
```
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)
```
Install Python on your system and ensure that it's mapped to the path variable such that it can be invoked through your terminal. Ideally the path variable will be named `python`, but if it's something else you may need to modify the source code to make it work - check Common Errors 2. 

## Running the project
Drag the entire folder into VSCode and click run on the top right corner.
The first run will take some time to compile the project, but subsequent runs will be faster to initialize.

If you encounter an error, please read the error message and determine its source.

## Common Errors
1. If you see an error like this:
```
java.util.concurrent.ExecutionException: java.lang.OutOfMemoryError: Java heap space
        at java.util.concurrent.FutureTask.report(FutureTask.java:122)
        at java.util.concurrent.FutureTask.get(FutureTask.java:192)
        at fiji.plugin.trackmate.TrackMate.processFrameByFrame(TrackMate.java:590)
        at fiji.plugin.trackmate.TrackMate.execDetection(TrackMate.java:375)
        at fiji.plugin.trackmate.TrackMate.process(TrackMate.java:783)
        at KalmanTrackerRunner.runKalmanTracking(KalmanTrackerRunner.java:166)
        at Main.main(Main.java:77)
Caused by: java.lang.OutOfMemoryError: Java heap space
        at net.imglib2.img.basictypeaccess.array.AbstractIntArray.<init>(AbstractIntArray.java:50)
        at net.imglib2.img.basictypeaccess.array.IntArray.<init>(IntArray.java:48)
        at net.imglib2.img.basictypeaccess.array.IntArray.createArray(IntArray.java:59)
        at net.imglib2.img.basictypeaccess.array.IntArray.createArray(IntArray.java:42)
        at net.imglib2.img.array.ArrayImgFactory.create(ArrayImgFactory.java:93)
        at net.imglib2.img.array.ArrayImgFactory.create(ArrayImgFactory.java:69)
        at net.imglib2.img.array.ArrayImgFactory.create(ArrayImgFactory.java:76)
        at net.imglib2.img.array.ArrayImgFactory.create(ArrayImgFactory.java:58)
        at fiji.plugin.trackmate.detection.MaskUtils.toLabeling(MaskUtils.java:202)
        at fiji.plugin.trackmate.detection.MaskUtils.fromThreshold(MaskUtils.java:250)
        at fiji.plugin.trackmate.detection.ThresholdDetector.process(ThresholdDetector.java:126)
        at fiji.plugin.trackmate.TrackMate$1.call(TrackMate.java:518)
        at fiji.plugin.trackmate.TrackMate$1.call(TrackMate.java:498)
        at java.util.concurrent.FutureTask.run(FutureTask.java:266)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at java.lang.Thread.run(Thread.java:748)
Detection failed after 0 frames:
Problem during detection: java.lang.OutOfMemoryError: Java heap spaceFound 0 spots prior failure.
```
It's likely caused by the source stacks not having the Z and T channels switched. When the images are exported from the C10/Gen5, the images are stacked into Z layers instead of T (timepoints). These need to be switched manually through another ImageJ script prior to running this automated kalman tracker.

2. Python script isn't working - this is likely due to an issue with the way the python command is invoked on your system. Check this by typing `python --version` in your terminal. If this doesn't work, test it with other invokations like `python3`, `py`, or `py3`. Then replace the string in KalmanTrackerRunner.java from `python` to whichever invokation gave you a response in the line `ProcessBuilder pb = new ProcessBuilder("python", "src\\Track-Spot_Merger_Auto.py", "--csvlist", csvArgumentString);`.
