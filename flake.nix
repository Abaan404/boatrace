{
  description = "BoatRacing Minigame";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };

      lib = pkgs.lib;
    in
    {
      devShells.${system}.default = pkgs.mkShell rec {
        packages = [
          pkgs.jetbrains.jdk-no-jcef
          pkgs.gradle
          pkgs.flite
          pkgs.libGL
          pkgs.glfw3-minecraft
          pkgs.libpulseaudio
        ];

        shellHook = ''
          export LD_LIBRARY_PATH="${lib.makeLibraryPath packages}";
          export JAVA_HOME = "${pkgs.jetbrains.jdk-no-jcef.home}";
        '';
      };
    };
}
