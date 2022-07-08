package dev.mouradski.sgbnftbot.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mouradski.sgbnftbot.model.Meta;
import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class IpfsHelper {


    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private RestTemplate restTemplate;

    private List<IPFS> ipfsList = new ArrayList<>();

    private List<String> ipfsGateways = Arrays.asList("https://ipfs.io/ipfs/", "https://sparkles.mypinata.cloud/ipfs/", "https://ipfs.io/ipfs/");

    public IpfsHelper(@Autowired RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        MultiAddress addr = new MultiAddress("/ip4/195.154.200.130/tcp/5001");
        ipfsList.add(new IPFS(addr.getHost(), addr.getTCPPort(), "/api/v0/", 20000, 30000, false));

        MultiAddress addrBackup = new MultiAddress("/dnsaddr/ipfs.infura.io/tcp/5001/https");
        ipfsList.add(new IPFS(addrBackup.getHost(), addrBackup.getTCPPort(), "/api/v0/", 20000, 250000, true));

    }

    public byte[] get(String uri) {
        for (IPFS ipfs : ipfsList) {
            try {
                return get(uri, ipfs);
            } catch (Exception e) {
                log.error("error retrieving IPFS resource", uri, e.getMessage());
            }
        }
        return null;
    }

    public Meta getMeta(String uri) throws IOException {
        byte[] content = get(uri);

        if (content != null) {
            return objectMapper.readValue(content, Meta.class);
        }

        if (uri.contains("/ipfs/")) {
            content = get(uri.split("/ipfs/")[1]);
        }

        if (content != null) {
            return objectMapper.readValue(content, Meta.class);
        }

        return getMetaFromGateway(uri);
    }

    public Meta getMetaNoIpfsUrl(String url) {
        ResponseEntity<dev.mouradski.sgbnftbot.model.Meta> responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, buildEmptyEntity(), dev.mouradski.sgbnftbot.model.Meta.class);
        return responseEntity.getBody();
    }

    public Meta getMetaFromGateway(String url, String httpGateway) {
        String jsonUrl = null;

        if (url.startsWith("http")) {
            jsonUrl = url;
        } else {
            jsonUrl = url.replace("ipfs://", httpGateway);
        }

        ResponseEntity<dev.mouradski.sgbnftbot.model.Meta> responseEntity = restTemplate.exchange(jsonUrl, HttpMethod.GET, buildEmptyEntity(), dev.mouradski.sgbnftbot.model.Meta.class);

        return responseEntity.getBody();
    }

    public Meta getMetaFromGateway(String url) {

        for (String gateway: ipfsGateways) {
            try {
                return getMetaFromGateway(url, gateway);
            } catch (Exception e) {
                return getMetaNoIpfsUrl(url);
            }
        }

       return null;
    }

    private HttpEntity buildEmptyEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.115 Safari/537.36");
        HttpEntity entity = new HttpEntity("", headers);
        return entity;
    }

    public byte[] get(String uri, IPFS ipfsProvider) throws IOException {
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

        return content;
    }
}
