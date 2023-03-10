package id.holigo.services.common.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter

public class PassengerDto implements Serializable {

    private UUID id;

    private PassengerType type;

    private PassengerTitle title;

    private String name;

    private String phoneNumber;

    private IdentityCardDto identityCard;

    private PassportDto passport;

    private String baggageCode;

    private String seatCode;
}
