package net.yourhome.server.ikea;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("socket")
public class SwitchDevice extends Device {

    private Boolean state;

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }
}
