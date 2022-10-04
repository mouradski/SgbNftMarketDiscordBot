package dev.mouradski.sgbnftbot.pattern.flare;

import dev.mouradski.sgbnftbot.model.Network;
import org.springframework.stereotype.Component;

@Component
public class NftsoFlareDirectBuyTransaction extends dev.mouradski.sgbnftbot.pattern.songbird.NftsoDirectBuyTransaction {

    @Override
    protected Network getNetwork() {
        return Network.FLARE;
    }
}
