package com.alexhappytim.edhTGbot.backend.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TournamentApiFunctionalTest {
//    private static final Logger log = LoggerFactory.getLogger(TournamentApiFunctionalTest.class);
//
//    @Autowired
//    private MockMvc mockMvc;
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private static Long userId1;
//    private static Long userId2;
//    private static Long tournamentId;
//    private static Long matchId;
//
//    @Test
//    @Order(1)
//    void registerUsers() throws Exception {
//        String user1 = "{\"userTag\":\"alice\",\"displayName\":\"Alice\"}";
//        String user2 = "{\"userTag\":\"bob\",\"displayName\":\"Bob\"}";
//        MvcResult res1 = mockMvc.perform(post("/users")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(user1))
//                .andExpect(status().isOk()).andReturn();
//        userId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();
//        log.info("Registered user1: {}", res1.getResponse().getContentAsString());
//        MvcResult res2 = mockMvc.perform(post("/users")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(user2))
//                .andExpect(status().isOk()).andReturn();
//        userId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();
//        log.info("Registered user2: {}", res2.getResponse().getContentAsString());
//        assertThat(userId1).isNotNull();
//        assertThat(userId2).isNotNull();
//    }
//
//    @Test
//    @Order(2)
//    void createTournament() throws Exception {
//        String tournament = "{\"name\":\"EDH Test\",\"maxPlayers\":2}";
//        MvcResult res = mockMvc.perform(post("/tournaments")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(tournament))
//                .andExpect(status().isOk()).andReturn();
//        tournamentId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
//        log.info("Created tournament: {}", res.getResponse().getContentAsString());
//        assertThat(tournamentId).isNotNull();
//    }
//
//    @Test
//    @Order(3)
//    void registerParticipants() throws Exception {
//        String join1 = String.format("{\"userId\":%d}", userId1);
//        String join2 = String.format("{\"userId\":%d}", userId2);
//        MvcResult res1 = mockMvc.perform(post("/tournaments/" + tournamentId + "/participants")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(join1))
//                .andExpect(status().isOk()).andReturn();
//        log.info("User1 joined: {}", res1.getResponse().getContentAsString());
//        MvcResult res2 = mockMvc.perform(post("/tournaments/" + tournamentId + "/participants")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(join2))
//                .andExpect(status().isOk()).andReturn();
//        log.info("User2 joined: {}", res2.getResponse().getContentAsString());
//    }
//
//    @Test
//    @Order(4)
//    void startRoundAndSubmitResult() throws Exception {
//        mockMvc.perform(post("/tournaments/" + tournamentId + "/start-round"))
//                .andExpect(status().isOk());
//        log.info("Started round for tournament {}", tournamentId);
//        // Get pairings
//        MvcResult pairings = mockMvc.perform(get("/tournaments/" + tournamentId + "/pairings"))
//                .andExpect(status().isOk()).andReturn();
//        log.info("Pairings: {}", pairings.getResponse().getContentAsString());
//        matchId = objectMapper.readTree(pairings.getResponse().getContentAsString()).get(0).get("matchId").asLong();
//        // Submit result
//        String result = String.format("{\"matchId\":%d,\"scoreA\":1,\"scoreB\":0}", matchId);
//        mockMvc.perform(post("/tournaments/" + tournamentId + "/submit")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(result))
//                .andExpect(status().isOk());
//        log.info("Submitted match result for match {}", matchId);
//    }
//
//    @Test
//    @Order(5)
//    void getStandings() throws Exception {
//        MvcResult standings = mockMvc.perform(get("/tournaments/" + tournamentId + "/standings"))
//                .andExpect(status().isOk()).andReturn();
//        log.info("Standings: {}", standings.getResponse().getContentAsString());
//        assertThat(standings.getResponse().getContentAsString()).contains("alice");
//    }
}
