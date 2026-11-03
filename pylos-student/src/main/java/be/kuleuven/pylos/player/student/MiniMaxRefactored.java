package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiniMaxRefactored extends PylosPlayer {

    private static final int WIN = 1_000_000;
    private enum Kind { ADD, MOVE, REMOVE, PASS }

    private static final byte TT_EXACT = 0, TT_LOWER = 1, TT_UPPER = 2;

    /* ------------------------- transpositietabel (arrays) ------------------------- */

    private static final int TT_BITS = 21;                 // 2M entries, ~32 MB per speler
    private static final int TT_SIZE = 1 << TT_BITS;
    private final long[]  ttKey   = new long[TT_SIZE];
    private final int[]   ttValue = new int[TT_SIZE];
    private final byte[]  ttDepth = new byte[TT_SIZE];     // -1 = leeg slot
    private final byte[]  ttFlag  = new byte[TT_SIZE];
    private final short[] ttMove  = new short[TT_SIZE];    // beste zet als compacte code, -1 = geen

    /* ------------------------------ eval-cache (arrays) --------------------------- */

    private static final int EVAL_BITS = 20;
    private static final int EVAL_SIZE = 1 << EVAL_BITS;
    private final long[] evalKey = new long[EVAL_SIZE];    // -1 = leeg slot
    private final int[]  evalVal = new int[EVAL_SIZE];

    private static int idx(long key, int bits) {
        return (int) ((key * 0x9E3779B97F4A7C15L) >>> (64 - bits));
    }

    /* --------------------------------------------------------------------------- */

    private static int locCode(PylosLocation l) {
        return (l.Z << 4) | (l.Y << 2) | l.X;              // 6 bits
    }

    private static class Move {
        final Kind kind;
        final PylosSphere sphere;
        final PylosLocation to;
        final int code;          // compacte zetcode voor de TT: kind(2) | from(6) | to(6)
        // undo-info
        PylosGameState prevState;
        PylosPlayerColor prevColor;
        PylosLocation prevLocation;
        // ordering en iterative deepening
        int order;
        int score;

        Move(Kind kind, PylosSphere sphere, PylosLocation to) {
            this.kind = kind;
            this.sphere = sphere;
            this.to = to;
            int from = (kind == Kind.MOVE || kind == Kind.REMOVE) ? locCode(sphere.getLocation()) : 0;
            int t = to != null ? locCode(to) : 0;
            this.code = (kind.ordinal() << 12) | (from << 6) | t;
        }
    }

    private final int diepte;

    public MiniMaxRefactored(int diepte) {
        this.diepte = diepte;
        Arrays.fill(ttDepth, (byte) -1);
        Arrays.fill(ttMove, (short) -1);
        Arrays.fill(evalKey, -1L);
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

    /* ------------------------- root + iterative deepening ------------------------- */

    private Move choose(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);

        List<Move> rootMoves = generateMoves(sim, board);
        orderMoves(rootMoves, sim.getColor(), -1);          // startvolgorde voor iteratie 1

        if (rootMoves.size() == 1) return rootMoves.get(0); // enige zet, niet zoeken

        Move bestMove = rootMoves.get(0);

        for (int d = 1; d <= diepte; d++) {
            int bestValue = Integer.MIN_VALUE;
            Move iterationBest = null;

            for (Move m : rootMoves) {
                apply(m, sim);
                int value = minimax(childDepth(m, d), sim, board, bestValue, Integer.MAX_VALUE);
                undo(m, sim);

                m.score = value;
                if (iterationBest == null || value > bestValue) {
                    bestValue = value;
                    iterationBest = m;
                }
            }

            if(iterationBest != null){
                bestMove = iterationBest;
            }
            // beste zet vooraan voor de volgende iteratie (stabiele sortering)
            rootMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        }
        return bestMove;
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

    /* ---------------------------------- zoeken ------------------------------------ */

    private int minimax(int depth, PylosGameSimulator sim, PylosBoard board, int alpha, int beta) {
        if (sim.getState() == PylosGameState.COMPLETED) {
            boolean iWon = sim.getColor() != PLAYER_COLOR;
            return iWon ? WIN + depth : -WIN - depth;
        }
        if (depth <= 0 && sim.getState() == PylosGameState.MOVE) return eval(board, sim.getColor());

        long key = positionKey(board, sim);
        int slot = idx(key, TT_BITS);
        int hint = -1;

        if (ttDepth[slot] >= 0 && ttKey[slot] == key) {
            hint = ttMove[slot];                            // beste zet uit eerdere zoektocht
            if (ttDepth[slot] >= depth) {
                int v = ttValue[slot];
                switch (ttFlag[slot]) {
                    case TT_EXACT: return v;
                    case TT_LOWER: alpha = Math.max(alpha, v); break;
                    case TT_UPPER: beta = Math.min(beta, v); break;
                }
                if (alpha >= beta) return v;
            }
        }

        List<Move> moves = generateMoves(sim, board);
        if (moves.isEmpty()) {
            // geen zetten meer = eindwaarde, onafhankelijk van de diepte
            int v = eval(board, sim.getColor());
            store(key, 100, v, TT_EXACT, -1);
            return v;
        }

        orderMoves(moves, sim.getColor(), hint);

        boolean max = sim.getColor() == PLAYER_COLOR;
        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestCode = -1;
        int alphaOrig = alpha, betaOrig = beta;

        for (Move m : moves) {
            apply(m, sim);
            int v = minimax(childDepth(m, depth), sim, board, alpha, beta);
            undo(m, sim);

            if (max) {
                if (v > best) { best = v; bestCode = m.code; }
                alpha = Math.max(alpha, v);
            } else {
                if (v < best) { best = v; bestCode = m.code; }
                beta = Math.min(beta, v);
            }
            if (beta <= alpha) break;
        }

        // bound bepalen tov het originele zoekvenster (fail-soft)
        byte flag = best <= alphaOrig ? TT_UPPER : (best >= betaOrig ? TT_LOWER : TT_EXACT);
        store(key, depth, best, flag, bestCode);
        return best;
    }

    private void store(long key, int depth, int value, byte flag, int moveCode) {
        int slot = idx(key, TT_BITS);
        ttKey[slot] = key;
        ttValue[slot] = value;
        ttDepth[slot] = (byte) Math.min(depth, 100);
        ttFlag[slot] = flag;
        ttMove[slot] = (short) moveCode;
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

    private void orderMoves(List<Move> moves, PylosPlayerColor color, int ttHint) {
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
            if (m.code == ttHint) s += 10_000;   // beste zet uit de TT altijd eerst
            m.order = s;
        }
        moves.sort((a, b) -> Integer.compare(b.order, a.order));
    }

    // true als een bal van `color` op `loc` een vierkant van 4 eigen ballen vormt
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
        long key = (board.toLong() << 1) | toMove.ordinal();
        int slot = idx(key, EVAL_BITS);
        if (evalKey[slot] == key) return evalVal[slot];

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

        evalKey[slot] = key;
        evalVal[slot] = score;
        return score;
    }
}