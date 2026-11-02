package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniMaxStudent extends PylosPlayer {

    private static final int WIN = 1_000_000;
    private enum Kind { ADD, MOVE, REMOVE, PASS }

    //caching
    private static final int CACHE_MAX_ENTRIES = 1 << 20;

    private static final byte TT_EXACT = 0, TT_LOWER = 1, TT_UPPER = 2;

    private record TTEntry(long key, int depth, int value, byte flag){}

    // transpositietabel: zelfde positie (zelfde bord + zelfde game-state + zelfde speler) niet opnieuw doorzoeken
    private final Map<Long, TTEntry> transpositionTable = new HashMap<>();
    // eval hangt enkel af van de bordpositie + wie aan zet is -> memoizen
    private final Map<Long, Integer> evalCache = new HashMap<>();

    /* --------------------------------------------------------------------------- */

    private static class Move {
        final Kind kind;
        final PylosSphere sphere;
        final PylosLocation to;
        // undo-info
        PylosGameState prevState;
        PylosPlayerColor prevColor;
        PylosLocation prevLocation;
        // voor move ordering
        int order;

        Move(Kind kind, PylosSphere sphere, PylosLocation to) {
            this.kind = kind;
            this.sphere = sphere;
            this.to = to;
        }
    }

    private final int diepte;

    public MiniMaxStudent(int diepte) {
        this.diepte = diepte;
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    private Move choose(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);
        Move best = null;
        int bestValue = Integer.MIN_VALUE;

        List<Move> rootMoves = generateMoves(sim, board);
        orderMoves(rootMoves, sim.getColor());

        for (Move m : rootMoves) {
            apply(m, sim);
            int value = minimax(childDepth(m, diepte), sim, board, bestValue, Integer.MAX_VALUE);
            undo(m, sim);

            if (best == null || value > bestValue) {
                bestValue = value;
                best = m;
            }
        }
        return best;
    }

    private void execute(PylosGameIF game, Move m) {
        switch (m.kind) {
            case ADD:
            case MOVE:
                game.moveSphere(m.sphere, m.to);
                break;
            case REMOVE:
                game.removeSphere(m.sphere);
                break;
            case PASS:
                game.pass();
                break;
        }
    }

    private int minimax(int depth, PylosGameSimulator sim, PylosBoard board, int alpha, int beta) {
        if (sim.getState() == PylosGameState.COMPLETED) {
            boolean iWon = sim.getColor() != PLAYER_COLOR;
            return iWon ? WIN + depth : -WIN - depth;
        }
        if (depth <= 0 && sim.getState() == PylosGameState.MOVE) return eval(board, sim.getColor());

        long key = positionKey(board, sim);
        TTEntry entry = transpositionTable.get(key);
        if (entry != null && entry.key == key && entry.depth >= depth) {
            switch (entry.flag) {
                case TT_EXACT: return entry.value;
                case TT_LOWER: alpha = Math.max(alpha, entry.value); break;
                case TT_UPPER: beta = Math.min(beta, entry.value); break;
            }
            if (alpha >= beta) return entry.value;
        }

        List<Move> moves = generateMoves(sim, board);
        if (moves.isEmpty()) {
            // geen zetten meer = eindwaarde, onafhankelijk van de diepte
            int v = eval(board, sim.getColor());
            transpositionTable.put(key, new TTEntry(key, Integer.MAX_VALUE, v, TT_EXACT));
            return v;
        }

        // beste zetten eerst -> meer alpha-beta cutoffs
        orderMoves(moves, sim.getColor());

        boolean max = sim.getColor() == PLAYER_COLOR;
        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int alphaOrig = alpha, betaOrig = beta;

        for (Move m : moves) {
            apply(m, sim);
            int v = minimax(childDepth(m, depth), sim, board, alpha, beta);
            undo(m, sim);

            if (max) {
                best = Math.max(best, v);
                alpha = Math.max(alpha, v);
            } else {
                best = Math.min(best, v);
                beta = Math.min(beta, v);
            }
            if (beta <= alpha) break;
        }

        // bound bepalen tov het originele zoekvenster (fail-soft)
        byte flag = best <= alphaOrig ? TT_UPPER : (best >= betaOrig ? TT_LOWER : TT_EXACT);
        store(key, depth, best, flag);
        return best;
    }

    private void store(long key, int depth, int value, byte flag) {
        if (transpositionTable.size() >= CACHE_MAX_ENTRIES && !transpositionTable.containsKey(key)) {
            transpositionTable.clear(); // cache is enkel een optimalisatie, leeggooien is veilig
        }
        transpositionTable.put(key, new TTEntry(key, depth, value, flag));
    }

    private static long positionKey(PylosBoard board, PylosGameSimulator sim) {
        // 60 bits bord + 2 bits game-state + 1 bit kleur = unieke positie
        return (board.toLong() << 3)
                | ((long) sim.getState().ordinal() << 1)
                | sim.getColor().ordinal();
    }

    // diepte enkel verlagen bij een zet vanuit MOVE
    private int childDepth(Move m, int depth) {
        return m.prevState == PylosGameState.MOVE ? depth - 1 : depth;
    }

    /* ------------------------------ MOVE ORDERING ------------------------------ */

    private void orderMoves(List<Move> moves, PylosPlayerColor color) {
        for (Move m : moves) {
            int s;
            switch (m.kind) {
                case REMOVE:
                    s = 500 + m.sphere.getLocation().Z;
                    break;
                case MOVE:
                    s = m.to.Z > m.sphere.getLocation().Z ? 300 + m.to.Z : 0;
                    break;
                case ADD:
                    s = 100 + m.to.Z;
                    break;
                default: // PASS
                    s = 0;
            }

            if (m.to != null) {
                if (completesSquare(m.to, color)) {
                    s += 1000;   // eigen vierkant sluiten
                } else if (completesSquare(m.to, color.other())) {
                    s += 200;    // vierkant van tegenstander blokkeren
                }
            }
            m.order = s;
        }
        moves.sort((a, b) -> Integer.compare(b.order, a.order));
    }

    // Hulpfunctie op één plek: als maySquare in jouw API anders heet, pas je enkel dit aan.
    private static boolean completesSquare(PylosLocation loc, PylosPlayerColor color) {
        for (PylosSquare sq : loc.getSquares()) {
            if (sq.getInSquare(color) == 3 && sq.getInSquare(color.other()) == 0) {
                return true;
            }
        }
        return false;
    }

    /* --------------------------------------------------------------------------- */

    private List<Move> generateMoves(PylosGameSimulator sim, PylosBoard board) {
        List<Move> moves = new ArrayList<>(64);
        PylosPlayerColor color = sim.getColor();
        PylosGameState state = sim.getState();

        if (state == PylosGameState.MOVE) {
            PylosSphere reserve = board.getReserve(color);
            PylosSphere[] spheres = board.getSpheres(color);
            for (PylosLocation loc : board.getLocations()) {
                if (!loc.isUsable()) continue;
                if (reserve != null) moves.add(new Move(Kind.ADD, reserve, loc));
                for (PylosSphere s : spheres) {
                    if (!s.isReserve() && s.canMoveTo(loc)) {
                        moves.add(new Move(Kind.MOVE, s, loc));
                    }
                }
            }
        } else {
            if (state == PylosGameState.REMOVE_SECOND) {
                moves.add(new Move(Kind.PASS, null, null)); // tweede remove is optioneel
            }
            for (PylosSphere s : board.getSpheres(color)) {
                if (s.canRemove()) moves.add(new Move(Kind.REMOVE, s, null));
            }
        }
        return moves;
    }

    private void apply(Move m, PylosGameSimulator sim) {
        m.prevState = sim.getState();
        m.prevColor = sim.getColor();
        switch (m.kind) {
            case ADD:
                sim.moveSphere(m.sphere, m.to);
                break;
            case MOVE:
                m.prevLocation = m.sphere.getLocation();
                sim.moveSphere(m.sphere, m.to);
                break;
            case REMOVE:
                m.prevLocation = m.sphere.getLocation();
                sim.removeSphere(m.sphere);
                break;
            case PASS:
                sim.pass();
                break;
        }
    }

    private void undo(Move m, PylosGameSimulator sim) {
        switch (m.kind) {
            case ADD:
                sim.undoAddSphere(m.sphere, m.prevState, m.prevColor);
                break;
            case MOVE:
                sim.undoMoveSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                break;
            case REMOVE:
                if (m.prevState == PylosGameState.REMOVE_FIRST) {
                    sim.undoRemoveFirstSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                } else {
                    sim.undoRemoveSecondSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                }
                break;
            case PASS:
                sim.undoPass(m.prevState, m.prevColor);
                break;
        }
    }

    private int eval(PylosBoard board, PylosPlayerColor toMove) {
        // toMove zit in de key, anders krijg je verkeerde cache-hits
        long key = (board.toLong() << 1) | toMove.ordinal();
        Integer cached = evalCache.get(key);
        if (cached != null) return cached;

        PylosPlayerColor me = PLAYER_COLOR;
        PylosPlayerColor opp = PLAYER_COLOR.other();

        int score = 0;

        // reserve-ballen
        score += (board.getReservesSize(me) - board.getReservesSize(opp)) * 50;

        // tempo: de speler die aan zet is gaat nog een bal uitgeven
        if (toMove != PLAYER_COLOR) score += 50;

        // vierkanten: enkel tellen als ze niet gemengd zijn
        for (PylosSquare square : board.getAllSquares()) {
            int myCount = square.getInSquare(me);
            int oppCount = square.getInSquare(opp);

            if (myCount > 0 && oppCount > 0) {
                continue;
            }

            if (myCount == 3) {
                score += 30;
            } else if (myCount == 2) {
                score += 5;
            }

            if (oppCount == 3) {
                score -= 30;
            } else if (oppCount == 2) {
                score -= 5;
            }
        }

        // ballen op het bord krijgen punten naargelang hun niveau
        for (PylosSphere sphere : board.getSpheres(me)) {
            if (!sphere.isReserve()) {
                score += sphere.getLocation().Z * 5;
            }
        }

        for (PylosSphere sphere : board.getSpheres(opp)) {
            if (!sphere.isReserve()) {
                score -= sphere.getLocation().Z * 5;
            }
        }

        evalCache.put(key, score);
        return score;
    }
}