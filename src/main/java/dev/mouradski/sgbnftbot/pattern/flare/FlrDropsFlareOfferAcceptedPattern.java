package dev.mouradski.sgbnftbot.pattern.flare;

import dev.mouradski.sgbnftbot.model.Network;
import org.springframework.stereotype.Component;

@Component
public class FlrDropsFlareOfferAcceptedPattern extends dev.mouradski.sgbnftbot.pattern.songbird.FlrDropsOfferAcceptedPattern {

    @Override
    protected Network getNetwork() {
        return Network.FLARE;
    }

}
