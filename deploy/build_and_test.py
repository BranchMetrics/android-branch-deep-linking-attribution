#!/usr/bin/env python2

import os
import subprocess
import tempfile
import time

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

def restart_adb():
    # Restart the adb service
    print formatted_text.in_bold("Restarting the adb service...")
    execute_command("$ANDROID_HOME/platform-tools/adb kill-server")
    execute_command("$ANDROID_HOME/platform-tools/adb start-server")
    execute_command("$ANDROID_HOME/platform-tools/adb devices")
    print formatted_text.in_bold_green("Successfully finished restarting the adb service.\n")


# Launch all AVDs that are available on the system
def launch_avds():
    fp, filepath = tempfile.mkstemp();
    if not os.path.isfile(filepath):
        print("File path {} does not exist.  Exiting...".format(filepath))
        sys.exit()

    execute_command("SHELL=/bin/bash $ANDROID_HOME/emulator/emulator -list-avds > {}".format(filepath))

    avds = {}
    avds = [line.rstrip('\n') for line in open(filepath)]

    print "Launching {} AVDs".format(len(avds))

    for device in avds:
        try:
            print("Launching AVD Device: {}...".format(device))
            # execute_command("SHELL=/bin/bash $ANDROID_HOME/emulator/emulator -avd {} -wipe-data  &".format(device))
            execute_command("SHELL=/bin/bash $ANDROID_HOME/emulator/emulator -avd {} &".format(device))
            print formatted_text.in_bold_green("Finished spinning up the AVD.\n")

            # Sleeping here as spinning up these too fast causes others not to start
            time.sleep(1)
        except subprocess.CalledProcessError as e:
            print formatted_text.in_bold_red("Caught a CalledProcessError exception! The last command returned a non-zero exit code value of '{0}'.\n".format(e.returncode))
            raise
        except:
            print formatted_text.in_bold_red("Caught an unexpected exception!\n")
            raise

    os.remove(filepath);


# For each AVD, wait for it to start up
def wait_avds():
    print formatted_text.in_bold("Waiting for AVDs...")
    fp, filepath = tempfile.mkstemp();
    execute_command("SHELL=/bin/bash $ANDROID_HOME/platform-tools/adb devices | grep emulator | cut -f1 > {}".format(filepath))

    emulators = {}
    emulators = [line.rstrip('\n') for line in open(filepath)]

    for device in emulators:
        try:
            print("Emulator: {}".format(device))

            # Wait for the AVD to finish booting up
            print formatted_text.in_bold("Waiting for AVD to finish booting up...")
            execute_command("$ANDROID_HOME/platform-tools/adb -s {} wait-for-device".format(device))
            print formatted_text.in_bold_green("AVD finished booting up.\n")

            # Unlock the screen of the AVD
            print formatted_text.in_bold("Unlocking the AVD screen...")
            execute_command("$ANDROID_HOME/platform-tools/adb -s {} shell input keyevent 82 &".format(device))
            print formatted_text.in_bold_green("Successfully unlocked the AVD screen.\n")
        except subprocess.CalledProcessError as e:
            print formatted_text.in_bold_red("Caught a CalledProcessError exception! The last command returned a non-zero exit code value of '{0}'.\n".format(e.returncode))
            raise
        except:
            print formatted_text.in_bold_red("Caught an unexpected exception!\n")
            raise

    os.remove(filepath);

# For each AVD, shut it down
def close_avds():
    print formatted_text.in_bold("Shutting down the AVD...")
    execute_command("$ANDROID_HOME/platform-tools/adb devices | grep emulator | cut -f1 | while read line; do echo \"Killing $line\" && $ANDROID_HOME/platform-tools/adb -s $line emu kill; done")
    print formatted_text.in_bold_green("Successfully shutdown the AVD.\n")


def do_setup():
    # Change into the '<some_absolute_path>/android-sdk/sdk-android/' directory
    android_root_dir = os.getcwd() # <some_absolute_path>/android-sdk
    print "Current Working Directory: {0}".format(android_root_dir)
    sdk_android_dir = "../android-branch-deep-linking-attribution/" # <some_absolute_path>/android-sdk/sdk-android
    os.chdir(sdk_android_dir)
    print "Changed to the '{0}' directory\n".format(os.getcwd())


# NOTE The build and test script starts here
if __name__ == '__main__':
    try:
        # Initialization
        do_setup()

        # Restart the adb service
        restart_adb()

        # Launch all AVDs
        launch_avds()
        wait_avds()

        # Start running the instrumentation tests
        print formatted_text.in_bold("Running instrumentation tests...")
        os.chdir("./Branch-SDK/")
        execute_command("../gradlew connectedCheck --info")
        print formatted_text.in_bold_green("Finished running the instrumentation tests.\n")

    except subprocess.CalledProcessError as e:
        print formatted_text.in_bold_red("Caught a CalledProcessError exception! The last command returned a non-zero exit code value of '{0}'.\n".format(e.returncode))
        raise
    except:
        print formatted_text.in_bold_red("Caught an unexpected exception!\n")
        raise
    finally:
        # Shutdown the running AVD
        close_avds()
