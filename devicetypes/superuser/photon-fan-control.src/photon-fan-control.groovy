/**
 *  PHOTON FAN CONTROL
 *
 *  Copyright 2016 Tareker
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
 
preferences {
    section("Your Particle credentials and device Id:") {
        input("token", "text", title: "Access Token")
        input("deviceId", "text", title: "Device ID")
    }
}
 
// for the UI
metadata {
	definition (name: "Photon Fan Control", author: "tareker", mnmn: "SmartThings", vid: "generic-switch") {
    	capability "Switch"
	}

    // tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
		state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}

		main "switch"
		details "switch"
	}
}

def parse(String description) {
	log.error "This device does not support incoming events"
	return null
}

def on() {
    sendToDevice '2'
    sendEvent(name: 'switch', value: 'on')
}

def off() {
    sendToDevice '0'
    sendEvent(name: 'switch', value: 'off')
}

private sendToDevice(cmd) {
    // Particle API call to our photon device
    // "deviceId" will be replaced with our actual device name
    // FanControl is the name of our published function exposed by the device
    // state is our input parameter: 'on' or 'off'
	httpPost(
		uri: "https://api.particle.io/v1/devices/${deviceId}/FanControl",
        body: [access_token: token, command: cmd],  
	) {response -> log.debug (response.data)}
}