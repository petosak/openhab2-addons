# PLCLogo Binding

This binding provides native support of Siemens LOGO! PLC devices. Communication with Logo is done via Moka7 library.
Currently only two devices are supported: 0BA7 (LOGO! 7) and 0BA8 (LOGO! 8). Additionally multiple devices are supported.
Different families of LOGO! devices should work also, but was not tested now due to lack of hardware.
Binding works nicely at least 100ms polling rate, if network connection is stable.

## Discovery

Siemens LOGO! devices can be manually discovered by sending a request to every IP on the network.
This functionality should be used with caution, because it produces heavy load to the operating hardware.
For this reason, the binding does not do an automatic background discovery, but discovery can be triggered manually.

## Bridge configuration

Every Siemens LOGO! PLC is configured as bridge:

```
Bridge plclogo:device:plc [ address="<ip>", family="<0BA7/0BA8>", localTSAP="0x<number>", remoteTSAP="0x<number>", refresh="<number>" ]
```

| Parameter  | Required   | Default   | Description                                                      |
| ---------- | :--------: | :-------: | ---------------------------------------------------------------- |
| address    | Yes        |           | IP address of the LOGO! PLC.                                     |
| family     | Yes        |           | LOGO! family to communicate with. Can be `0BA7` or `0BA8` now.   |
| localTSAP  | Yes        |           | TSAP (in hex) is used by the local instance. Check configuration |
|            |            |           | in LOGO!Soft Comfort. Common used value is `0x0300`.             |
| remoteTSAP | Yes        |           | TSAP (in hex) of the remote LOGO! PLC, as configured by          |
|            |            |           | LOGO!Soft Comfort. Common used value is `0x0200`.                |
| refresh    | No         | 100ms     | Polling interval, in milliseconds. Is used for query the LOGO!.  |


## Thing configuration

Binding supports two types of things: digital and analog.

### Digital Things
The configuration pattern for digital things is as follow

```
Thing plclogo:digital:<ThingId> [ block="<name>", force="<true/false>" ]
```

| Parameter | Required   | Default   | Description                                                  |
| --------- | :--------: | :-------: | ------------------------------------------------------------ |
| block     | Yes        |           | Block name                                                   |
| force     | No         | false     | Send current value to openHAB, independent if changed or not |

Follow block names are allowed for digital things:

| Type           | `0BA7`              | `0BA8`              | 
| -------------- | :-----------------: | :---------------: |
| Input          | `I[1-24]`           | `I[1-24]`         |
| Output         | `Q[1-16]`           | `Q[1-20]`         |
| Marker         | `M[1-27]`           | `M[1-64]`         |
| Network input  |                     | `NI[1-64]`        |
| Network output |                     | `NQ[1-64]`        |
| Memory         | `VB[0-850].[0-7]`   | `VB[0-850].[0-7]` |

### Analog Things
The configuration pattern for analog things is as follow

```
Thing plclogo:analog:<ThingId> [ block="<name>", threshold="<number>", force="<true/false>" ]
```

| Parameter | Required   | Default   | Description                                                   |
| --------- | :--------: | :-------: | ------------------------------------------------------------- |
| block     | Yes        |           | Block name                                                    |
| threshold | No         | false     | Send current value to openHAB, if changed more than threshold |
| force     | No         | false     | Send current value to openHAB, independent if changed or not  |

Follow block names are allowed for analog things:

| Type           | `0BA7`        | `0BA8`      | 
| -------------- | :-----------: | :---------: |
| Input          | `AI[1-8]`     | `AI[1-8]`   |
| Output         | `AQ[1-2]`     | `AQ[1-8]`   |
| Marker         | `AM[1-16]`    | `AM[1-64]`  |
| Network input  |               | `NAI[1-32]` |
| Network output |               | `NAQ[1-16]` |
| Memory         | `VW[0-849]`   | `VW[0-849]` |

## Channels
Each digital thing have only one channel `state`. Dependend on configured block type,
two different types of items are supported: `Contact` for inputs and `Switch` for outputs.
Means, that for `I` and `NI` blocks `Contact` items must be used. For other blocks simply
use `Switch`, since they are bidirectional. The configuration pattern for digital channels is:

```
channel="plclogo:digital:<ThingId>:state"
```

Each analog thing have only one channel `value`. Only one item type is supported: `Number`
The configuration pattern for analog channels is:

```
channel="plclogo:digital:<ThingId>:value"
```

## Examples

logo.things:

```
Bridge plclogo:device:Logo [ address="192.168.0.1", family="0BA8", localTSAP="0x3000", remoteTSAP="0x2000", refresh ="100" ]
{
  Thing plclogo:digital:VB0_0 [ block="VB0.0" ]
  Thing plclogo:digital:VB0_1 [ block="VB0.1" ]
  Thing plclogo:digital:NI1   [ block="NI1" ]
  Thing plclogo:digital:NI2   [ block="NI2" ]
  Thing plclogo:digital:Q1    [ block="Q1" ]
  Thing plclogo:digital:Q2    [ block="Q2" ]
  Thing plclogo:analog:VW100  [ block="VW100", threshold="1" ]
}
```

logo.items:

```
// NI1 is mapped to VB0.0 address in LOGO!Soft Comfort 
// NI2 is mapped to VB0.1 address in LOGO!Soft Comfort 

Switch  OpenHABUp   {channel="plclogo:digital:VB0_0:state"}
Switch  OpenHABDown {channel="plclogo:digital:VB0_1:state"}
Contact LogoIsUp    {channel="plclogo:digital:NI1:state"}
Contact LogoIsDown  {channel="plclogo:digital:NI2:state"}
Switch  Output1     {channel="plclogo:digital:Q1:state"}
Switch  Output2     {channel="plclogo:digital:Q2:state"}
Number  Position    {channel="plclogo:analog:VW100:value"}
```
