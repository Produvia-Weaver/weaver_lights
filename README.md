# Weaver Lights and Android SDK
![WeaverLights](http://www.weavingthings.com/technical_docs/smartlights_walkthrough/images/produvia_man_home.png)
##### Weaver Lights is a smart-lights controller reference application
##### powered by the [Weaver SDK]

## 
## [Weaver SDK]
##### [Weaver SDK] is a cloud service and SDK that, lets developers connect and control smart devices easily saving the need to implement and maintain many different APIs and SDKs
###
### Description
##### Weaver Lights uses the Weaver Android SDK in order to:
  - Manage users (login and registration)
  - Scan and remember smart lighting services inside the network
  - Display and support many smart light services with one simple [JSON API]
   
##### Weaver Lights currenetly supports: Philips Hue, Lifx, and Flux smart bulbs. 
##### More devices are coming soon.
##

##### [Checkout Weaver Lights at Google Play] 
![Weaver Lights Image1](https://lh3.googleusercontent.com/1DOkB1oYLbqPWClV2OjoA6jZ8B9V7S4g1ibXE9MH5LmESOruCtJzwEHeWsuZg5NHkVI7=h310-rw) ![Weaver Lights Image2](https://lh3.googleusercontent.com/KYhRgqH_wiw7n5E7KS_vkXS-5PzrzbkJnMMj7Aqe6yjEusmzgDtq8uPf2n1WMRmK5Jql=h310-rw) ![Weaver Lights Image3](https://lh3.googleusercontent.com/nJanFq_JtE0qI_XiT8y9AAbzZ6Xjh8Mz-Bh1-ILc2q6bauokf_dJ5mdbY7SyZdikZuwa=h310-rw) 

### How To Use Weaver SDK
- Simply add the android SDK library to your build.gradle
- [Join our beta program] to receive your Weaver-SDK API KEY
- Checkout the [Full Step By Step Guide] for building a lighting App using the Weaver SDK
- Also checkout the [Android Documentation]


### Installation

Add WeaverSDK android library to your build.gradle dependencies:

```sh
  compile 'produvia.com.weaverandroidsdk:weaverandroidsdk:0.0.26'
```
> The goal of the WeaverSDK is to let developers concentrate on developing their UI without the need to bother with maintaining tons of APIs.
> Weaver uses a simple JSON api in order to scan for and control smart devices.
> Whenever new device support is added, apps will usually automatically support the new devices without the need to update the App!

Weaver Lights uses [LarsWerkman/HoloColorPicker] opensource library



### Documentation
- [Android Documentation]
- [Full Step By Step Guide]


License
----

The Weaver Lights app is distributed under the MIT License



   [Full Step By Step Guide]: <http://www.weavingthings.com/technical_docs/smartlights_walkthrough/>
   [JSON API]: <http://weavingthings.com/weaver-sdk-reference/>
   [Join our beta program]: <http://weavingthings.com/#contact>
   [Android Documentation]: <http://weavingthings.com/weaver-sdk-reference/>
   [Weaver SDK]: <http://weavingthings.com>
   [Checkout Weaver Lights at Google Play]: <https://play.google.com/store/apps/details?id=produvia.com.lights&hl=en>
   [LarsWerkman/HoloColorPicker]: <https://github.com/LarsWerkman/HoloColorPicker>
   
