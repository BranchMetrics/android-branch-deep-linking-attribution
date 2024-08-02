{
  description = "Branch Android SDK for deep linking and attribution";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
    android.url = "github:tadfisher/android-nixpkgs";
  };

  outputs = { self, nixpkgs, flake-utils, android }: flake-utils.lib.eachSystem [ "aarch64-darwin" "x86_64-darwin" "x86_64-linux" ] (system:
    let
      inherit (nixpkgs) lib;
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
        overlays = [
          (final: prev: {
            android-sdk = android.sdk.${system} (sdkPkgs: with sdkPkgs; [
              build-tools-34-0-0
              cmdline-tools-latest
              emulator
              platform-tools
              platforms-android-34
            ]
            ++ lib.optionals (system == "aarch64-darwin") [
              system-images-android-34-google-apis-arm64-v8a
            ]
            ++ lib.optionals (system == "x86_64-darwin" || system == "x86_64-linux") [
              system-images-android-34-google-apis-x86-64
            ]);
          })
        ];
      };
    in
    {
      formatter = pkgs.nixpkgs-fmt;
      devShells = {
        default = pkgs.mkShell {
          name = "branch-sdk-android";
          ANDROID_HOME = "${pkgs.android-sdk}/share/android-sdk";
          ANDROID_SDK_ROOT = "${pkgs.android-sdk}/share/android-sdk";
          JAVA_HOME = "${pkgs.jdk17.home}";
          shellHook = ''
            echo "
            Entered the Branch SDK Android development environment.
            ";
          '';
          packages = [
            pkgs.android-sdk
            pkgs.jdk17 # for android build-tools
          ];
        };
      };
    }
  );
}
