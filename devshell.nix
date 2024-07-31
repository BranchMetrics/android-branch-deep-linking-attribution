{ pkgs }:

with pkgs;

let
  jdk = pkgs.jdk17;
in
with pkgs;

# Configure your development environment.
#
# Documentation: https://github.com/numtide/devshell
devshell.mkShell {
  name = "branch-sdk-android";
  motd = ''
    Entered the Branch SDK Android development environment.
  '';
  env = [
    {
      name = "ANDROID_HOME";
      value = "${android-sdk}/share/android-sdk";
    }
    {
      name = "ANDROID_SDK_ROOT";
      value = "${android-sdk}/share/android-sdk";
    }
    {
      name = "JAVA_HOME";
      value = jdk.home;
    }
  ];
  packages = [
    android-sdk
    jdk17 # for android build-tools
    bun # faster scripting
    coreutils # for date
    ngrok
    jq
  ];
}