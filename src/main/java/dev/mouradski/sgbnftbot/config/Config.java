package dev.mouradski.sgbnftbot.config;

import okhttp3.OkHttpClient;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.Arrays;

@Configuration
public class Config {

    @Bean
    public RestTemplate restTemplate() {
        var restTemplate = new RestTemplate();

        var converter = new MappingJackson2HttpMessageConverter();

        converter.setSupportedMediaTypes(
                Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));

        restTemplate.setMessageConverters(Arrays.asList(converter, new FormHttpMessageConverter()));
        return restTemplate;
    }


    @Bean("songbirdWeb3")
    public Web3j songbirdWeb3(@Value("${web3.songbird.provider}") String songbirdRpcProviderUrl) {
        var httpClient = new OkHttpClient.Builder();
        var client = httpClient.build();

        var httpService = new HttpService(songbirdRpcProviderUrl, client);

        return Web3j.build(httpService);
    }


    @Bean("flareWeb3")
    public Web3j flaredWeb3(@Value("${web3.flare.provider}") String flareRpcProviderUrl) {
        var httpClient = new OkHttpClient.Builder();
        var client = httpClient.build();

        var httpService = new HttpService(flareRpcProviderUrl, client);

        return Web3j.build(httpService);
    }

    @Bean
    @ConditionalOnProperty(value = "app.production")
    public DiscordApi discordApi(@Value("${discord.token}") String discordToken) {
        return new DiscordApiBuilder().setToken(discordToken)
                .setAllIntents()
                .login().join();
    }
}
