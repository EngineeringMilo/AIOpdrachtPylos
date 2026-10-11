package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

public class MiniMaxStudent extends PylosPlayer {

    int diepte;

    public MiniMaxStudent(int diepte) {
        this.diepte = diepte;
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        /* board methods
         * 	PylosLocation[] allLocations = board.getLocations();
         * 	PylosSphere[] allSpheres = board.getSpheres();
         * 	PylosSphere[] mySpheres = board.getSpheres(this);
         * 	PylosSphere myReserveSphere = board.getReserve(this); */

        /* game methods
         * game.moveSphere(myReserveSphere, allLocations[0]); */

        PylosGameSimulator gameSimulator = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);
        int bestValue = Integer.MIN_VALUE;
        PylosLocation bestLocation = null;
        PylosSphere myBall = board.getReserve(PLAYER_COLOR);
        for (PylosLocation loc: board.getLocations()){
            if(loc.isUsable()){
                PylosGameState prevState = gameSimulator.getState();
                PylosPlayerColor prevColor = gameSimulator.getColor();
                gameSimulator.moveSphere(myBall, loc);
                int value = minimax(diepte,gameSimulator.getColor() == PLAYER_COLOR, gameSimulator, board, Integer.MIN_VALUE, Integer.MAX_VALUE);
                gameSimulator.undoAddSphere(myBall,prevState,prevColor);
                if(value > bestValue){
                    bestValue = value;
                    bestLocation = loc;
                }
            }
        }
        if(bestLocation != null){
            game.moveSphere(myBall, bestLocation);
        }else{
            ArrayList<PylosLocation> usableLocations = new ArrayList<>();
            for(PylosLocation loc: board.getLocations()){
                if(loc.isUsable()){
                    usableLocations.add(loc);
                }
            }
            game.moveSphere(myBall, usableLocations.get(getRandom().nextInt(usableLocations.size())));
        }
    }

    private int minimax(int depth, boolean maximizingplayer, PylosGameSimulator simulator, PylosBoard board, int alpha, int beta){
        if (simulator.getWinner() == PLAYER_COLOR) return Integer.MAX_VALUE;
        if (simulator.getWinner() == PLAYER_COLOR.other()) return Integer.MIN_VALUE;
        if (depth <= 0 && simulator.getState() == PylosGameState.MOVE) return eval(board);

        PylosPlayerColor currentColor = simulator.getColor();

        if(simulator.getState() == PylosGameState.MOVE){
            PylosSphere ball = board.getReserve(currentColor);
            if(ball == null) return eval(board);

            if(maximizingplayer){
                int maxVal = Integer.MIN_VALUE;

                for(PylosLocation loc: board.getLocations()){
                    if(loc.isUsable()){
                        PylosGameState prevState = simulator.getState();
                        PylosPlayerColor prevColor = simulator.getColor();
                        simulator.moveSphere(ball, loc);
                        int value = minimax(depth-1, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                        simulator.undoAddSphere(ball, prevState, prevColor);
                        maxVal = Math.max(maxVal, value);
                        alpha = Math.max(alpha, value);
                        if (beta <= alpha) break;
                    }
                }
                return maxVal;
            }
            else {
                int minVal = Integer.MAX_VALUE;
                for(PylosLocation loc: board.getLocations()){
                    if(loc.isUsable()){
                        PylosGameState prevState = simulator.getState();
                        PylosPlayerColor prevColor = simulator.getColor();
                        simulator.moveSphere(ball, loc);
                        int value = minimax(depth-1, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                        simulator.undoAddSphere(ball, prevState, prevColor);
                        minVal = Math.min(minVal, value);
                        beta = Math.min(beta, value);
                        if (beta <= alpha) break;

                    }
                }
                return minVal;
            }
        }
        if(simulator.getState() == PylosGameState.REMOVE_FIRST){

            ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
            for(PylosSphere ball: board.getSpheres(currentColor)){
                if(ball.canRemove()){
                    removableSpheres.add(ball);
                }
            }
            if(removableSpheres.isEmpty()){
                PylosGameState prevState = simulator.getState();
                PylosPlayerColor prevColor = simulator.getColor();
                simulator.pass();
                int val = minimax(depth, maximizingplayer, simulator, board, alpha, beta);
                simulator.undoPass(prevState, prevColor);
                return val;
            }

            if(maximizingplayer){
                int maxVal = Integer.MIN_VALUE;
                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    //true want na remove_first in remove_second en dat is zelfde speler nog steeds
                    //zou denk ik ook gewoon met maximizinplayer en !maximizingplayer werken zou eens moeten bekijken ¯\_(ツ)_/¯
                    int value = minimax(depth, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                    simulator.undoRemoveFirstSphere(ball, prevLocation, prevState, prevColor);
                    maxVal = Math.max(maxVal, value);
                    alpha = Math.max(alpha, value);
                    if (beta <= alpha) break;
                }
                return maxVal;
            }else{
                int minVal = Integer.MAX_VALUE;

                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    int value = minimax(depth, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                    simulator.undoRemoveFirstSphere(ball, prevLocation, prevState, prevColor);
                    minVal = Math.min(minVal, value);
                    beta = Math.min(beta, value);
                    if (beta <= alpha) break;
                }
                return minVal;
            }
        }
        if(simulator.getState() == PylosGameState.REMOVE_SECOND){
            ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
            for(PylosSphere ball: board.getSpheres(currentColor)){
                if(ball.canRemove()){
                    removableSpheres.add(ball);
                }
            }
            if(removableSpheres.isEmpty()){
                PylosGameState prevState = simulator.getState();
                PylosPlayerColor prevColor = simulator.getColor();
                simulator.pass();
                int value = minimax(depth, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                simulator.undoPass(prevState, prevColor);
                return value;
            }
            if(maximizingplayer){
                int maxVal = Integer.MIN_VALUE;
                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    int value = minimax(depth, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                    simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);
                    maxVal = Math.max(maxVal, value);
                    alpha = Math.max(alpha, value);
                    if(beta <= alpha) break;
                }
                return maxVal;
            }else{
                int minVal = Integer.MAX_VALUE;
                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    int value = minimax(depth - 1, simulator.getColor() == PLAYER_COLOR, simulator, board, alpha, beta);
                    simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);
                    minVal = Math.min(minVal, value);
                    beta = Math.min(beta, value);
                    if(beta <= alpha) break;
                }
                return minVal;
            }
        }
        return (eval(board));
    }

    private int eval(PylosBoard board) {
        PylosPlayerColor me = PLAYER_COLOR;
        PylosPlayerColor opp = me.other();

        int score = 100 * (
                board.getReservesSize(me) - board.getReservesSize(opp)
        );

        for (PylosSphere sphere : board.getSpheres(me)) {
            if (!sphere.isReserve()) {
                score += sphere.getLocation().Z * 3;
            }
        }

        for (PylosSphere sphere : board.getSpheres(opp)) {
            if (!sphere.isReserve()) {
                score -= sphere.getLocation().Z * 3;
            }
        }

        return score;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator simulator = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);

        int bestValue = Integer.MIN_VALUE;
        PylosSphere bestSphere = null;

        for (PylosSphere ball : board.getSpheres(this)) {
            if (!ball.canRemove()) continue;

            PylosGameState prevState = simulator.getState();
            PylosPlayerColor prevColor = simulator.getColor();
            PylosLocation prevLocation = ball.getLocation();

            simulator.removeSphere(ball);
            // na REMOVE_FIRST is dezelfde speler nog aan de beurt (REMOVE_SECOND) -> maximizing blijft true
            int value = minimax(diepte, simulator.getColor() == PLAYER_COLOR, simulator, board, Integer.MIN_VALUE, Integer.MAX_VALUE);
            simulator.undoRemoveFirstSphere(ball, prevLocation, prevState, prevColor);

            if (bestSphere == null || value > bestValue) {
                bestValue = value;
                bestSphere = ball;
            }
        }

        game.removeSphere(bestSphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator simulator = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);

        //pass?
        PylosGameState passState = simulator.getState();
        PylosPlayerColor passColor = simulator.getColor();
        simulator.pass();
        int bestValue = minimax(diepte, simulator.getColor() == PLAYER_COLOR, simulator, board, Integer.MIN_VALUE, Integer.MAX_VALUE);
        simulator.undoPass(passState, passColor);

        PylosSphere bestSphere = null; // null = passen

        for (PylosSphere ball : board.getSpheres(this)) {
            if (!ball.canRemove()) continue;

            PylosGameState prevState = simulator.getState();
            PylosPlayerColor prevColor = simulator.getColor();
            PylosLocation prevLocation = ball.getLocation();

            simulator.removeSphere(ball);
            int value = minimax(diepte, simulator.getColor() == PLAYER_COLOR, simulator, board, Integer.MIN_VALUE, Integer.MAX_VALUE); // beurt gaat naar de tegenstander
            simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);

            if (value > bestValue) {
                bestValue = value;
                bestSphere = ball;
            }
        }

        if (bestSphere == null) {
            game.pass();
        } else {
            game.removeSphere(bestSphere);
        }
    }
}
