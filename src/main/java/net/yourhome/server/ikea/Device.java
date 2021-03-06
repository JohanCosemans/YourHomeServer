package net.yourhome.server.ikea;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = BlindDevice.class),
                @JsonSubTypes.Type(value = SwitchDevice.class),
                @JsonSubTypes.Type(value = RepeaterDevice.class),
                @JsonSubTypes.Type(value = UnknownDevice.class)
        })
public class Device {

    private Integer id;
    private String name;
    private String type;
    private Integer typeId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTypeId() {
        return typeId;
    }
    public IkeaNetController.IkeaDeviceType getIkeaDeviceType() {
        return IkeaNetController.IkeaDeviceType.convert(getType());
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }
}
