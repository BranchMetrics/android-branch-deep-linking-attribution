#!/usr/bin/env python2

import os
import subprocess

# Custom modules
from helpers import formatted_text


def execute_command(cmd, stdout=None, stderr=None):
    # To wait on the standard output or the error output,
    # set either 'stdout' or 'stderr' to a value of 'subprocess.PIPE'
    print "Executing command: {0}".format(cmd)
    p = subprocess.Popen(cmd, stdout=stdout, stderr=stderr, shell=True)
    stdout, stderr = p.communicate()
    if stdout:
        print stdout
    if p.returncode != 0:
        if stderr:
            print formatted_text.in_bold_red("{0}".format(stderr))
        print formatted_text.in_bold_red("Exit code: {0}".format(p.returncode))
        raise subprocess.CalledProcessError(p.returncode, cmd, output=stdout)


# NOTE The build and test script starts here
if __name__ == '__main__':
    try:
        # Change into the '<some_absolute_path>/android-sdk/sdk-android/' directory
        android_root_dir = os.getcwd() # <some_absolute_path>/android-sdk
        print "Current Working Directory: {0}".format(android_root_dir)
        sdk_android_dir = "../android-branch-deep-linking-attribution/" # <some_absolute_path>/android-sdk/sdk-android
        os.chdir(sdk_android_dir)
        print "Changed to the '{0}' directory\n".format(os.getcwd())

        # # Execute the gradle 'generateRelease' task
        # print formatted_text.in_bold("Building the Android SDK...")
        # execute_command("./gradlew build")
        # print formatted_text.in_bold_green("Successfully finished building the Android SDK.\n")

        # Create a new AVD
        print formatted_text.in_bold("Creating a new AVD...")
        # execute_command("echo n | $ANDROID_HOME/tools/bin/avdmanager --verbose create avd -f -n emu -k 'system-images;android-27;google_apis;x86'")
        print formatted_text.in_bold_green("\nSuccessfully created a new AVD.\n")

        # Restart the adb service
        print formatted_text.in_bold("Restarting the adb service...")
        execute_command("$ANDROID_HOME/platform-tools/adb kill-server")
        execute_command("$ANDROID_HOME/platform-tools/adb start-server")
        execute_command("$ANDROID_HOME/platform-tools/adb devices")
        print formatted_text.in_bold_green("Successfully finished restarting the adb service.\n")

        # Start the newly created AVD
        print formatted_text.in_bold("Spinning up the AVD...")
        execute_command("SHELL=/bin/bash $ANDROID_HOME/emulator/emulator -avd A_Pixel_3_XL_API_29 -wipe-data  &")
        execute_command("SHELL=/bin/bash $ANDROID_HOME/emulator/emulator -avd A_Pixel_3_XL_API_29  &")
        print formatted_text.in_bold_green("Finished spinning up the AVD.\n")

        # Wait for the AVD to finish booting up
        print formatted_text.in_bold("Waiting for AVD to finish booting up...")
        execute_command("$ANDROID_HOME/platform-tools/adb wait-for-device")
        print formatted_text.in_bold_green("AVD finished booting up.\n")

        # Unlock the screen of the AVD
        print formatted_text.in_bold("Unlocking the AVD screen...")
        execute_command("$ANDROID_HOME/platform-tools/adb shell input keyevent 82 &")
        print formatted_text.in_bold_green("Successfully unlocked the AVD screen.\n")

        # Start running the instrumentation tests
        print formatted_text.in_bold("Running instrumentation tests...")
        os.chdir("./Branch-SDK/")
        execute_command("gradle connectedCheck --info")
        print formatted_text.in_bold_green("Finished running the instrumentation tests.\n")

    except subprocess.CalledProcessError as e:
        print formatted_text.in_bold_red("Caught a CalledProcessError exception! The last command returned a non-zero exit code value of '{0}'.\n".format(e.returncode))
        raise
    except:
        print formatted_text.in_bold_red("Caught an unexpected exception!\n")
        raise
    finally:
        # Shutdown the running AVD
        print formatted_text.in_bold("Shutting down the AVD...")
        execute_command("$ANDROID_HOME/platform-tools/adb devices | grep emulator | cut -f1 | while read line; do echo \"Killing $line\" && $ANDROID_HOME/platform-tools/adb -s $line emu kill; done")
        print formatted_text.in_bold_green("Successfully shutdown the AVD.\n")
