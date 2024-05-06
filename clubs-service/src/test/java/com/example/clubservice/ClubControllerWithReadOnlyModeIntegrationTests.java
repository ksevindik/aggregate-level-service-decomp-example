package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestPropertySource(properties = {
        "service.test.monolith.entity-change-event-publisher.enabled=true"
})
public class ClubControllerWithReadOnlyModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @Test
    public void testGetAllClubs() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/ES");
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testGetClubById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<Club> response = performGetClubRequest("/clubs/" + testFixture.club1FromMonolith.getId());
        verifyGetResponse(response, testFixture.club1FromMonolith);
    }

    @Test
    public void testCreateClub() {
        /*
        new club should be created on the monolith side
        entity change event published from the monolith side should be consumed and club should be persisted on the service side
        entity ids should be from the monolith side
         */
        registerMonolithResponse("/clubs", "POST", """
                {
                    "name": "RO",
                    "country": "ES",
                    "president": "XX"
                }
                """, 201, """
                {
                    "id": 555,
                    "name": "RO",
                    "country": "ES",
                    "president": "XX"
                }
                """);

        //before create
        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(654L, "Club");
        assertNull(idMapping);

        Club club = new Club("RO", "ES", "XX");
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);

        //after create
        verifyClub(new Club(555L,"RO", "ES", "XX"), savedClub);

        waitForEntityPersistedEvent();
        idMapping = idMappingRepository.findByMonolithIdAndTypeName(555L, "Club");
        assertNotNull(idMapping);

        Club clubFromDB = clubRepository.findById(idMapping.getServiceId()).orElse(null);
        verifyClub(new Club(idMapping.getServiceId(),"RO", "ES", "XX"), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        /*
        club should be updated on the monolith side
        entity change event published from the monolith side should be consumed and club should be updated on the service side
        entity ids should be from the monolith side
         */
        registerMonolithResponse("/clubs/321/president", "PUT", "AY", 200, """
                {
                    "id": 321,
                    "name": "FB",
                    "country": "TR",
                    "president": "AY",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);

        //before update
        verifyClub(new Club(testFixture.club3.getId(),"FB", "TR", "AK"), testFixture.club3);

        Club updatedClub = restTemplate.exchange("/clubs/321/president",
                HttpMethod.PUT, new HttpEntity<String>("AY"), Club.class).getBody();

        //after update
        verifyClub(new Club(321L,"FB", "TR", "AY"), updatedClub);

        waitForEntityPersistedEvent();
        Club clubFromDB = clubRepository.findById(testFixture.club3.getId()).get();
        verifyClub(new Club(testFixture.club3.getId(),"FB", "TR", "AY"), clubFromDB);
    }
}
