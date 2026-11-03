package be.kuleuven.pylos.main;

import be.kuleuven.pylos.battle.Battle;
import be.kuleuven.pylos.battle.BattleMT;
import be.kuleuven.pylos.battle.BattleResult;
import be.kuleuven.pylos.battle.RoundRobin;
import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGame;
import be.kuleuven.pylos.game.PylosGameObserver;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerFactory;
import be.kuleuven.pylos.player.PylosPlayerObserver;
import be.kuleuven.pylos.player.PylosPlayerType;
import be.kuleuven.pylos.player.codes.PlayerFactoryCodes;
import be.kuleuven.pylos.player.codes.PylosPlayerBestFit;
import be.kuleuven.pylos.player.codes.PylosPlayerMiniMax;
import be.kuleuven.pylos.player.student.*;
import be.kuleuven.pylos.sprt.SPRT;
import be.kuleuven.pylos.tournament.Tournament;
import be.kuleuven.pylos.tournament.TournamentConfig;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class PylosMain {

    public static void main(String[] args) {
        /* !!! jvm argument !!! -ea */

//        startSPRT();
        startEloRoundRobinTournament();
//
//        startSingleGame();
//        startBattle();
//        startBattleMultithreaded();
//        startRoundRobinTournament();
    }

    public static void startSPRT() {
        PylosPlayerType newPlayer = new PylosPlayerType("MinimaxRefactored depth 9") {
            @Override
            public PylosPlayer create() {
                return new MiniMaxRefactored(6);
            }
        };
        PylosPlayerType basePlayer = new PylosPlayerType("MinimaxStudent") {
            @Override
            public PylosPlayer create() {
                return new MiniMaxStudent(6);
            }
        };

        SPRT.test(newPlayer, basePlayer, 0.0, 10.0);
    }

    public static void startEloRoundRobinTournament() {
        PylosPlayerFactory factory = new PlayerFactoryCodes();

        PlayerFactoryStudent fac = new PlayerFactoryStudent();

        fac.addAll(factory);

        TournamentConfig config = TournamentConfig.builder()
                .playerFactory(fac)
                .anchorPlayer("CODeS - Level 4", 1000.0)
                .build();
        Tournament.run(config);
    }

    public static void startSingleGame() {

        Random random = new Random(0);

        PylosPlayer playerLight = new PylosPlayerBestFit();
        PylosPlayer playerDark = new PylosPlayerMiniMax(2);

        PylosBoard pylosBoard = new PylosBoard();
        PylosGame pylosGame = new PylosGame(pylosBoard, playerLight, playerDark, random, PylosGameObserver.CONSOLE_GAME_OBSERVER, PylosPlayerObserver.NONE);

        pylosGame.play();
    }

    public static void startBattle() {
        int nRuns = 50;

        PylosPlayerType p1 = new PylosPlayerType("Wij") {
            @Override
            public PylosPlayer create() {
                return new MiniMaxRefactored(9);
            }
        };
        PylosPlayerType p2 = new PylosPlayerType("Zeger") {
            @Override
            public PylosPlayer create() {
                return new BigZ();
            }
        };

        Battle.play(p1, p2, nRuns);
    }

    public static void startBattleMultithreaded() {
        //Please refrain from using Collections.shuffle(List<?> list) in your player,
        //as this is not ideal for use across multiple threads.
        //Use Collections.shuffle(List<?> list, Random random) instead, with the Random object from the player (PylosPlayer.getRandom())

        int nRuns = 1000;
        int nThreads = 8;

        PylosPlayerType p1 = new PylosPlayerType("MiniMaxRefactored") {
            @Override
            public PylosPlayer create() {
                return new MiniMaxRefactored(7);
            }
        };
        PylosPlayerType p2 = new PylosPlayerType("CODES 4") {
            @Override
            public PylosPlayer create() {
                return new PylosPlayerMiniMax(4);
            }
        };

        BattleMT.play(p1, p2, nRuns, nThreads);
    }

    public static void startRoundRobinTournament() {
        //Same requirements apply as for startBattleMultithreaded()

        //Create your own PlayerFactory containing all PlayerTypes you want to test
        PlayerFactoryCodes pFactory = new PlayerFactoryCodes();
        //PlayerFactoryStudent pFactory = new PlayerFactoryStudent();

        int nRunsPerCombination = 1000;
        int nThreads = 8;

        Set<RoundRobin.Match> matches = RoundRobin.createTournament(pFactory);

        RoundRobin.play(matches, nRunsPerCombination, nThreads);

        List<BattleResult> results = matches.stream().map(c -> c.battleResult).collect(Collectors.toList());

        RoundRobin.printWinsMatrix(results, pFactory.getTypes());
    }
}
