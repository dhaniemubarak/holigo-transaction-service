package id.holigo.services.common.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AirportDto implements Serializable {

    private String id;

    private String name;

    private Boolean isInternational;

    private Boolean isDomestic;

    private String city;

    private String country;

    private String ianaTimezone;

    private String windowsTimezone;

}
