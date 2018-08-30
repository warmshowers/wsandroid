package fi.bitrite.android.ws.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osmdroid.util.BoundingBox;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class LoadedAreaTest {

    private static class ComparableBoundingBox {
        private final BoundingBox mBoundingBox;

        ComparableBoundingBox(BoundingBox boundingBox) {
            mBoundingBox = boundingBox;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            BoundingBox bb = null;
            if (other instanceof BoundingBox) {
                bb = (BoundingBox) other;
            } else if (other instanceof ComparableBoundingBox) {
                bb = ((ComparableBoundingBox) other).mBoundingBox;
            }
            return bb != null
                    && mBoundingBox.getLatNorth() == bb.getLatNorth()
                    && mBoundingBox.getLonEast() == bb.getLonEast()
                    && mBoundingBox.getLatSouth() == bb.getLatSouth()
                    && mBoundingBox.getLonWest() == bb.getLonWest();
        }
    }

    private static void subtractAndCheckEqual(List<BoundingBox> loadedAreas, BoundingBox query,
                                              List<BoundingBox> expectedResult) {
        LoadedArea la = new LoadedArea();
        for (BoundingBox rect : loadedAreas) {
            la.addLoadedArea(rect);
        }
        subtractAndCheckEqual(la, query, expectedResult);
    }
    private static void subtractAndCheckEqual(LoadedArea la, BoundingBox query,
                                              List<BoundingBox> expectedResult) {
        List<BoundingBox> result = la.subtractLoadedAreas(query);

        assertThat(result.size()).isEqualTo(expectedResult.size());

        List<ComparableBoundingBox> expectedResultComparable = new LinkedList<>();
        for (BoundingBox expected : expectedResult) {
            expectedResultComparable.add(new ComparableBoundingBox(expected));
        }
        for (BoundingBox r : result) {
            expectedResultComparable.remove(new ComparableBoundingBox(r));
        }
        if (!expectedResultComparable.isEmpty()) {
            System.out.println("Query: " + getRectStr(query));
            System.out.println("Result rects:");
            for (BoundingBox r : result) {
                System.out.println(" - " + getRectStr(r));
            }
            System.out.println("Remaining expected rects:");
            for (ComparableBoundingBox cb : expectedResultComparable) {
                System.out.println(" - " + getRectStr(cb.mBoundingBox));
            }
            System.out.println();

            assertThat(false).isTrue();
        }
    }
    private static List<BoundingBox> loaded(BoundingBox... loadedAreas) {
        return Arrays.asList(loadedAreas);
    }
    private static List<BoundingBox> expected(BoundingBox... loadedAreas) {
        return Arrays.asList(loadedAreas);
    }
    private static String getRectStr(BoundingBox r) {
        return String.format("%.5f,%.5f, %.5f,%.5f",
                r.getLonWest(), r.getLatSouth(), r.getLonEast(), r.getLatNorth());
    }

    // Translation from (left,bottom,right,top) to (north,east,south,west).
    private static BoundingBox r(double left, double bottom, double right, double top) {
        return new BoundingBox(top, right, bottom, left);
    }
    private static BoundingBox query(double left, double bottom, double right, double top) {
        return r(left, bottom, right, top);
    }

    // Overlapped is always in terms of the loaded area.

    @Test
    public void fullyOverlappedTest() {
        // l -----l
        // | q--q |
        // | |  | |
        // | q--q |
        // l------l
        subtractAndCheckEqual(
                loaded(
                        r(0,1, 10,11)
                ),
                r(0,1, 10,11),
                expected()); // Just touching.
        subtractAndCheckEqual(
                loaded(
                        r(0,0, 5,5),
                        r(0,5, 5,10),
                        r(5,0,10,5),
                        r(5,5,10,10)
                ),
                query(0,0, 10,10),
                expected()); // Just touching by four non-overlapping areas.
        subtractAndCheckEqual(
                loaded(
                        r(0,0, 5,5),
                        r(0,5, 5,10),
                        r(5,0,10,5),
                        r(5,5,10,10)
                ),
                query(1,1, 9,9),
                expected());
    }

    @Test
    public void nonOverlappingTest() {
        // l----l  q--q
        // |    |  |  |
        // l----l  q--q
        subtractAndCheckEqual(
                loaded(
                        r(0,0, 10,10), // left of
                        r(0,10, 20,20), // top of
                        r(20,0, 30,15), // right of
                        r(-10,-10, 20,0) // bottom of
                ),
                query(10,0, 20,10),
                expected(
                        r(10,0, 20,10)
                )
        );
    }

    @Test
    public void leftSideOverlappingTest() {
        // l---l
        // | q-+---q
        // | | | B |
        // | q-+---q
        // l---l
        subtractAndCheckEqual(
                loaded(
                        r(0,1, 10,11)
                ),
                query(2,2, 12,8),
                expected(
                        r(10,2, 12,8) // B
                )
        );
    }

    @Test
    public void topSideOverlappingTest() {
        // l-------l
        // | q---q |
        // l-+---+-l
        //   | C |
        //   q---q
        subtractAndCheckEqual(
                loaded(
                        r(0,10, 11,20)
                ),
                query(2,4, 7,14),
                expected(
                        r(2,4, 7,10) // C
                )
        );
    }

    @Test
    public void rightSideOverlappingTest() {
        //     l---l
        // q---+-q |
        // | D | | |
        // q---+-q |
        //     l---l
        subtractAndCheckEqual(
                loaded(
                        r(10,0, 20,11)
                ),
                query(1,5, 12,8),
                expected(
                        r(1,5, 10,8) // D
                )
        );
    }
    @Test
    public void bottomSideOverlappingTest() {
        //   q---q
        //   | A |
        // l-+---+-l
        // | q---q |
        // l-------l
        subtractAndCheckEqual(
                loaded(
                        r(-5,-10, 5,2)
                ),
                query(-2,-3, 4,6),
                expected(
                        r(-2,2, 4,6) // A
                )
        );
    }

    @Test
    public void centerOverlappingTest() {
        // q ----------q
        // |   : A :   |
        // |   l---l   |
        // | D |   | B |
        // |   l---l   |
        // |   : C :   |
        // q-----------q
        subtractAndCheckEqual(
                loaded(
                        r(10,11, 15,16)
                ),
                query(0,1, 20,21),
                expected(
                        r(10,16, 15,21), // A
                        r(15,1, 20,21), // B
                        r(10,1, 15,11), // C
                        r(0,1, 10,21) // D
                ));
    }

    @Test
    public void topLeftCornerOverlappingTest() {
        // l-----l
        // | q---+---q
        // | |   |   |
        // l-+---l B |
        //   | C :   |
        //   q-------q
        subtractAndCheckEqual(
                loaded(
                        r(0,6, 10,16)
                ),
                query(5,1, 15,11),
                expected(
                        r(10,1, 15,11), // B
                        r(5,1, 10,6) // C
                ));
    }

    @Test
    public void topRightCornerOverlappingTest() {
        //     l-----l
        // q---+---q |
        // |   |   | |
        // | D l---+-l
        // |   : C |
        // q-------q
        subtractAndCheckEqual(
                loaded(
                        r(5,6, 15,16)
                ),
                query(0,1, 10,11),
                expected(
                        r(5,1, 10,6), // C
                        r(0,1, 5,11) // D
                ));
    }

    @Test
    public void bottomRightCornerOverlappingTest() {
        // q-------q
        // |   : A |
        // | D l---+-l
        // |   |   | |
        // q---+---q |
        //     l-----l
        subtractAndCheckEqual(
                loaded(
                        r(5,1, 15,11)
                ),
                query(0,6, 10,16),
                expected(
                        r(5,11, 10,16), // A
                        r(0,6, 5,16) // D
                ));
    }

    @Test
    public void bottomLeftCornerOverlappingTest() {
        //   q-------q
        //   | A :   |
        // l-+---l   |
        // | |   | B |
        // | q---+---q
        // l-----l
        subtractAndCheckEqual(
                loaded(
                        r(0,1, 10,11)
                ),
                query(5,6, 15,16),
                expected(
                        r(5,11, 10,16), // A
                        r(10,6, 15,16) // B
                ));
    }

    @Test
    public void horizontalBarFullyOverlappingTest() {
        //   q---q
        //   | A |
        // l-+---+-l
        // | |   | |
        // l-+---+-l
        //   | C |
        //   q---q
        subtractAndCheckEqual(
                loaded(
                        r(0,1, 10,11)
                ),
                query(5,-10, 7,20),
                expected(
                        r(5,11, 7,20), // A
                        r(5,-10, 7,1) // C
                ));
    }

    @Test
    public void horizontalBarPartiallyOverlappingLeftTest() {
        //   q-------q
        //   | A :   |
        // l-+---l   |
        // | |   | B |
        // l-+---l   |
        //   | C :   |
        //   q-------q
        subtractAndCheckEqual(
                loaded(
                        r(0,5, 10,11)
                ),
                query(6,1, 12,20),
                expected(
                        r(6,11, 10,20), // A
                        r(10,1, 12,20), // B
                        r(6,1, 10,5) // C
                ));
    }

    @Test
    public void horizontalBarPartiallyOverlappingRightTest() {
        // q-------q
        // |   : A |
        // |   l---+-l
        // | D |   | |
        // |   l---+-l
        // |   : C |
        // q-------q
        subtractAndCheckEqual(
                loaded(
                        r(3,5, 15,10)
                ),
                query(1,2, 12,20),
                expected(
                        r(3,10, 12,20), // A
                        r(3,2, 12,5), // C
                        r(1,2, 3,20) // D
                ));
    }

    @Test
    public void verticalBarFullyOverlappingTest() {
        //     l--l
        // q---+--+---q
        // | D |  | B |
        // q---+--+---q
        //     l--l
        subtractAndCheckEqual(
                loaded(
                        r(0,1, 10,11)
                ),
                query(-10,3, 13,6),
                expected(
                        r(10,3, 13,6), // B
                        r(-10,3, 0,6) // D
                ));
    }

    @Test
    public void verticalBarPartiallyOverlappingTopTest() {
        //     l---l
        // q---+---+---q
        // | D l---l B |
        // |   : C :   |
        // q ----------q
        subtractAndCheckEqual(
                loaded(
                        r(4,5, 8,16)
                ),
                query(0,1, 20,7),
                expected(
                        r(8,1, 20,7), // B
                        r(4,1, 8,5), // C
                        r(0,1, 4,7) // D
                ));
    }

    @Test
    public void verticalBarPartiallyOverlappingBottomTest() {
        // q ----------q
        // |   : A :   |
        // | D l---l B |
        // q---+---+---q
        //     l---l
        subtractAndCheckEqual(
                loaded(
                        r(4,1, 8,7)
                ),
                query(0,5, 20,16),
                expected(
                        r(4,7, 8,16), // A
                        r(8,5, 20,16), // B
                        r(0,5, 4,16) // D
                ));
    }
}
