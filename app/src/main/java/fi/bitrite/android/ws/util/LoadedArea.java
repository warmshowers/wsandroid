package fi.bitrite.android.ws.util;

import org.osmdroid.util.BoundingBox;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Allows for queries to substract the loaded areas from a given query area that should be loaded.
 * This is done by running over a list of loaded areas, comparing each with the query area, dividing
 * the latter upon an overlap.
 *
 * If this ever became too slow we could be moving to some more sophisticated data structure such as
 * a segment tree.
 */
public class LoadedArea {
    private final static double ROUNDING_PRECISION = 0.0025; // ~300m

    // Keeps the loaded areas sorted by their left x-value. This allows one to only look at the
    // ones in this set that have a smaller left x-value than the right x-value of the search box.
    private final SortedSet<BoundingBox> mLoadedAreas = new TreeSet<>(mBoundingBoxComparator);

    public void addLoadedArea(BoundingBox rect) {
        // The loaded rect is expected to be previously pushed through subtractLoadedAreas. So it is
        // safe to round it up.
        roundUpRect(rect);
        synchronized (mLoadedAreas) {
            mLoadedAreas.add(rect);
        }
    }

    private void addRect(List<BoundingBox> list, BoundingBox rect) {
        if (rect.getDiagonalLengthInMeters() > 0.0d) {
            list.add(rect);
        }
    }
    public List<BoundingBox> subtractLoadedAreas(BoundingBox query) {
        // Make the query rect bigger.
        roundUpRect(query);

        LinkedList<BoundingBox> ret = new LinkedList<>();
        ret.add(query);
        synchronized (mLoadedAreas) {
            for (BoundingBox loadedRect : mLoadedAreas) {
                final double ln = loadedRect.getLatNorth();
                final double le = loadedRect.getLonEast();
                final double ls = loadedRect.getLatSouth();
                final double lw = loadedRect.getLonWest();

                if (ret.isEmpty()) {
                    // Completely overlapped.
                    break;
                }

                if (lw >= query.getLonEast()) { // To the right
                    // None of the following rects will intersect ours.
                    break;
                }
                if (le <= query.getLonWest() // To the left
                    || ls >= query.getLatNorth() // To the top
                    || ln <= query.getLatSouth()) { // To the bottom
                    continue;
                }

                // Search for intersections.
                LinkedList<BoundingBox> nextRet = new LinkedList<>();
                for (BoundingBox querySubRect : ret) {
                    final double qn = querySubRect.getLatNorth();
                    final double qe = querySubRect.getLonEast();
                    final double qs = querySubRect.getLatSouth();
                    final double qw = querySubRect.getLonWest();

                    final Overlap xOverlap = getOverlap(lw, le, qw, qe);
                    final Overlap yOverlap = getOverlap(ls, ln, qs, qn);
                    if (xOverlap == Overlap.NONE || yOverlap == Overlap.NONE) {
                        nextRet.add(querySubRect);
                        continue;
                    }

                    switch (xOverlap) {
                        case OUTSIDE:
                            switch (yOverlap) {
                                case OUTSIDE:
                                    // l -----l
                                    // | q--q |
                                    // | |  | |
                                    // | q--q |
                                    // l------l
                                    /* We just remove the query and do nothing else */
                                    break;

                                case INSIDE:
                                    //   q---q
                                    //   | A |
                                    // l-+---+-l
                                    // | |   | |
                                    // l-+---+-l
                                    //   | C |
                                    //   q---q
                                    addRect(nextRet, new BoundingBox(qn, qe, ln, qw)); // A
                                    addRect(nextRet, new BoundingBox(ls, qe, qs, qw)); // C
                                    break;

                                case LEFT_OR_BOTTOM: // BOTTOM
                                    //   q---q
                                    //   | A |
                                    // l-+---+-l
                                    // | q---q |
                                    // l-------l
                                    addRect(nextRet, new BoundingBox(qn, qe, ln, qw)); // A
                                    break;

                                case RIGHT_OR_TOP: // TOP
                                    // l-------l
                                    // | q---q |
                                    // l-+---+-l
                                    //   | C |
                                    //   q---q
                                    addRect(nextRet, new BoundingBox(ls, qe, qs, qw)); // C
                                    break;
                            }
                            break;

                        case INSIDE:
                            switch (yOverlap) {
                                case OUTSIDE:
                                    //     l--l
                                    // q---+--+---q
                                    // | D |  | B |
                                    // q---+--+---q
                                    //     l--l
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case INSIDE:
                                    // q ----------q
                                    // |   : A :   |
                                    // |   l---l   |
                                    // | D |   | B |
                                    // |   l---l   |
                                    // |   : C :   |
                                    // q-----------q
                                    addRect(nextRet, new BoundingBox(qn, le, ln, lw)); // A
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(ls, le, qs, lw)); // C
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case LEFT_OR_BOTTOM: // BOTTOM
                                    // q ----------q
                                    // |   : A :   |
                                    // | D l---l B |
                                    // q---+---+---q
                                    //     l---l
                                    addRect(nextRet, new BoundingBox(qn, le, ln, lw)); // A
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case RIGHT_OR_TOP: // TOP
                                    //     l---l
                                    // q---+---+---q
                                    // | D l---l B |
                                    // |   : C :   |
                                    // q ----------q
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(ls, le, qs, lw)); // C
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;
                            }
                            break;

                        case LEFT_OR_BOTTOM: // LEFT
                            switch (yOverlap) {
                                case OUTSIDE:
                                    // l---l
                                    // | q-+---q
                                    // | | | B |
                                    // | q-+---q
                                    // l---l
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    break;

                                case INSIDE:
                                    //   q-------q
                                    //   | A :   |
                                    // l-+---l   |
                                    // | |   | B |
                                    // l-+---l   |
                                    //   | C :   |
                                    //   q-------q
                                    addRect(nextRet, new BoundingBox(qn, le, ln, qw)); // A
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(ls, le, qs, qw)); // C
                                    break;

                                case LEFT_OR_BOTTOM: // BOTTOM
                                    //   q-------q
                                    //   | A :   |
                                    // l-+---l   |
                                    // | |   | B |
                                    // | q---+---q
                                    // l-----l
                                    addRect(nextRet, new BoundingBox(qn, le, ln, qw)); // A
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    break;

                                case RIGHT_OR_TOP: // TOP
                                    // l-----l
                                    // | q---+---q
                                    // | |   |   |
                                    // l-+---l B |
                                    //   | C :   |
                                    //   q-------q
                                    addRect(nextRet, new BoundingBox(qn, qe, qs, le)); // B
                                    addRect(nextRet, new BoundingBox(ls, le, qs, qw)); // C
                                    break;
                            }
                            break;

                        case RIGHT_OR_TOP: // RIGHT
                            switch (yOverlap) {
                                case OUTSIDE:
                                    //     l---l
                                    // q---+-q |
                                    // | D | | |
                                    // q---+-q |
                                    //     l---l
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case INSIDE:
                                    // q-------q
                                    // |   : A |
                                    // |   l---+-l
                                    // | D |   | |
                                    // |   l---+-l
                                    // |   : C |
                                    // q-------q
                                    addRect(nextRet, new BoundingBox(qn, qe, ln, lw)); // A
                                    addRect(nextRet, new BoundingBox(ls, qe, qs, lw)); // C
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case LEFT_OR_BOTTOM: // BOTTOM
                                    // q-------q
                                    // |   : A |
                                    // | D l---+-l
                                    // |   |   | |
                                    // q---+---q |
                                    //     l-----l
                                    addRect(nextRet, new BoundingBox(qn, qe, ln, lw)); // A
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;

                                case RIGHT_OR_TOP: // TOP
                                    //     l-----l
                                    // q---+---q |
                                    // |   |   | |
                                    // | D l---+-l
                                    // |   : C |
                                    // q-------q
                                    addRect(nextRet, new BoundingBox(ls, qe, qs, lw)); // C
                                    addRect(nextRet, new BoundingBox(qn, lw, qs, qw)); // D
                                    break;
                            }
                            break;
                    }
                }
                ret = nextRet;
            }
        }
        return ret;
    }

    /**
     * Rounds up the coordinates of the given rect to avoid very small (non)overlaps.
     */
    private static void roundUpRect(BoundingBox rect) {
        rect.set(
                roundUp(rect.getLatNorth()),
                roundUp(rect.getLonEast()),
                roundUp(rect.getLatSouth()),
                roundUp(rect.getLonWest()));
    }
    private static double roundUp(double i) {
        int sign = i < 0 ? -1 : 1;
        return Math.ceil(Math.abs(i) / ROUNDING_PRECISION) * ROUNDING_PRECISION * sign;
    }

    // As seen by 'a'. The intersection of 'a' is favored to be NONE or OUTSIDE in case values are
    // equal. INSIDE is never favored.
    private static Overlap getOverlap(double aLow, double aHigh, double bLow, double bHigh) {
        if (aLow <= bLow) {
            if (aHigh <= bLow) {
                return Overlap.NONE;
            } else if (aHigh >= bHigh) {
                return Overlap.OUTSIDE;
            } else {
                return Overlap.LEFT_OR_BOTTOM;
            }
        } else {
            if (aLow >= bHigh) {
                return Overlap.NONE;
            } else if (aHigh < bHigh) {
                return Overlap.INSIDE;
            } else {
                return Overlap.RIGHT_OR_TOP;
            }
        }
    }

    // Sorts the rects by their left-top corner from left-top to right-bottom.
    private final static Comparator<BoundingBox> mBoundingBoxComparator = (left, right) -> {
        double diff = left.getLonWest() - right.getLonWest();
        if (diff == 0.0d) {
            diff = left.getLatNorth() - right.getLatNorth();
        }
        return diff == 0 ? 0 : (diff < 0 ? -1 : 1); // Avoid rounding errors.
    };

    private enum Overlap { // Overlap of 'a' with 'b'. What is 'a'?
        NONE,           // a--a b==b   or   b==b a--a
        OUTSIDE,        // a-- b==b --a
        INSIDE,         // b== a--a ==b
        LEFT_OR_BOTTOM, // a-- b== --a ==b
        RIGHT_OR_TOP    // b== a-- ==b --a
    }
}
