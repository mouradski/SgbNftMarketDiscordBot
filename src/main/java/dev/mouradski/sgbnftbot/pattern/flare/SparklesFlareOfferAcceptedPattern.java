package dev.mouradski.sgbnftbot.pattern.flare;

import dev.mouradski.sgbnftbot.model.Network;
import org.springframework.stereotype.Component;

@Component
public class SparklesFlareOfferAcceptedPattern extends dev.mouradski.sgbnftbot.pattern.songbird.SparklesOfferAcceptedPattern{

    @Override
    protected Network getNetwork() {
        return Network.FLARE;
    }
}
