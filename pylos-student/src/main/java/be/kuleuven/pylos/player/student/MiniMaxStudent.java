    package be.kuleuven.pylos.player.student;

    import be.kuleuven.pylos.game.*;
    import be.kuleuven.pylos.player.PylosPlayer;

    import java.util.ArrayList;

    public class MiniMaxStudent extends PylosPlayer {

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
                    int value = minimax(3,false, gameSimulator, board);
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

        private int minimax(int depth, boolean maximizingplayer, PylosGameSimulator simulator, PylosBoard board){
            if (simulator.getWinner() == PLAYER_COLOR) return Integer.MAX_VALUE;
            if (simulator.getWinner() == PLAYER_COLOR.other()) return Integer.MIN_VALUE;
            if (depth == 0) return eval(board);

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
                        int value = minimax(depth - 1, true, simulator, board);
                        simulator.undoRemoveSecondSphere(ball, prevLocation, prevState, prevColor);
                        minVal = Math.min(minVal, value);
                    }
                    return minVal;
                }
            }
            return (eval(board));
        }

        private int eval(PylosBoard board) {
//            int BestBallLoc = 0;
//            PylosSphere BestBall = null;
//            for (PylosSphere ball: board.getSpheres()){
//                if(ball.getLocation().Z > BestBallLoc){
//                    BestBall = ball;
//                    BestBallLoc = ball.getLocation().Z;
//                }
//            }
            return board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other());
        }

        @Override
        public void doRemove(PylosGameIF game, PylosBoard board) {
            ArrayList<PylosSphere> removableSpeheres = new ArrayList<>();
            for (PylosSphere ps: board.getSpheres(PLAYER_COLOR)){
                if(ps.canRemove()) removableSpeheres.add(ps);
            }
            game.removeSphere(removableSpeheres.get(getRandom().nextInt(removableSpeheres.size())));
        }

        @Override
        public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
            ArrayList<PylosSphere> removableSpeheres = new ArrayList<>();
            for (PylosSphere ps: board.getSpheres(PLAYER_COLOR)){
                if(ps.canRemove()) removableSpeheres.add(ps);
            }
            if(removableSpeheres.isEmpty()) {
                game.pass();
            }else{
               game.removeSphere(removableSpeheres.get(getRandom().nextInt(removableSpeheres.size())));
            }
        }
    }
