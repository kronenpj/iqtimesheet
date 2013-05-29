if [ -f ../build/IQTimeSheet-instrumented.apk ]; then
  adb install -r ../build/IQTimeSheet-instrumented.apk
else
  adb install -r ../build/IQTimeSheet-debug.apk
fi
adb install -r build/IQTimeSheet-debug.apk
