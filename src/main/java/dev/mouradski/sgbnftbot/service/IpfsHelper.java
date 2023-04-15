package dev.mouradski.sgbnftbot.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mouradski.sgbnftbot.model.Meta;
import dev.mouradski.sgbnftbot.model.Network;
import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class IpfsHelper {

    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private RestTemplate restTemplate;

    private EthHelper ethHelper;

    private List<IPFS> ipfsList = new ArrayList<>();

    private List<String> ipfsGateways = Arrays.asList("https://ipfs.io/ipfs/", "https://sparkles.mypinata.cloud/ipfs/", "https://ipfs.io/ipfs/");

    public IpfsHelper(RestTemplate restTemplate, EthHelper ethHelper) {
        this.restTemplate = restTemplate;
        this.ethHelper = ethHelper;
    }

    @PostConstruct
    public void init() {
        MultiAddress addr = new MultiAddress("/ip4/195.154.200.130/tcp/5001");
        ipfsList.add(new IPFS(addr.getHost(), addr.getTCPPort(), "/api/v0/", 20000, 30000, false));
    }

    public Optional<byte[]> get(String uri) {
        for (IPFS ipfs : ipfsList) {
            try {
                return get(uri, ipfs);
            } catch (Exception e) {
                log.error("error retrieving IPFS resource", uri, e.getMessage());
            }
        }
        return Optional.empty();
    }

    public Optional<Meta> getMeta(String uri) throws IOException {
        Optional<byte[]> content = get(uri);

        if (content.isPresent()) {
            return Optional.of(objectMapper.readValue(content.get(), Meta.class));
        }

        if (uri.contains("/ipfs/")) {
            content = get(uri.split("/ipfs/")[1]);
        }

        if (content.isPresent()) {
            return Optional.of(objectMapper.readValue(content.get(), Meta.class));
        }

        return getMetaFromGateway(uri);
    }

    public Optional<Meta> getMetaNoIpfsUrl(String url) {
        ResponseEntity<dev.mouradski.sgbnftbot.model.Meta> responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, buildEmptyEntity(), dev.mouradski.sgbnftbot.model.Meta.class);

        if (HttpStatus.OK == responseEntity.getStatusCode()) {
            return Optional.of(responseEntity.getBody());
        } else {
            log.error("Error retrieving Metadata from {}, httpStatus : {}", url, responseEntity.getStatusCode());
            return Optional.empty();
        }
    }

    public Optional<Meta> getMetaFromGateway(String url, String httpGateway) {
        String jsonUrl = null;

        if (url.startsWith("http")) {
            jsonUrl = url;
        } else {
            jsonUrl = url.replace("ipfs://", httpGateway);
        }

        ResponseEntity<dev.mouradski.sgbnftbot.model.Meta> responseEntity = restTemplate.exchange(jsonUrl, HttpMethod.GET, buildEmptyEntity(), dev.mouradski.sgbnftbot.model.Meta.class);

        if (HttpStatus.OK == responseEntity.getStatusCode()) {
            return Optional.of(responseEntity.getBody());
        } else {
            log.error("Error retrieving Metadata from {}, httpStatus : {}", jsonUrl, responseEntity.getStatusCode());
            return Optional.empty();
        }

    }

    public Optional<Meta> getMetaFromGateway(String url) {
        for (String gateway: ipfsGateways) {
            try {
                return getMetaFromGateway(url, gateway);
            } catch (Exception e) {
                return getMetaNoIpfsUrl(url);
            }
        }

       return Optional.empty();
    }

    private HttpEntity<String> buildEmptyEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.115 Safari/537.36");
        return new HttpEntity("", headers);
    }

    public Optional<byte[]> get(String uri, IPFS ipfsProvider) throws IOException {
        String[] ipfsArgs = uri.contains("/ipfs/") ?
                uri.split("/ipfs/")[1].split("/") :
                uri.replace("ipfs://", "").split("/");

        byte[] content = null;

        Multihash filePointer = Multihash.fromBase58(ipfsArgs[0]);

        if (ipfsArgs.length == 1) {
            content = ipfsProvider.cat(filePointer);
        } else if (ipfsArgs.length == 2) {
            content = ipfsProvider.cat(filePointer, "/" + ipfsArgs[1]);
        } else {
            content = ipfsProvider.cat(filePointer, "/" + ipfsArgs[1] + "/" + ipfsArgs[2]);
        }

        return content == null ? Optional.empty() : Optional.of(content);
    }


    public Optional<Meta> retreiveMetaFromCollection(String contract, Network network) throws IOException {

        int i = 0;

        Optional<String> metaIpfsUri = Optional.empty();

        while (i++ < 100 && !metaIpfsUri.isPresent()) {
            metaIpfsUri = ethHelper.getTokenUri(contract, Long.valueOf(i), network);
        }


        return metaIpfsUri.isPresent() ? getMeta(metaIpfsUri.get()) : Optional.empty();
    }
}
