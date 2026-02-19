# Agent CLI

`Agent CLI` is an IntelliJ plugin that runs your local agent command inside a dedicated IDE tab.

It is useful when you want to keep agent runs inside your project workspace, with a terminal experience directly in IntelliJ.

## What this plugin does

- Lets you define one or more agent configurations in IDE settings.
- Lets you select the active configuration from the main toolbar (`Select Agent`).
- Runs the selected command from `Run Agent`.
- Opens the session in a dedicated `Agent` tab backed by IntelliJ Terminal.

Each configuration supports:

- `Name` - label shown in the toolbar/tab
- `Binary Path` - command path (required)
- `Arguments` - optional CLI args
- `Working Directory` - optional directory (defaults to current project directory)

## Installation

1. Open this repository's **Releases** page.
2. Download the plugin asset (`.zip` or `.jar`) from the latest release.
3. In IntelliJ, open `Settings -> Plugins`.
4. Click the gear icon and choose `Install Plugin from Disk...`.
5. Select the downloaded file and restart IntelliJ.

## How to use

1. Open `Settings -> Tools -> Agent CLI`.
2. Add a configuration with at least a valid `Name` and `Binary Path`.
3. Mark one configuration as `Default` (if you have multiple).
4. In the toolbar, choose the configuration from `Select Agent`.
5. Click `Run Agent`.
6. Use the opened `Agent` tab to interact with the running command.

## Screenshots

Settings page:

![Agent CLI Settings](docs/Screenshot%20Settings.png)

Running agent tab:

![Agent CLI Running](docs/Screenshot%20Agent.png)

## Requirements and notes

- Compatible with IntelliJ builds from `253` and newer.
- The configured binary must exist and be executable.
- If `Working Directory` is empty, the plugin uses the current project root.

## Developer documentation

Developer and contributor docs are in `docs/development.md`.

## License

This project is licensed under the Apache License 2.0. See `LICENSE`.