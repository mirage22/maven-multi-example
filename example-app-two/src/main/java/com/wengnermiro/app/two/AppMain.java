/*
 * Copyright (C)  2018 Miroslav Wengner
 *                        http://www.wengnermiro.com/
 *
 *  This software is free:
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *   OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *   IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *   NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Copyright (C) Miroslav Wengner, 2018
 */

package com.wengnermiro.app.two;

import com.wengnermiro.app.two.config.AppConfig;
import com.wengnermiro.commons.CommonFeature;
import com.wengnermiro.commons.dto.BasicStatus;
import com.wengnermiro.commons.dto.DataStatus;
import com.wengnermiro.commons.dto.SampleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * AppMain simple application
 *
 * @author Miroslav Wengner (@miragemiko)
 */
@EnableEurekaClient
@RestController
@EnableAutoConfiguration
@EnableConfigurationProperties
@Import(value = AppConfig.class)
@RequestMapping(value = "/two")
public class AppMain {

    private static final String NAME = "secondService";
    private static boolean active;

    private final AtomicInteger counter = new AtomicInteger();
    private final Map<Integer, SampleData> storage = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(AppMain.class, args);
    }

    public static boolean isActive() {
        return active;
    }

    private final RestTemplate restTemplate;

    @Autowired
    public AppMain(final RestTemplate restTemplate) {
        active = true;
        this.restTemplate = restTemplate;
    }

    @RequestMapping(method =
            RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE},
            consumes = {APPLICATION_JSON_VALUE})
    @ResponseBody
    public BasicStatus appStatusGet() {
        BasicStatus status = new BasicStatus();
        status.setName(NAME);
        status.setState(active);
        return status;
    }

    @RequestMapping(value = "/data", method =
            RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE},
            consumes = {APPLICATION_JSON_VALUE})
    @ResponseBody
    public DataStatus dataGet() {
        ResponseEntity<DataStatus> responseData = requestDataGet(restTemplate, "http://example-app-one/one/data");
        DataStatus remoteData = responseData.getBody();
        DataStatus extendedData = CommonFeature.createData(storage, true);
        if (remoteData != null && remoteData.getMessagge() != null) {
            extendedData.setMessagge(extendedData.getMessagge().concat(",REMOTE:").concat(remoteData.getMessagge()));
        }
        return extendedData;
    }

    @RequestMapping(value = "/add", method =
            RequestMethod.POST,
            produces = {APPLICATION_JSON_VALUE},
            consumes = {APPLICATION_JSON_VALUE})
    @ResponseBody
    public DataStatus addDataPost(@RequestBody SampleData data) {
        storage.put(counter.getAndIncrement(), data);
        return CommonFeature.createData(storage, true);
    }


    private ResponseEntity<DataStatus> requestDataGet(RestTemplate restTemplate, String url, Object... vars) {
        HttpHeaders header = new HttpHeaders();
        header.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        HttpEntity<DataStatus> httpEntity = new HttpEntity<>(header);
        return restTemplate.exchange(url, HttpMethod.GET,
                httpEntity, new ParameterizedTypeReference<DataStatus>() {
                }, vars);

    }
}
