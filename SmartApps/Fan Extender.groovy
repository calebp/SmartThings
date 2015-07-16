/**
 *  Fan Extender
 *
 *  Copyright 2015 Caleb Packard
 *  Runs a central blower fan for a set amount of time after an HVAC operation to ensure all
 *  cooled/heated air is moved out of the ductwork
 *
 */
 definition(
    name: "Fan Extender",
    namespace: "",
    author: "Caleb",
    description: "Extends an HVAC fan runtime to empty ductwork",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
	section("Pick a thermostat and fan extension duration") {
		input "thermostat", "capability.thermostat", title: "Choose a thermostat..."
        input "intervalInMin", "number", title: "Additional fan runtime in minutes"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize() 
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
    state.priorFanMode = thermostat.currentValue("thermostatFanMode")
}

def operatingStateHandler(evt) {
    log.debug "Operating state is $evt.value"
    if(evt.value == "heating" || evt.value == "cooling")
    {
        state.priorFanMode = thermostat.currentValue("thermostatFanMode")
        log.debug "Hvac cycle for $evt.value entered, setting fan on, fan mode was ${state.priorFanMode}"
        thermostat.fanOn()
    }
    else if(evt.value == "idle") {
        log.debug "Hvac control is idle, setting fan timeout for ${intervalInMin} minutes"
        runIn(intervalInMin*60, fanExtensionExpiryHandler)
    }
    else {
        log.debug "Hvac state was unhandled: $evt.value"
    }
    
}

def fanExtensionExpiryHandler(evt) {
    log.debug "Fan shutoff interval reached, prior fan mode is ${state.priorFanMode}"
    def currentFanMode = thermostat.currentValue("thermostatFanMode")
    log.debug "Current fan mode is ${currentFanMode}"
    
    if(currentFanMode == "fanOn")
    {
        parseAndSetFan(state.priorFanMode)
    }
    else {
        log.debug "Fan state changed to ${currentFanMode} since initial set, returning"
    }
    state.priorFanMode = ""
}

def parseAndSetFan(fanState) {
    if(fanState == "fanAuto")
    {
        thermostat.fanAuto()
    }
    else if(fanState == "fanOn") {
        thermostat.fanOn()
    }
    else if(fanState == "fanCirculate") {
        thermotat.fanCirculate()
    }
    else {
        log.debug "Couldn't parse fan state value: ${fanState}"
        thermostat.fanAuto()
    }
}