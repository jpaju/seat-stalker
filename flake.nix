{
  description = "General flake for Scala development";

  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
      in
      {
        devShells.default = pkgs.mkShell {
          name = "scala";
          packages = with pkgs; [
            sbt
            scala-cli
            temurin-bin-21
            azure-functions-core-tools
          ];

          shellHook = ''
            export SBT_TPOLECAT_DEV=1
          '';
        };

        devShells.infrastructure = pkgs.mkShell {
          name = "infrastructure";
          packages = with pkgs; [
            azure-cli
            terraform
            tfsec
          ];
        };
      }
    );
}
