package dev.mouradski.sgbnftbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mouradski.sgbnftbot.model.Meta;
import dev.mouradski.sgbnftbot.model.SaleNotification;
import dev.mouradski.sgbnftbot.model.Subscription;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.core.entity.message.embed.EmbedBuilderDelegateImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@TestPropertySource(locations="classpath:test.properties")
public class SgbNftMarketBotTest {

    @Autowired
    @InjectMocks
    private SgbNftMarketBot sgbNftMarketBotWIP;

    @MockBean
    private IpfsHelper ipfsHelper;

    @MockBean
    private DiscordApi discordApi;

    @MockBean
    private SubscriptionService subscriptionService;

    @Mock
    private TextChannel textChannelMock;

    @Mock
    private CompletableFuture completableFuture;

    @Captor
    ArgumentCaptor<EmbedBuilder> embedMessageCapture;

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    public void initTest() throws IOException {
        Mockito.reset(ipfsHelper, discordApi, subscriptionService, textChannelMock);
        Mockito.when(discordApi.getTextChannelById(eq("12345"))).thenReturn(Optional.of(textChannelMock));

        Subscription subscription = Subscription.builder().channelId("12345").tokenName("TOKEN_NAME").build();
        Mockito.when(subscriptionService.getByContract(anyString())).thenReturn(Arrays.asList(subscription));

        Mockito.when(ipfsHelper.getMeta(anyString())).thenReturn(Optional.of(Meta.builder().image("IMAGE_URL").build()));
        Mockito.when(ipfsHelper.get(eq("IMAGE_URL"))).thenReturn(Optional.empty());


        Mockito.when(completableFuture.join()).thenReturn(null);
        Mockito.when(textChannelMock.sendMessage(any(EmbedBuilder.class))).thenReturn(completableFuture);

    }

    @ParameterizedTest
    @CsvSource({
            "0x3c89e61195306b07aee8e5f41cfa42760b291db5d3f0e73f745947e05f4b652b,0xcdb019c0990c033724da55f5a04be6fd6ec1809d,0x29ca23b5ae0cbca059bc4d495fedc407b164018d,15995,25000,SparklesNFT,OFFER_ACCEPTED",
            "0x05b765c21e2238b100dabaf362cb3b2f2fb789bb485a21780c57902c40e5c774,0xe6e7db32df87f75609fb78d5f52753c2d3d98d84,0xe3609c5a35702782963309c38852b6fcdd59230d,2081,425,SparklesNFT,BUY",
            "0x545c89a5b8823658e5ab666699e6d0d97c98a4ea7881d0d8fb8e10462c9ffd56,0x2972ea6e6cc45c5837ce909def032dd325b48415,0x1e3cec41608c438ca7524d6fb042f905c46ecbae,3738,132.6,NFTSO,BUY",
            "0x6fa19da322f507cce3c1707493de576988ce21d359acd007593e4ffb4f4f038f,0x4f52a074de9f2651d2f711fee63fee9e3b439a7e,0x1e3cec41608c438ca7524d6fb042f905c46ecbae,964,500,SparklesNFT,BUY",
            "0x15446be772557f9ad441544b381b243baf7b9fd86a0dead0cd0e9760b34abefa,0xe6e7db32df87f75609fb78d5f52753c2d3d98d84,0x943e22d186dbbd9a023d508b90d8f7dae57d27e7,1907,350,SparklesNFT,OFFER_ACCEPTED",
            "0x97b10637cc810a987caaa377ab1bdfd91e7092a31d66b00317b7553b203f29c5,0x2d086e61267a57503dd4aa1bb4e807bc50fa7ee1,0xce9de01156bc1281bac9254e49ba5d325bf2f6d8,1834,500,SparklesNFT,BUY",
            "0x5eb2fa413f688d5cc15f2e55b5d3d883351f5afb56c6756badfbfac8ad1f4802,0xcdb019c0990c033724da55f5a04be6fd6ec1809d,0x6062cbdfb38e8d09e1180341b0d38eef750292b3,13896,625,SparklesNFT,OFFER_ACCEPTED",
            "0x33a15d8174a1d2e7b48d77594002658e8f5157ee62bc40fe4be80fde3bc758c2,0x279a222a18c033124ab02290ddec97912a8b7185,0x17f2a558e29a8ca7b994c2527f6ff8d4dd4d2d81,7274,620,SparklesNFT,OFFER_ACCEPTED",
            "0xe697b51a94a6648337d979d8bd5a1c8ee97ede12bd87a3dad33d023c96527990,0xcdb019c0990c033724da55f5a04be6fd6ec1809d,0x7b85de63bfaf89e8b6bffe6f38697a1115cef8e3,20195,1390,SparklesNFT,BUY",
            "0x5c8166e6873a2612b5c042aaa0b94834b1752183795bf36ee75b5de95d81c146,0xcdb019c0990c033724da55f5a04be6fd6ec1809d,0x7b85de63bfaf89e8b6bffe6f38697a1115cef8e3,21972,1200,SparklesNFT,BUY",
            "0xce5781c10cf196a6b59683b9976b4509e677210c4d5506a7df53c5713b0c24d9,,,,,,",
            "0x97321d554675b4c0e85ce99a9abeb77c4148d6522657033572ab327631a5d514,,,,,,"})
    public void testProcess(String transactionHash, String contract, String buyer, Long tokenId, Double price, String marketplace, String transactionType) throws URISyntaxException, IOException {
        Optional<SaleNotification> saleNotification = sgbNftMarketBotWIP.process(transactionHash);

        if (contract == null) {
            Assertions.assertFalse(saleNotification.isPresent());
        } else {
            Assertions.assertTrue(saleNotification.isPresent());
            Assertions.assertEquals(contract, saleNotification.get().getContract());
            Assertions.assertEquals(buyer, saleNotification.get().getBuyer());
            Assertions.assertEquals(tokenId, saleNotification.get().getTokenId());
            Assertions.assertEquals(price, saleNotification.get().getPrice());
            Assertions.assertEquals(marketplace, saleNotification.get().getMarketplace().toString());
            Assertions.assertEquals(transactionType, saleNotification.get().getTransactionType().name());

            Mockito.verify(textChannelMock).sendMessage(embedMessageCapture.capture());

            EmbedBuilder embedMsg = embedMessageCapture.getValue();

            String jsonMsg = ((EmbedBuilderDelegateImpl)embedMsg.getDelegate()).toJsonNode().toString();

            String jsonMsgTemplate = new String(Files.readAllBytes(Paths.get(getClass().getResource("/jsonMsgTemplate.json").toURI())));

            String expectedJsonMsg =
                    jsonMsgTemplate.replace("_BUYER_", buyer)
                            .replace("_PRICE_", NumberFormat.getNumberInstance(Locale.US).format(price) + " SGB")
                            .replace("_TITLE_", "TOKEN_NAME #" + tokenId + " has been sold !")
                            .replace("_LISTING_URL_", marketplace.equalsIgnoreCase("NFTSO") ?  ("https://nftso.xyz/item-details/19/" + contract + "/" + tokenId) :
                                            ("https://www.sparklesnft.com/item/" + contract + "_" + tokenId))
                            .replace("_MARKETPLACE_", marketplace)
                            .replace("_TOKEN_ID_", tokenId.toString())
                            .replace("_TRANSACTION_TYPE_", "BUY".equalsIgnoreCase(transactionType) ? "Buy" : "Offer Accepted");

            Assertions.assertEquals(OBJECT_MAPPER.readTree(expectedJsonMsg), OBJECT_MAPPER.readTree(jsonMsg));
        }
    }

}
