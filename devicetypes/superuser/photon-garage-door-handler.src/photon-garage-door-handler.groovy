/**
 *  Photon Garage Door Handler
 *
 *  Copyright 2019
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

// preferences allow the user to configure certain properties of their device
preferences {
    section("Your Particle credentials and device Id:") {
        input("token", "text", title: "Access Token", required: true, displayDuringSetup: true)
        input("deviceId", "text", title: "Device ID", required: true, displayDuringSetup: true)
    }
}

metadata {
	definition (name: "Photon Garage Door Handler", namespace: "", author: "tareker", mnmn: "SmartThings", vid: "generic-switch") {
		capability "Garage Door Control"     
        capability "Actuator"
        capability "Switch"
		capability "Sensor"        
        capability "Contact Sensor"
        capability "Polling"
        capability "Refresh"
        capability "Health Check"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
    // tile definitions. Scale: 2 means a 6 wide grid
	tiles (scale: 2) {
    	multiAttributeTile(name:"main", type:"generic", width:6, height:4, canChangeIcon: true) {
        	// our 'door' attribute of the Garage Door Capability
			tileAttribute("device.door", key: "PRIMARY_CONTROL") {
            	// define the States for the door
				attributeState "unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e"      
                
                // when the door attribute is "closed", the tile will display icon garage-closed with a backgroundColor of 79b821. the Tile action will be control.open
				//attributeState "closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"opening"
				attributeState "closed", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-closed", backgroundColor:"#ff0000"
                // when the door attribute is "open", the tile will display icon garage-open with a backgroundColor of ffa81e. the Tile action will be control.close
				//attributeState "open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", nextState:"closing"
				attributeState "open", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#00ff00"
                                
				attributeState "opening", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-opening", backgroundColor:"#D3D3D3"
				attributeState "closing", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-closing", backgroundColor:"#D3D3D3"
            }
		}
		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
			state "default", label:'open', action:"door control.open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
			state "default", label:'close', action:"door control.close", icon:"st.doors.garage.garage-closing"
		}
            
        // which tile is our main tile
		main "main"
        // other tiles that should appear
		details(["main","open","close"])
	}
}

// The parse method is the core method in a typical device handler
// Sample data: [result:1, cmd:VarReturn, coreInfo:[last_handshake_at:2018-05-16T19:10:23.294Z, last_app:, product_id:6, deviceID:360047000c47343432313031, 
//               connected:true, last_heard:2018-05-16T19:11:32.419Z], name:doorstate]
def parseDoorState(data) {
    def state = null
      
    log.debug "parseDoorState data.Result is ${data.result}" 
        
    switch (data.result) {
        case 0: 
        	state = "unknown"
        	break
        case 1: 
        	state = "open"
        	break
        case 2: 
        	state = "closed"
        	break
        case 3: 
        	state = "opening"
        	break
        case 4: 
        	state = "closing"
        	break 
        case 5: 
        	state = "manual"
        	break 
        default: 
        	state = "unknown" 
        	break
    }
    
    // Create and fire an Event. In cases where you need to fire the event (outside of the parse() method), sendEvent() is used instead of CreateEvent
	//sendEvent(name: 'door', value: state)
    
    log.debug "parseDoorState returns state '${state}'" 
        
    return state
}

def on() {
    open()
}

def off() {
	close()
}
    
// handle commands
def open() {
	log.debug "Executing 'open'"
    sendEvent(name: "door", value: "opening")
    setDoorState('OPEN')
}

def close() {
	log.debug "Executing 'close'"
	sendEvent(name: "door", value: "closing")
    setDoorState('CLOSE')
}

// Particle API call to our photon device
// "deviceId" will be replaced with our actual device name
// cmd is our input parameter: 'open' or 'close'
private setDoorState(cmd) {
	log.debug "Executing 'setDoorState'"
	httpPost(
		uri: "https://api.particle.io/v1/devices/${deviceId}/SetDoorState",
        body: [access_token: token, command: cmd],  
	) {
    	response -> 
        	log.debug "setDoorState httpPost returns status: ${response.status}, data: ${response.data}"
        	runIn(20, refresh)
    }
}

// This method is expected to be defined by Device Handlers.
// Called when messages from a device are received from the hub. 
// The parse method is responsible for interpreting those messages and returning Event definitions.
def parse(data) {
	log.debug "Executing 'parse'"
    def state = parseDoorState(data)
    // Create a Map that represents an Event object.  The resulting map is then returned from the parse() method.
    // The SmartThings platform will then create an Event object and propagate it through the system.
    // Only events that constitute a state change are propagated through the SmartThings platform. 
    // A state change is when a particular attribute of the device changes. 
    // This is handled automatically by the platform, but should you want to override that behavior, you can do so by specifying the isStateChange parameter.
    // The createEvent just creates a data structure (a Map) with information about the Event. It does not actually fire an Event.
	// Only by returning that created map from your parse method will an Event be fired by the SmartThings platform.
    def result = createEvent(name: 'door', value: state)
    
    // createEvent does not appear to work/be called. Calling sendEvent explicitly does.
    // If you need to generate an Event outside of the parse() method, you can use the sendEvent() method. It simply calls createEvent() and fires the Event.
    //sendEvent(name: "door", value: state)    
    // calling sendEvent twice in a row results in the door status "unknown" to be shown.
    // adding the 2 optional parameters seemed to solve that issue...
    sendEvent(name: "door", value: state, isStateChange:true, displayed:true)
    
    // so the Samsung connect app will show the correct status
    if (state == "open") {
        sendEvent(name: "contact", value: "open")
        sendEvent(name: "switch", value: "on")
	} else if (state == "closed") {
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "switch", value: "off")
	}
    
    //log.debug "Result: ${result}"
    //result.isStateChange = true
    //result.displayed = true  
    log.debug "Result: ${result}"
    
    log.debug "Parse returned ${result?.descriptionText}"
    return result
}
// Particle API call to our photon device
// "deviceId" will be replaced with our actual device name
// returns current door state: 'open', 'close', opening, closing etc
private getDoorState()
{
	httpGet(
		uri: "https://api.particle.io/v1/devices/${deviceId}/doorstate",
        requestContentType: "application/json",
        headers: [Authorization: " Bearer " + token],   
	) { response -> 
        	log.debug "getDoorState httpGet returns status: ${response.status}, data: ${response.data}" 
            if (response.status == 200){
    			//log.debug "Success! Parsing '${response.data}'"
        	    parse(response.data)
            }
    }
}

def refresh() {
	log.debug "Executing 'refresh'"
    getDoorState() 
}

// If you add the Polling capability to your device type, this command
// will be called approximately every 5 minutes to check the device's state
def poll() {
    refresh()
}