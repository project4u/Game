package com.example.game.model;

import com.example.game.Utils;
import com.example.game.exception.InvalidGameActionException;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jdk.jshell.execution.Util;
import lombok.Getter;
import lombok.Setter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import netscape.javascript.JSObject;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.websocket.server.ServerEndpoint;
import java.util.*;

@Entity
@Table(name = "games")
public class Game extends Auditable{
    @ManyToMany
    @Getter @Setter
    @JsonIdentityReference
    private Set<Player> players=new HashSet<>();

    @NotNull
    @Getter @Setter
    @ManyToOne
    private GameMode gameMode;

    @OneToMany(mappedBy = "game",cascade = CascadeType.ALL)
    @JsonManagedReference
    @OrderBy(value = "round_number asc")
    private List<Round> rounds=new ArrayList<>();

    @Getter @Setter
    private int numRounds = 10;

    @Getter @Setter
    private Boolean hasEllen =false;

    @Getter @Setter
    @ManyToOne
    @NotNull
    @JsonIdentityReference
    private Player leader;

    @ManyToMany(cascade = CascadeType.ALL)
    @Getter @Setter
    @JsonIdentityReference
    private Map<Player, Stat> playerStats = new HashMap<>();

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus=GameStatus.PLAYERS_JOINING;

    @ManyToMany
    @Getter @Setter
    private Set<Player> readyPlayers = new HashSet<>();

    public Game(){}

    public Game(Builder builder) {
        super();
        setGameMode(builder.gameMode);
        setNumRounds(builder.numRounds);
        setHasEllen(builder.hasEllen);
        setGameStatus(builder.gameStatus);
        setLeader(builder.leader);
        try {
            addPlayer(builder.leader);
        } catch (InvalidGameActionException e) {
            e.printStackTrace();
        }
    }

    public void addPlayer(Player player) throws InvalidGameActionException {
        if(!gameStatus.equals(GameStatus.PLAYERS_JOINING)){
            throw new InvalidGameActionException("Cannot Join as Game already started");
        }
        players.add(player);
        player.setCurrentGame(this);
    }

    public void removePlayer(Player player) throws InvalidGameActionException {
        if(!players.contains(player))
            throw new InvalidGameActionException("No Such player was in the game");
        players.remove(player);
        if(player.getCurrentGame().equals(this))
            player.setCurrentGame(null);
        if(players.size()==0 || (players.size()==1 && !gameStatus.equals(GameStatus.PLAYERS_JOINING))){
            endGame();
        }
    }

    public void startGame(Player player) throws InvalidGameActionException {
        if(!gameStatus.equals(GameStatus.PLAYERS_JOINING))
            throw new InvalidGameActionException("The Game has already Started");
        if(players.size()<2)
            throw new InvalidGameActionException("Can't Start Game with a Single Player");
        if(!player.equals(leader)){
            throw new InvalidGameActionException("Only Leader can Start the game");
        }
        startNewRound();
    }

    public void startNewRound() {
        gameStatus=GameStatus.SUBMITTING_ANSWERS;
        Question question= Utils.getRandomQuestion(gameMode);
        System.out.println(question.getQuestion());
        Round round=new Round(this, question,rounds.size()+1);
        System.out.println(round.getQuestion());
        if(hasEllen){
            round.setEllenAnswer(Utils.getRandomEllenAnswer(question));
        }
        rounds.add(round);
    }

    public void submitAnswer(Player player, String answer) throws InvalidGameActionException {
        if(answer.length()==0)
            throw new InvalidGameActionException("Answer cannot be empty");
        if(!players.contains(player))
            throw new InvalidGameActionException("No Such player exists in the game");
        if(!gameStatus.equals((GameStatus.SUBMITTING_ANSWERS)))
            throw new InvalidGameActionException("Game is not accepting answer at present");
        Round currentRound=getCurrentRound();
        getCurrentRound().submitAnswer(player,answer);
        if(currentRound.allAnswersSubmitted(players.size())){
            gameStatus=GameStatus.SELECTING_ANSWERS;
        }
    }
    public void selectAnswer(Player player,PlayerAnswer selectedAnswer) throws InvalidGameActionException {
        if(players.contains(player))
            throw new InvalidGameActionException("No Such Player present in the game");
        if(!gameStatus.equals(GameStatus.SELECTING_ANSWERS))
            throw new InvalidGameActionException("Game is not Accepting answer at present");
        Round currentRound=getCurrentRound();
        getCurrentRound().selectAnswer(player,selectedAnswer);
        if(currentRound.allAnswersSelected(players.size())){
            if(rounds.size()<numRounds)
                gameStatus=GameStatus.WAITING_FOR_READY;
            else
                endGame();
        }
    }

    public void playerIsReady(Player player) throws InvalidGameActionException {
        if(players.contains(player))
            throw new InvalidGameActionException("No Such Player present in the game");
        if(!gameStatus.equals(GameStatus.WAITING_FOR_READY))
            throw new InvalidGameActionException("Game is not waiting for players to be ready");
        readyPlayers.add(player);
        if(rounds.size()==numRounds)
            endGame();
        else
        if(readyPlayers.size()==players.size())
            startNewRound();
    }

    public void playerIsNotReady(Player player) throws InvalidGameActionException {
        if(players.contains(player))
            throw new InvalidGameActionException("No Such Player present in the game");
        if(!gameStatus.equals(GameStatus.WAITING_FOR_READY))
            throw new InvalidGameActionException("Game is not waiting for players to be ready");
        readyPlayers.remove(player);
    }

    public Round getCurrentRound() throws InvalidGameActionException {
        if(rounds.size()==0)
            throw new InvalidGameActionException("Game not started yet");
        return rounds.get(rounds.size()-1);
    }

    private void endGame() {
        gameStatus = GameStatus.ENDED;
        for (Player player : players) {
            if (player.getCurrentGame().equals(this))
                player.setCurrentGame(null);
            Stat oldPlayerStats = player.getStats();
            Stat deltaPlayerStats = playerStats.get(player);
            oldPlayerStats.setCorrectAnswerCount(oldPlayerStats.getCorrectAnswerCount() + deltaPlayerStats.getCorrectAnswerCount());
            oldPlayerStats.setGamePsychedCount(oldPlayerStats.getGamePsychedCount() + deltaPlayerStats.getGamePsychedCount());
            oldPlayerStats.setPsychedOthersCount(oldPlayerStats.getPsychedOthersCount() + deltaPlayerStats.getPsychedOthersCount());
        }
    }

    public void endGame(Player player) throws InvalidGameActionException {
        if (gameStatus.equals(GameStatus.ENDED))
            throw new InvalidGameActionException("The game has already ended");
        if (!player.equals(leader))
            throw new InvalidGameActionException("Only the leader can end the game");
        endGame();
    }

    public JSONObject getRoundData() throws InvalidGameActionException {
        return getCurrentRound().getRoundData();
    }

    public String getSecretCode() {
        return Utils.getSecretCodeFromGameId(getId());
    }

    /*public void createNewRound() {
        gameStatus=GameStatus.SUBMITTING_ANSWERS;
    }

    public JSONObject getGameState(){
        JSONObject state=new JSONObject();
        state.put("id",getId());
        state.put("numRounds",getNumRounds());
        state.put("mode",getGameMode().getName());
        JSONArray playerData = new JSONArray();
        for(Player player : players){
            JSONObject data=new JSONObject();
            data.put("alias",player.getAlias());
            playerData.add(data);
        }
        state.put("players",playerData);
        return state;
    }*/












    public static final class Builder{
        private @NotNull GameMode gameMode;
        private Integer numRounds = 10;
        private Boolean hasEllen =false;
        private GameStatus gameStatus;
        private @NotNull Player leader;

        public Builder gameMode(@NotNull GameMode val){
            gameMode=val;
            return this;
        }
        public Builder numRounds(Integer val){
            numRounds=val;
            return this;
        }
        public Builder hasEllen(Boolean val){
            hasEllen=val;
            return this;
        }
        public Builder gameStatus(GameStatus val){
            gameStatus=val;
            return this;
        }
        public Builder leader(Player val){
            leader=val;
            return this;
        }
        public Game build(){
            return new Game(this);
        }
    }
}
