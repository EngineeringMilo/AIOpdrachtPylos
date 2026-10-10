package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

public class MinMax extends PylosPlayer {


    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator gameSimulator = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);
        int bestValue = Integer.MIN_VALUE;
        PylosLocation bestLocation = null;
        PylosSphere bestSphere = null; //beste sphere onthoude die verplaatst moet worden

        //eerst kijken of er een sphere gemoved kan worden
        for (PylosSphere sphere1 : board.getSpheres(this)) {
            if (!sphere1.isReserve()) {
                for (PylosLocation loc : board.getLocations()) {
                    if (sphere1.canMoveTo(loc)) { // canMoveTo checkt of die sphere onder iets lig
                        PylosGameState prevState = gameSimulator.getState();
                        PylosPlayerColor prevColor = gameSimulator.getColor();
                        PylosLocation prevLoc = sphere1.getLocation(); //alles beware voor undo functie

                        gameSimulator.moveSphere(sphere1, loc);
                        int value = minimax(4, false, gameSimulator, board);
                        gameSimulator.undoMoveSphere(sphere1, prevLoc, prevState, prevColor);

                        if (value > bestValue) {
                            bestValue = value;
                            bestLocation = loc;
                            bestSphere = sphere1;
                        }
                    }
                }
            }
        }

        //kijken naar sphere resrves
        PylosSphere sphere2 = board.getReserve(this);
        if (sphere2 != null) {
            for (PylosLocation loc: board.getLocations()){
                if(loc.isUsable()){
                    PylosGameState prevState = gameSimulator.getState();
                    PylosPlayerColor prevColor = gameSimulator.getColor();

                    gameSimulator.moveSphere(sphere2, loc);
                    int value = minimax(4, false, gameSimulator, board);
                    gameSimulator.undoAddSphere(sphere2, prevState, prevColor);

                    if(value > bestValue){
                        bestValue = value;
                        bestLocation = loc;
                        bestSphere = sphere2;
                    }
                }
            }
        }

        if(bestLocation != null && bestSphere != null){
            game.moveSphere(bestSphere, bestLocation);
        } else {
            //idk moest er iets mis zijn ofz toch gwn random ma nrml geraak ik hier niet eens
            ArrayList<PylosLocation> usableLocations = new ArrayList<>();
            for(PylosLocation loc: board.getLocations()){
                if(loc.isUsable()){
                    usableLocations.add(loc);
                }
            }
            game.moveSphere(sphere2, usableLocations.get(getRandom().nextInt(usableLocations.size())));
        }
    }

    private int minimax(int depth,boolean maximizingplayer, PylosGameSimulator simulator, PylosBoard board){
        if (depth == 0) return eval(board);
        if (simulator.getWinner() == PLAYER_COLOR) return Integer.MAX_VALUE;
        if (simulator.getWinner() == PLAYER_COLOR.other()) return Integer.MIN_VALUE;

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
                        int value = minimax(depth-1, false, simulator, board);
                        simulator.undoAddSphere(ball, prevState, prevColor);
                        maxVal = Math.max(maxVal, value);
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
                        int value = minimax(depth-1, true, simulator, board);
                        simulator.undoAddSphere(ball, prevState, prevColor);
                        minVal = Math.min(minVal, value);

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
                int val = minimax(depth, maximizingplayer, simulator, board);
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
                    int value = minimax(depth, true, simulator, board);
                    simulator.undoRemoveFirstSphere(ball, prevLocation, prevState, prevColor);
                    maxVal = Math.max(maxVal, value);
                }
                return maxVal;
            }else{
                int minVal = Integer.MAX_VALUE;

                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    int value = minimax(depth, false, simulator, board);
                    simulator.undoRemoveFirstSphere(ball, prevLocation, prevState, prevColor);
                    minVal = Math.min(minVal, value);
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
                int value = minimax(depth, !maximizingplayer, simulator, board);
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
                    int value = minimax(depth, false, simulator, board);
                    simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);
                    maxVal = Math.max(maxVal, value);
                }
                return maxVal;
            }else{
                int minVal = Integer.MAX_VALUE;
                for(PylosSphere ball: removableSpheres){
                    PylosGameState prevState = simulator.getState();
                    PylosPlayerColor prevColor = simulator.getColor();
                    PylosLocation prevLocation = ball.getLocation();
                    simulator.removeSphere(ball);
                    int value = minimax(depth - 1,  true, simulator, board);
                    simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);
                    minVal = Math.min(minVal, value);
                }
                return minVal;
            }
        }
        return (eval(board));
    }

    private int eval(PylosBoard board) {

        PylosPlayerColor me = PLAYER_COLOR;
        PylosPlayerColor opp = PLAYER_COLOR.other();

        int score = 0;


        //mss weights met statics make dak da zo kan verandere vanbove in de functie ipv hier zo met weights te gaan spele
        //reservespheres
        score += (board.getReservesSize(me)-board.getReservesSize(opp))*50;

        //vierkanten bekijke, hoeveel hebk er van mij hoeveel van enemy zo score reducten of bijdoen
        for (PylosSquare square : board.getAllSquares()) {
            int myCount = square.getInSquare(me);
            int oppCount = square.getInSquare(opp);

            //gemengd, doek niks mee, late staan?
            if (myCount > 0 && oppCount > 0) {
                continue;
            }

            //mijn spheres in vierkant
            if (myCount == 3) {
                score += 30;
            } else if (myCount == 2) {
                score += 5;
            }

            //sphere vn opps in vierkant
            if (oppCount == 3) {
                score -= 30;
            } else if (oppCount == 2) {
                score -= 5;
            }
        }

        //spheres dat niet in de reserve liggen krijgen ook nog punte!
        for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR)){
            if(!sphere.isReserve()) {
                PylosLocation locatie = sphere.getLocation();
                score+=(locatie.Z * 5);

                //mss ook nog de bollen die vrijgespeeld kunne worde ofz als ze me 3 zijn?????? idfk
            }
        }

        for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR.other())) {
            if (!sphere.isReserve()) {
                PylosLocation loc = sphere.getLocation();
                score -= (loc.Z * 5);
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
            int value = minimax(4, true, simulator, board);
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
        int bestValue = minimax(4, false, simulator, board);
        simulator.undoPass(passState, passColor);

        PylosSphere bestSphere = null; // null = passen

        for (PylosSphere ball : board.getSpheres(this)) {
            if (!ball.canRemove()) continue;

            PylosGameState prevState = simulator.getState();
            PylosPlayerColor prevColor = simulator.getColor();
            PylosLocation prevLocation = ball.getLocation();

            simulator.removeSphere(ball);
            int value = minimax(4, false, simulator, board); // beurt gaat naar de tegenstander
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