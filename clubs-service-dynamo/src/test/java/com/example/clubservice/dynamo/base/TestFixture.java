package com.example.clubservice.dynamo.base;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;

public class TestFixture {

    public Club club1, club2, club3;
    public Player player1, player2, player3, player4;

    public Club club1FromMonolith, club2FromMonolith, club3FromMonolith;
    public Player player1FromMonolith, player2FromMonolith, player3FromMonolith, player4FromMonolith;

    private DynamoDBMapper dynamoDBMapper;

    public TestFixture(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.createTestFixture();
    }


    private void createTestFixture() {

        //monolith side
        club1FromMonolith = new Club(456L, "GS", "TR", "FT");
        club2FromMonolith = new Club(123L, "BJK", "ES", "FU");
        club3FromMonolith = new Club(321L, "FB", "TR", "AK");

        player1FromMonolith = new Player(789L,"SGS", "TR", 100, club1FromMonolith);
        player2FromMonolith = new Player(790L,"SYS", "TR", 90, club1FromMonolith);
        player3FromMonolith = new Player(780L,"HS", "US", 80, club2FromMonolith);
        player4FromMonolith = new Player(770L,"KS", "DE", 70, null);

        //service side
        ClubPlayerItem clubItem1 = ClubPlayerItem.fromClub(new Club(1111L,"GS", "TR", "FT"));
        ClubPlayerItem clubItem2 = ClubPlayerItem.fromClub(new Club(2222L, "BJK", "ES", "FU"));
        ClubPlayerItem clubItem3 = ClubPlayerItem.fromClub(new Club(3333L, "FB", "TR", "AK"));

        clubItem1.setMonolithId(club1FromMonolith.getId());
        clubItem2.setMonolithId(club2FromMonolith.getId());
        clubItem3.setMonolithId(club3FromMonolith.getId());

        dynamoDBMapper.save(clubItem1);
        dynamoDBMapper.save(clubItem2);
        dynamoDBMapper.save(clubItem3);

        club1 = clubItem1.toClub();
        club2 = clubItem2.toClub();
        club3 = clubItem3.toClub();

        player1 = new Player(4444L, "SGS", "TR", 100, club1);
        player2 = new Player(5555L, "SYS", "TR", 90, club1);
        player3 = new Player(6666L, "HS", "US", 80, club2);
        player4 = new Player(7777L, "KS", "DE", 70, null);

        ClubPlayerItem playerItem1 = ClubPlayerItem.fromPlayer(player1);
        ClubPlayerItem playerItem2 = ClubPlayerItem.fromPlayer(player2);
        ClubPlayerItem playerItem3 = ClubPlayerItem.fromPlayer(player3);
        ClubPlayerItem playerItem4 = ClubPlayerItem.fromPlayer(player4);

        playerItem1.setMonolithId(player1FromMonolith.getId());
        playerItem2.setMonolithId(player2FromMonolith.getId());
        playerItem3.setMonolithId(player3FromMonolith.getId());
        playerItem4.setMonolithId(player4FromMonolith.getId());

        dynamoDBMapper.save(playerItem1);
        dynamoDBMapper.save(playerItem2);
        dynamoDBMapper.save(playerItem3);
        dynamoDBMapper.save(playerItem4);

        player1 = playerItem1.toPlayer();
        player2 = playerItem2.toPlayer();
        player3 = playerItem3.toPlayer();
        player4 = playerItem4.toPlayer();
    }
}
