package com.example.mygenerics.web.webclient;

import com.example.mygenerics.web.webclient.models.UserData;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class GenericWebclientTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void postForSingleObjResponse() {
    }

    @Test
    void postForCollectionResponse() {
    }

    @Test
    void getForCollectionResponse() {
    }

    @Test
    void getForSingleObjResponse() throws URISyntaxException {
        var res =GenericWebclient.getForSingleObjResponse("https://reqres.in/api/users/2", UserData.class);
        assertNotNull(res);
        var body = res.block();
        log.info("BODY: {}", body);
        assertEquals(Objects.requireNonNull(body).getData().getId(), (int) Objects.requireNonNull(body).getData().getId());
    }
}