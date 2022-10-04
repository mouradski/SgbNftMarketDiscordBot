package dev.mouradski.sgbnftbot.pattern.flare;

import dev.mouradski.sgbnftbot.model.Network;
import org.springframework.stereotype.Component;

@Component
public class FlrDropsFlareDirectBuyTransaction extends dev.mouradski.sgbnftbot.pattern.songbird.FlrDropsDirectBuyTransaction {

    @Override
    protected Network getNetwork() {
        return Network.FLARE;
    }
}
