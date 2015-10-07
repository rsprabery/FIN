#!/bin/bash

clear
echo "Stop! Hold it! Wait a second...."
echo "This script is assuming that you've done the following:"
echo "1. Rooted your Android Device."
echo "2. Installed an app to manage super user permissions."
echo "3. Put your device in debug mode."
echo "4. Plugged your device into this computer."
echo ""
echo "If you're all good, press ENTER - otherwise hit CTRL-C now!"
read
clear

echo "Grabbing Xposed Installer"
mkdir deps_temp
cd deps_temp
wget -O xposed_installer.apk http://dl-xda.xposed.info/modules/de.robv.android.xposed.installer_v33_36570c.apk
adb install xposed_installer.apk
cd ..
rm -rf deps_temp
sleep 1
clear

echo ""
echo "Open 'Xposed Installer' on your phone and select 'Framework'."
echo "Grant it super user permissions (should be prompted)."
echo "Choose the install option, but do not reboot (select CANCEL when prompted)."
adb shell am start de.robv.android.xposed.installer/.WelcomeActivity
read -p "Press enter to continue."
echo ""
adb shell am force-stop de.robv.android.xposed.installer

clear
cd AndroidFunctionMitm
echo "Please edit the config file for your application."
echo "Press enter to open the config file in nano. Press CTRL-X to save and exit"
read
nano app/src/main/assets/config.yaml
echo "Bulding the MitM hook apk"
echo "Enter the directory for your Android SDK:"
read ANDROID_HOME 
echo "sdk.dir=${ANDROID_HOME}" > local.properties
./gradlew build
adb install app/build/outputs/apk/app-debug.apk
adb shell am start com.example.androidfunctionmitm/.MitmConfigLoaderActivity
sleep 1 
echo ""

clear
echo "Enable the hook in XposedInstaller->Modules section"
echo "by simply checking it off."
adb shell am start de.robv.android.xposed.installer/.WelcomeActivity
echo ""
echo "ENTER to continue."
read

clear
echo "Make sure you have installed the application you want to hook with:"
echo "adb install PACKAGE_NAME.apk"
echo "Press enter to continue. (Will reboot device)"
read

adb shell reboot
