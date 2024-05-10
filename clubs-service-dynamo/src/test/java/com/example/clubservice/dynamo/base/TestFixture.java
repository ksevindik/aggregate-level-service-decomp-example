package com.example.clubservice.dynamo.base;

import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.Player;

public class TestFixture {

    public Club club1, club2, club3;
    public Player player1, player2, player3, player4;

    public Club club1FromMonolith, club2FromMonolith, club3FromMonolith;
    public Player player1FromMonolith, player2FromMonolith, player3FromMonolith, player4FromMonolith;

    public TestFixture() {
        this.createTestFixture();
    }


    private void createTestFixture() {
        /*
        //service side
        club1 = clubRepository.save(new Club("GS", "TR", "FT"));
        club2 = clubRepository.save(new Club("BJK", "ES", "FU"));
        club3 = clubRepository.save(new Club("FB", "TR", "AK"));

        player1 = playerRepository.save(new Player("SGS", "TR", 100, club1));
        player2 = playerRepository.save(new Player("SYS", "TR", 90, club1));
        player3 = playerRepository.save(new Player("HS", "US", 80, club2));
        player4 = playerRepository.save(new Player("KS", "DE", 70, null));

        //monolith side
        club1FromMonolith = new Club("GS", "TR", "FT");
        club1FromMonolith.setId(456L);
        club2FromMonolith = new Club("BJK", "ES", "FU");
        club2FromMonolith.setId(123L);
        club3FromMonolith = new Club("FB", "TR", "AK");
        club3FromMonolith.setId(321L);


        player1FromMonolith = new Player(789L,player1.getName(),player1.getCountry(), player1.getRating(), club1FromMonolith);
        player2FromMonolith = new Player(790L,player2.getName(),player2.getCountry(), player2.getRating(), club1FromMonolith);
        player3FromMonolith = new Player(780L,player3.getName(),player3.getCountry(), player3.getRating(), club2FromMonolith);
        player4FromMonolith = new Player(770L,player4.getName(),player4.getCountry(), player4.getRating(), null);

        createIdMappings(
                new IdMapping(club1.getId(), club1FromMonolith.getId(), "Club"),
                new IdMapping(club2.getId(), club2FromMonolith.getId(), "Club"),
                new IdMapping(club3.getId(), club3FromMonolith.getId(), "Club"),
                new IdMapping(player1.getId(), player1FromMonolith.getId(), "Player"),
                new IdMapping(player2.getId(), player2FromMonolith.getId(), "Player"),
                new IdMapping(player3.getId(), player3FromMonolith.getId(), "Player"),
                new IdMapping(player4.getId(), player4FromMonolith.getId(), "Player"));

         */
    }
}
