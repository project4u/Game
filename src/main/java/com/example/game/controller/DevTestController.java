package com.example.game.controller;

import com.example.game.Constants;
import com.example.game.Pair;
import com.example.game.Utils;
import com.example.game.model.*;
import com.example.game.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
@RequestMapping("/")
public class DevTestController {

    @GetMapping("/dev-test")
    public String sayHello(){
        return "Hello World";
    }

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoundRepository roundRepository;
    @Autowired
    private GameModeRepository gameModeRepository;

    @GetMapping("/dev-test/players")
    public List<Player> getAllPlayers(){
        return playerRepository.findAll();
    }

    @GetMapping("/dev-test/players/{id}")
    public Player getPlayerById(@PathVariable(name="id") Long id){
        return playerRepository.findById(id).orElseThrow();
    }

    @GetMapping("/dev-test/questions")
    public List<Question> getAllQuestion(){ return questionRepository.findAll();}

    @GetMapping("/dev-test/question/{id}")
    public Question getQuestionById(@PathVariable(name="id") Long id){return questionRepository.findById(id).orElseThrow();}

    @GetMapping("/dev-test/games")
    public List<Game> getAllGames(){return gameRepository.findAll();}

    @GetMapping("/dev-test/games/{id}")
    public Game getGameById(@PathVariable(name="id") Long id){ return gameRepository.findById(id).orElseThrow();}

    @GetMapping("/dev-test/users")
    public List<User> getAllUsers(){return userRepository.findAll();}

    @GetMapping("/dev-test/users/{id}")
    public User getUserById(@PathVariable(name="id") Long id){ return userRepository.findById(id).orElseThrow();}

    @GetMapping("/dev-test/rounds")
    public List<Round> getAllRounds(){return roundRepository.findAll();}

    @GetMapping("/dev-test/rounds/{id}")
    public Round getRoundById(@PathVariable(name="id") Long id){ return roundRepository.findById(id).orElseThrow();}

    @GetMapping("/dev-test/populate")
    public String populateDB(){
        for(Player player : playerRepository.findAll()){
            player.getGames().clear();
            player.setCurrentGame(null);
            playerRepository.save(player);
        }
        gameRepository.deleteAll();
        playerRepository.deleteAll();
        questionRepository.deleteAll();
        gameModeRepository.deleteAll();

        Player player1 =new Player.Builder()
                .alias("Dharmendra S Singh")
                .saltedHashedPassword("abcd1234")
                .email("ds1234@gmail.com")
                .build();
        playerRepository.save(player1);
        Player player2 =new Player.Builder()
                .alias("John & John")
                .saltedHashedPassword("john1234")
                .email("jn1234@gmail.com")
                .build();
        playerRepository.save(player2);
        GameMode isThisFact=new GameMode("IS THIS A FACT","https://assets3.thrillist.com/v1/image/2846512/size/gn-gift_guide_variable_c_2x.jpg",
                "IS THIS FACT");
        gameModeRepository.save(isThisFact);
        GameMode isThisBluff=new GameMode("WORD-UP","https://assets3.thrillist.com/v1/image/2846512/size/gn-gift_guide_variable_c_2x.jpg",
                "WORD-UP");
        gameModeRepository.save(isThisBluff);
        GameMode unScramble=new GameMode("UN-SCRAMBLE","https://assets3.thrillist.com/v1/image/2846512/size/gn-gift_guide_variable_c_2x.jpg",
                "UN-SCRAMBLE");
        gameModeRepository.save(unScramble);
        Game g1=new Game.Builder()
                .numRounds(10)
                .gameMode(isThisFact)
                .gameStatus(GameStatus.PLAYERS_JOINING)
                .hasEllen(false)
                .leader(player1)
                .build();
        gameRepository.save(g1);

        List<Question> questions=new ArrayList<>();
        for(Map.Entry<String ,String> fileMode : Constants.QA_FILES.entrySet()){
            //String fileName=fileMode.getKey();
            GameMode gameMode=gameModeRepository.findByName(fileMode.getValue()).orElseThrow();
            System.out.println("add" +fileMode);
            System.out.println("filepath :"+fileMode.getKey());
            System.out.println("1"+Utils.readQAFile((fileMode.getKey())));
            for(Pair<String ,String> questionAnswer : Utils.readQAFile(fileMode.getKey())){
                Question question=new Question.Builder().question(questionAnswer.getFirst()).
                        correctAnswer(questionAnswer.getSecond()).
                        gameMode(gameMode).build();
                questions.add(question);
            }
        }
        questionRepository.saveAll(questions);
        /*g1.getPlayers().add(player1);
        player1.setCurrentGame(g1);*/
         return "populated";
    }
}
