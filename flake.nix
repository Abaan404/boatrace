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
      devShells.${system}.default = pkgs.mkShell {

        buildInputs = [
          pkgs.jdk
          pkgs.gradle
        ];

        shellHook = ''
          export LD_LIBRARY_PATH="${lib.makeLibraryPath [ pkgs.libGL ]}";
        '';
      };
    };
}
