package blake.bot.suppliers;

import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;

//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertNull;


//@RunWith(JUnit4.class)
public class MutualSupportSupplierTest {

    public static final String VIENNA_ARMY = "VIEAMY";
    public static final String BUDAMY = "BUDAMY";
    public static final String TYRAMY = "TYRAMY";

//    @Test
//    public void generateMutualSupport() {
//        DiplomacyGameBuilder gameBuilder = new DiplomacyGameBuilder();
//        gameBuilder.setPhase(Phase.FAL, 1903);
//
//        gameBuilder.placeUnit("FRA", VIENNA_ARMY);
//        gameBuilder.placeUnit("ITA", BUDAMY);
//        gameBuilder.placeUnit("ITA", TYRAMY);
//        gameBuilder.placeUnit("ENG", "BOHAMY");
//        gameBuilder.placeUnit("ENG", "GALAMY");
////        gameBuilder.
//        Game myGame = gameBuilder.createMyGame();
//
//        MutualSupportSupplier supportSupplier = new MutualSupportSupplier(
//                Collections.emptyList(),
//                new DBraneTactics(),
//                myGame,
//                myGame.getPower("FRA"),
//                Collections.singletonList(myGame.getPower("ITA")));
//        OrderBuilder orderBuilder = new OrderBuilder(myGame);
//        final BasicDeal firstCall = supportSupplier.generateMutualSupport();
//        System.out.println(firstCall.getOrderCommitments());
//        assertArrayEquals(Arrays.stream(new OrderCommitment[]{
//                        newCommitment(myGame,
//                                orderBuilder.support(myGame.getRegion(VIENNA_ARMY),
//                                        orderBuilder.hold(myGame.getRegion(TYRAMY)))),
//                        newCommitment(myGame,
//                                orderBuilder.support(myGame.getRegion(TYRAMY),
//                                        orderBuilder.hold(myGame.getRegion(VIENNA_ARMY))))
//                }).map(OrderCommitment::toString).toArray(),
//                firstCall.getOrderCommitments().stream().map(OrderCommitment::toString).toArray());
//        assertArrayEquals(Arrays.stream(new OrderCommitment[]{
//                        newCommitment(myGame,
//                                orderBuilder.support(myGame.getRegion(VIENNA_ARMY),
//                                        orderBuilder.hold(myGame.getRegion(BUDAMY)))),
//                        newCommitment(myGame,
//                                orderBuilder.support(myGame.getRegion(BUDAMY),
//                                        orderBuilder.hold(myGame.getRegion(VIENNA_ARMY))))
//                }).map(OrderCommitment::toString).toArray(),
//                supportSupplier.generateMutualSupport().getOrderCommitments().stream().map(OrderCommitment::toString).toArray());
//        assertNull(supportSupplier.generateMutualSupport());
//    }

    private OrderCommitment newCommitment(Game myGame, Order order) {
        return new OrderCommitment(myGame.getYear(), myGame.getPhase(), order);
    }

    private class OrderBuilder {
        private final Game game;

        public OrderBuilder(Game game) {

            this.game = game;
        }

        public HLDOrder hold(Region region) {
            return new HLDOrder(game.getController(region.getProvince()), region);
        }

        public Order support(Region region, Order order) {
            return new SUPOrder(this.game.getController(region.getProvince()), region, order);
        }
    }
}