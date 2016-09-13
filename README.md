# YourHome

What is YourHome?

YourHome is a home automation software package that is built to control everything. YourHome enables you to design your own home automation system. You can do this by designing screens with YourHome designer. Use YourHome to integrate your devices and make them work together, save energy and have control.

## YourHome Server
The core system which runs the home automation software. A windows, linux machine or raspberry pi will do.
* Git: https://github.com/JohanCosemans/YourHomeServer

### Building the code
Execute the following steps to build the code from scratch:

```
gradlew --version
--> Make sure that JKD 1.8 is used
git clone https://github.com/JohanCosemans/YourHomeServer.git
gradlew build
```
This will create two files:
/distributons/yourHome-latest.zip
/distributons/yourHome-<version number>.zip

To get going with this file, read http://yourhomeapp.net/getting-started.html

### Adding your own integrations
If you want to add your own custom integrations, you can do so by extending the AbstractController class.
Look at these classes for examples:
VaillantVSMartController
IPCameraController
ZWaveNetController
HueController
...



## YourHome Designer
The configuration center where you can design screen layouts easily with drag and drop to build your layout as you like. You can also configure the system and build rules and scenes to automate your home.

![App](http://yourhomeapp.net/images/designer/editor.png)

* Git: https://github.com/JohanCosemans/YourHomeServer 
See folder src/main/resources/web/

## YourHome App
A mobile app that connects to the home server and allows to control your home.
* Git: https://github.com/JohanCosemans/YourHomeApp


# More information
* Website: http://www.yourhomeapp.net
* Getting Started: http://yourhomeapp.net/getting-started.html
* E-mail: johan@coteq.be