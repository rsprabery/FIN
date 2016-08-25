# Function Interception (FIN)

FIN allows you to easily MITM functions on Android. Why? When reversing apps you'll often find custom protocols. With the growing popularity of bluetooth devices and the usage of the [audio port for communication](http://web.eecs.umich.edu/~prabal/projects/hijack/), you may spend many hours reversing a protocol. To intercept an audio connection, you must implement your own soft-modem and then reverse the encoding scheme, baud rate, and other seemingly esoteric variables. It's much easier to simply hook the methods "Encode" and "Decode" (if the applications has them) and replace the inputs or outputs if you need to fuzz the protocol.

That's great, but then you have to write redundant code every time you reverse a new app. That's where FIN comes in. This project allows you to specify the functions to hook, which parts to replace (either the arguments or the return value), and provides a server for managing and modifying the inputs. For a more detailed introduction, see my [blog post](https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2015/october/mitming-custom-protocols-on-mobile-devices/) I wrote while interning at [NCC Group](https://www.nccgroup.trust/us/).

## <a id="toc">TOC</a>
1. [Android](#android)
	- [Android Dependencies](#androiddeps)
	- [Config File Format](#config)
	- [Setting up your Phone](#phone_setup)
3. [MITM Server](#server_side)
	- [Default Handlers](#default_handlers)
	- [Custom Handlers](#custom_handlers)
		- [Python API](#python_handlers)
		- [Any Language (STDIN/STDOUT)](#stdin)
	- [Protocol Details](#protocol)
4. [Default Failure Handling](#failures)
4. [Extending Android Code to work with Arbitrary Objects](#arb_types)
5. [Resources](#resources)
6. [Contributing](#contributing)
7. [License](#license)
7. [Future Work / TODO](#todo)

## <a id="android">Android</a>

You'll need a few things to get going. I take care of most of the setup with the `setup.sh` script that I've provided in the Android directory, but you need to do a few things to get that working first. Below I go over everything you need to do before you execute that script.

Before you begin, you should have an Android device that has been rooted and has an app installed to manage super user privileges.

### <a id="androiddeps">Android Dependencies</a>

To be able to build the project and put it on your mobile device, you'll need the [Android SDK](https://developer.android.com/sdk/installing/index.html). You can use either the standalone SDK, or Android Studio, whichever you prefer.

Be sure and jot down the location of the SDK, as you'll be prompted for it when running the `setup.sh` script later. For example, on my OSX box, it's located at `/Users/read/Library/Android/sdk`.

The important part is that you put the Android Debugger, `adb`, in your `$PATH`. So in your `~/.bashrc` or `~/.zshrc` file, put something like the following.

```bash
# Based on the default SDK installation on OSX
PATH=$PATH:/Users/$YOUR_USERNAME/Library/Android/sdk
```
**NOTE**, the exact location of your `adb` command is going to be dependent on where you've installed your sdk.

### <a id="config"> Config File Format</a>

During setup, you'll be prompted to fill out a config file. Lets go over the layout of that file so you'll have the information handy before running `setup.sh`.

The general layout for the config file is:

```yaml
---
serverIp: $YOUR_IP
packageName: com.example
hooks:
  # Hooking a method that modifies arguments.
  - classToHook: com.example.package.MessageHandler
    functionToHook: encode
    parameterTypes:
      - String
  # Hooking a method to modify the return value.
  - classToHook: com.example.package.MessageHandler
    functionToHook: decode
    parameterTypes:
    	- String
    returnValueOnly: True
    returnType: String
```

So let's break that down:

- **General Options**
    - `serverIp` - the IP address of the machine hosting the MITM handlers.
    - `packageName` - the root package name of the application you want to hook. This is what is displayed in the URL under the `id` field when browsing the Play store. See [my post]() for an example.
    - `hooks` - an array of hook definitions.
- ** Hook Definitions **
	- General Options for both types of function hooks
		- `classToHook` - the full name, including the package, of the class containing the method you want to hook.
		- `functionToHook` - the method you are hooking.
		- `parameterTypes` - a YAML list of parameter types.
	- Modifying **function args** (often useful for an *Encode* function)
		- If you just want to hook method parameters, you don't need to do anything special, as this is the default behavior.
	- Modifying **function return values** (often useful for a *Decode* function)
		- If you want to modify only the return vales, you'll need to add the following fields:
		- `returnValueOnly: True` - define this hook as one that only modifies the return value.
		- `returnType` - configure the return type to help in (de)/serialization.
	- Modifying **both** function args and return value of a single function
		 - You'll need to provide two hook definitions, one with the options listed above for return value only and one without.

You'll use a different mixture of hook definitions depending on the implementation of the protocol you are inspecting and the specific application you are working with. Feel free to define as many as necessary. Have these ready before running your setup script. See the [resources](#resources) section for a few links on reversing tools for Android.

### <a id="phone_setup"> Setup Your Device</a>

Once you've setup the Android SDK and **identified which functions you want to intercept**, you're ready to setup your phone.

The included setup script will:
- Install the [Xposed Installer](http://repo.xposed.info/module/de.robv.android.xposed.installer) application.
- Prompt you on installing the Xposed Framework inside of the above application (required for function hooks to work).
- Ask you to update the config file by opening it in `nano`.
- Prompt you for the location of the Android SDK.
- Build the function hooking application and load it on your phone.
- Setup the config file to be readable by other applications (so your hook is configured even though it is running with the permissions of the hooked application).
- Prompt you to activate the new Xposed Module in the Xposed Installer application.
- Reboot your phone.

Make sure to **read and follow** the prompts.

OK got it? Great! When you're ready run:

```bash
git clone https://github.com/rsprabery/$PROJECT
cd $PROJECT/mobile/android
./setup.sh
```

Once you're finished, the script will reboot your phone. Before opening the app you're interested in, be sure to setup the server side process:

## <a id="server_side"> Server Side</a>

The process that handles modification of function arguments and return types runs on a separate machine and the hook passes data to it using a TCP socket.

Below, I'll go over the default data handlers that are available and provide information on implementing your own.

### <a id="default_handlers">Default Data Handlers</a>

You'll need to run a handler before opening the application being hooked (though it will block until it is able to connect if you do happen to open it before your MITM server is running). The default handlers are in the [server](host/server) directory.

There are two default handlers:
- `mitm.py` - in line modification of arguments and return values.
	- prints data to be modified
	- you are prompted to enter modified the data
	- pressing ENTER and leaving the prompt blank will simply return the same data that was sent
	- pressing CTRL-C at any time changes the mode.
		- *prompt mode* - prints data and prompts the user for modified data
		- *log mode* - simply prints data to the screen and never asks the user for data.
    - pressing CTRL-C twice quickly (within .7 of a second) will end the program.
- `passThrough.py` - takes an executable as an argument and passes all data to it for modification, see section below for implementing your own handler.

Both scripts can be executed with `--help` for more information on their arguments.

### <a id="custom_handlers"> Implementing Your Own Handler</a>

The easiest way to implement your own handler is to use the Python API. You can also use any language that can read and write to STDIN/STDOUT, but will have to decode the protocol in order to do this.

#### <a id="python_handlers"> Using Python</a>

Looking at the code for `mitm.py` and `passThrough.py` will help you get started quickly. The following code snippet is a small data handler that simply prints out the data and returns the original data unmodified.

```python
#!/usr/bin/env python

from fin.handlers import InputHandler, ServerHandler
from fin.mitm import MitmServer

class CustomHandler(InputHandler):

    def __init__(self):
        pass

    def handle_input(self, from_function, input_args):
        print "in function: " + from_function
        return input_args

ServerHandler(MitmServer(8080), CustomHandler()).run()
```

The highlights of implementing your own handler in Python boil down to:
- Provide an `InputHandler` implementation:
	- i.e.: Make a class that has a `handle_input(self, from_function, input_args)` method.
- Instantiate a `ServerHandler`:
	- The first argument is a `MitmServer` that is initiated with a port number.
	- The second argument is an instantiation of your new handler class.
- Call `.run()` on the `ServerHandler`.


#### <a id="stdin">Using any Language (STDIN/STDOUT)</a>

To interface with the MITM server using any language, you'll utilize a protocol very similar to the one defined in the section below (this is the same protocol that is used to pass data too and from the mobile device). I highly recommend you look at the source for `scripting_example.py` - while this was written in Python, you can treat it as a stand alone binary calling it with `./passThrough.py ./scripting_example.py`.

Your program will get called once every time a hooked function gets called on the mobile device. The data passed in via `stdin` consists of both the name of the function that is being hooked and the data to be modified. The `passThrough.py` program then waits for your program to write back modified data to `stdout`. If you need to keep state between calls, you'll need to use a data storage mechanism such as sqlite.

In summary your program will need to:

- Read a line from `stdin`.
	- Be sure and remove the trailing "\n"!
- Split the line on commas (",").
- Base64 decode each part of the split line.
- The first part of the decoded line is the name of the function being hooked.
- All subsequent parts are arguments or a return value from the function.
- Modify any of the arguments or return values as necessary.
- Base64 encode each of the modified arguments/data.
- Join the Base64 encoded strings with ",".
- Write out the joined string to `stdout` including a trailing "\n" (which is done for you in most languages).

Each of these steps is essentially a line of code in the example `./scripting_example.py` which is shown below:

```python
all_args = raw_input()  # reading in a line from stdin
split_args = all_args.split(',')  # splitting the line on commas
decoded_args = [x.decode('base64') for x in split_args]  # decoding each argument in the list
function_name = decoded_args[0]  # grabbing the first element in the list
function_args = decoded_args[1:]  # grabbing everything from the second element to the end of the list

# encoding each element in the function arguments and joining them with commas
print ','.join(x.encode('base64') for x in function_args)
```

### <a id="protocol">Protocol Details</a>

You *probably* don't need to read this. If you are heavily modifying this project, or trying to implement a similar interface on iOS this information may be useful.

All communication is done over a single socket and both sides block until the inputs/outputs for a single function are handled before moving on to the next function that has been hooked.

The protocol works by first building a message that consists of the name of the function followed by the arguments or return type for that function. Each part of the message is base64 encoded and then separated by commas.

If the literal value `null` is sent, it is translated into the string literal `"null"` and not surrounded by quotes before it is added to the message. Doing this allows the server to decipher between a null argument and the string `"null"` which would be sent as `" \"null\" "`.

This leaves us with a message that looks like:

`Base64(MethodName),Base64(ARG1),Base64(ARG2)...`

The length of the message is sent in a four byte buffer holding a big endian integer. After receiving and converting those four bytes containing the message length into an integer, the message can be received and decoded.

When communicating with other processes on the server side, `passThrough.py` uses newlines instead of sending the byte count of the message which reduces the complexity of implementing custom handlers in other languages.

## <a id="failures"> Handling Failures</a>

All failures are handled gracefully. Here is a list of failure cases and how each is handled. Reading over this will help you in debugging any hooks you have and allow you to have a better understanding when things go wrong.

- If the function hook cannot connect to the server, it blocks until the server is up and can respond to the hook.
- If one hook is improperly configured, information is logged in Xposed Installer->Logs and the other hooks are still instantiated.
- If the connection to the server is broken, (maybe the server crashed, or you simply wanted to change handlers), then the hook tries to reconnect and re-send the data until a response is received.
- If the mobile device crashes or the connection is broken on the mobile device side the server code will close the client socket and wait for a new connection.

## <a id="arb_types"> Handling Arbitrary Types on Android </a>

Right now, the code on Android can only use built in types. If you need to implement your own types/objects, you'll need to do a few things.

- Add your type to the encoding and decoding functions
	- The encoding method is `ProtocolWorker.convertArgtoString` in the [ProtocolWorker file.](mobile/android/AndroidFunctionMitm/app/src/main/java/com/example/androidfunctionmitm/ProtocolWorker.java)
	- The decoding method is `ProtrocolWorker.convertArgToObject` in the [ProtocolWorker file.](mobile/android/AndroidFunctionMitm/app/src/main/java/com/example/androidfunctionmitm/ProtocolWorker.java)
	- If the object you are using doesn't provide serialization, you have a few options:
        - Use reflection to translate the object and all references to a JSON document.
        - Use getters in the encode method and setters in the decode method.
        - Patch Xstream to do the reflection for you on Android.
- Until a full solution is implemented (likely using a modified implementation of Xstream), you also need to modify the method `isBuiltInType` which will enable the encoding and decoding methods to be called appropriately when necessary.
	- This is because of line 39 in [ProtocolWorker](mobile/android/AndroidFunctionMitm/app/src/main/java/com/example/androidfunctionmitm/ProtocolWorker.java). 
	- When modifying the `isBuiltInType` method, you'll need to be sure to rewrite `convertArgToString` as well, as instructed above.

## <a id="resources">Resources</a>
- [FIN Tutorial](https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2015/october/mitming-custom-protocols-on-mobile-devices/) on NCC Group's website.
- [Getting Started with Function Hooking](https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2015/september/code-injection-on-android/) - My blog post written at NCC Group.
- [Xposed Framework](http://repo.xposed.info/module/de.robv.android.xposed.installer) - the framework that makes this possible.
- [APK Downloader](https://apps.evozi.com/apk-downloader/) - download APK's with just a link from Google Play.
- [dex2jar](https://github.com/pxb1988/dex2jar) - tools for reversing .dex files.
- [Smali Reference](https://code.google.com/p/smali/w/list) - useful when reversing in Smali which is just a step above Dalvik bytecode.
- [Davlik Bytecode Reference](http://pallergabor.uw.hu/androidblog/dalvik_opcodes.html) - may help in understanding certain aspects of reflection and Smali code.

## <a id="contributing"> Contributing </a>

The contributing process is pretty smooth, just send a pull request!

- Fork the project on GitHub.
- Make your feature addition or bug fix.
- Commit with Git.
- Send me a pull request.
- If you've added a major feature, please include relevant changes in the README.

If you're interested in working on something and don't know where to start, see the future work section below. The references section above provides links if you're new to Android reversing.

## <a id="license"> License </a>

This project uses the Apache 2.0 License. Please see the [full license](./LICENSE)

## <a id="todo"> Future Work </a>

- Handle arbitrary types automatically.
	- This can likely be done using [Xstream](http://x-stream.github.io/), but it will need to be modified to work on Android.
- Handle array types - somewhat of a subset of the above.
	- Reflection is used to get a Class object from the `parameterTypes` in the config file. There needs to be code added to handle array definitions. Something like `String[]` would be mapped to `Lcom.lang.String;]` and `boolean[]` would translate to `B]` (see the Dalvic byte code link in the references section).
- iOS version that uses the same config file format.



