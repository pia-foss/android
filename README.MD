[![PIA logo][pia-image]][pia-url]

# Private Internet Access
Private Internet Access is the world's leading consumer VPN service. At Private Internet Access we believe in unfettered access for all, and as a firm supporter of the open source ecosystem we have made the decision to open source our VPN clients. For more information about the PIA service, please visit our website [privateinternetaccess.com](https://privateinternetaccess.com).

# Android Application
With this application, consumers can easily enable and communicate with the Private Internet Access services. It provides interfaces for authenticating, purchasing plans, updating servers, getting connectivity updates, handling VPN profiles etc.

## Installation

### Requirements
 - Git (latest)
 - Android Studio (Stable channel)
 - Gradle (latest)
 - ADB installed
 - NDK (latest)
 - Android 4.1+

#### Download Codebase
Using the terminal:

`git clone https://github.com/pia-foss/android.git *folder-name*`

type in what folder you want to put in without the **

or use a graphical interface like Android Studio's to clone the repo.

In the Terminal or Command Prompt, navigate to the cloned folder and once there type in:

`git submodule init`

This will initialize the submodules of our project. Then:

`git submodule update --recursive`

This will download and install the required submodules in our project.

Next is to download the Open VPN submodules. Next:

`cd openvpnTunnel/`

This will moves you into our Open VPN code folder. Next:

`git submodule init`

This will initialize the submodules of this project. Then:

`git submodule update --recursive`

This will download and install the remaining submodules in this project.

#### Building

Once the project is cloned and submodules are updated, the project will build once opened in Android Studio. This will require building the binaries for the underlying modules and configurations. This can take a while for certain computers and is only done on complete clean and rebuilds. You can see progress in the gradle console. Once completed, the app will be able to be run on a device connected to the computer or an emulator running on your computer.

*Please note that Android 4.x emulators are broken with VPNs and will not connect.*

## Documentation

#### Architecture

`com.privateinternetaccess.android` is the package name for the PIA section of the app.

`de.blinki.openvpn` contains all of PIA VPN's OpenVPN implementation.

All similar UI elements and model classes are keep together. All activities and fragments are keep together by the area of the app they are in.

Code structure via packages:

* `ui` - application UI classes and elements
* `model` - application model elements including handlers, interfaces, events and POJOs.
* `pia` - API, handlers, interfaces, model and tasks relating to Private Internet Access account, server and connection information
* `tunnel` - Bridging elements with OpenVPN.
* `receivers` - Broadcast receivers used in PIA VPN.

#### Significant Classes and files

* PIABuilder - Initializes the required notification channels, vpn logs cache, services and more. Should be initialized on Application.onCreate()
* PIAFactory - Creates instances of interfaces that let you control the VPN, get account information and handle connection elements.
* PiaOvpnConfig - Generates the vpn configuration to send to OpenVPN.
* ConnectionResponder - Handles IP grabbing, port forwarding and MACE for all connections.

### Authors

* Harrison Frede - half47/slai47
* Arne Schwabe - Arne
* Tommie Podzemski - tommie
* Justin Simpson - justin

## Contributing

By contributing to this project you are agreeing to the terms stated in the [Contributor License Agreement](CLA.md). For more details please see our [Contribution Guidelines](https://pia-foss.github.io/contribute) or [CONTRIBUTING](/CONTRIBUTING.md).

## Licensing

The source code is licensed under the GPLv3. See [LICENSE](/LICENSE) for more information.

### Acknowledgements

This product includes software developed by the OpenSSL Project for use in the OpenSSL Toolkit. (https://www.openssl.org/)

© 2002-2017 OpenVPN Inc. - OpenVPN is a registered trademark of OpenVPN Inc.


<!-- Markdown link & img dfn's -->
[pia-image]: https://www.privateinternetaccess.com/assets/PIALogo2x-0d1e1094ac909ea4c93df06e2da3db4ee8a73d8b2770f0f7d768a8603c62a82f.png
[pia-url]: https://www.privateinternetaccess.com/
[wiki]: https://en.wikipedia.org/wiki/Private_Internet_Access
