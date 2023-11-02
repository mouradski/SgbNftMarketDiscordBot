package dev.mouradski.sgbnftbot.pattern.flare;

import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.pattern.songbird.SparklesWSgbTokenDirectBuyPattern;
import org.springframework.stereotype.Component;

@Component
public class SparklesWFlrTokenDirectBuyPattern extends SparklesWSgbTokenDirectBuyPattern {
    @Override
    protected Network getNetwork() {
        return Network.FLARE;
    }
}
