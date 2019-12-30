package net.yourhome.server.ikea;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("blind")
public class BlindDevice extends Device {

    private Double state;

    public Double getState() {
        return state;
    }

    public void setState(Double state) {
        this.state = state;
    }
}
