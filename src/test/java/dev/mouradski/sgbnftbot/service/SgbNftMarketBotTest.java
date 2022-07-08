package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.SaleNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

@SpringBootTest
@TestPropertySource(locations="classpath:test.properties")
public class SgbNftMarketBotTest {

    @Autowired
    private SgbNftMarketBot sgbNftMarketBotWIP;

    @ParameterizedTest
    @CsvSource({
            "0x3c89e61195306b07aee8e5f41cfa42760b291db5d3f0e73f745947e05f4b652b,0xcdb019c0990c033724da55f5a04be6fd6ec1809d,0x9936907dcc2026bf4d4f882aee3900bdba6d4fbc,15995,23750,SparklesNFT,OFFER_ACCEPTED",
            "0x05b765c21e2238b100dabaf362cb3b2f2fb789bb485a21780c57902c40e5c774,0xe6e7db32df87f75609fb78d5f52753c2d3d98d84,0xe3609c5a35702782963309c38852b6fcdd59230d,2081,425,SparklesNFT,BUY",
            "0x545c89a5b8823658e5ab666699e6d0d97c98a4ea7881d0d8fb8e10462c9ffd56,0x2972ea6e6cc45c5837ce909def032dd325b48415,0x1e3cec41608c438ca7524d6fb042f905c46ecbae,3738,132.6,NFTSO,BUY",
            "0x6fa19da322f507cce3c1707493de576988ce21d359acd007593e4ffb4f4f038f,0x4f52a074de9f2651d2f711fee63fee9e3b439a7e,0x1e3cec41608c438ca7524d6fb042f905c46ecbae,964,500,SparklesNFT,BUY",
            "0x15446be772557f9ad441544b381b243baf7b9fd86a0dead0cd0e9760b34abefa,0xe6e7db32df87f75609fb78d5f52753c2d3d98d84,0x5558e1d55e8619ce93498cd3113435515d48eb03,1907,315,SparklesNFT,OFFER_ACCEPTED",
            "0xce5781c10cf196a6b59683b9976b4509e677210c4d5506a7df53c5713b0c24d9,,,,,,",
            "0x97321d554675b4c0e85ce99a9abeb77c4148d6522657033572ab327631a5d514,,,,,,"})
    public void testProcess(String transactionHash, String contract, String trigger, Long tokenId, Double price, String marketplace, String transactionType) throws Exception {
        Optional<SaleNotification> saleNotification = sgbNftMarketBotWIP.process(transactionHash);

        if (contract == null) {
            Assertions.assertFalse(saleNotification.isPresent());
        } else {
            Assertions.assertTrue(saleNotification.isPresent());
            Assertions.assertEquals(contract, saleNotification.get().getContract());
            Assertions.assertEquals(trigger, saleNotification.get().getTrigger());
            Assertions.assertEquals(tokenId, saleNotification.get().getTokenId());
            Assertions.assertEquals(price, saleNotification.get().getPrice());
            Assertions.assertEquals(marketplace, saleNotification.get().getMarketplace().toString());
            Assertions.assertEquals(transactionType, saleNotification.get().getTransactionType().name());
        }
    }


}
