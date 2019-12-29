package net.yourhome.server.ikea;

public class Device {

    private Integer id;
    private String name;
    private String type;
    private Integer typeId;
    private Double state;

    public Double getState() {
        return state;
    }

    public void setState(Double state) {
        this.state = state;
    }

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
