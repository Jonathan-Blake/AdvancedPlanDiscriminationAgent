package blake.bot.analyser;

import blake.bot.utility.DatedObject;
import blake.bot.utility.Utility;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.gameBuilder.DiplomacyGameBuilder;
import ddejonge.bandana.internalAdjudicator.InternalAdjudicator;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.*;
import es.csic.iiia.fabregues.dip.orders.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvancedAdjudicator {
    private static final Map<String, DBraneTactics> tacticsMap = new HashMap<>();
    private static final Random random = new Random();
    private static final InternalAdjudicator adjudicator = new InternalAdjudicator();
    private static int totalOrdersGuessed = 0;
    private static int totalOrdersExcepted = 0;

    static {
        tacticsMap.put("ENG", new DBraneTactics());
        tacticsMap.put("FRA", new DBraneTactics());
        tacticsMap.put("GER", new DBraneTactics());
        tacticsMap.put("RUS", new DBraneTactics());
        tacticsMap.put("ITA", new DBraneTactics());
        tacticsMap.put("AUS", new DBraneTactics());
        tacticsMap.put("TUR", new DBraneTactics());
    }

    private AdvancedAdjudicator() {
    }

    public static Game determineOutcome(Game currentGame, List<Order> moves) {
        adjudicator.clear();
        List<Region> allUnits = currentGame.getNonDeadPowers().stream()
                .flatMap(power -> power.getControlledRegions().stream())
                .collect(Collectors.toList());
        adjudicator.resolve(currentGame, moves);
        DiplomacyGameBuilder gameBuilder = new DiplomacyGameBuilder();
        DatedObject nextTurn = Utility.Dates.getNextDate(new DatedObject(currentGame));
        gameBuilder.setPhase(nextTurn.getPhase(), nextTurn.getYear());
        List<Region> setDestinations = new ArrayList<>(34);
        List<Order> failedOrders = new ArrayList<>(34);
        final boolean isAut = nextTurn.getPhase() == Phase.AUT;
        moves.forEach(
                move -> {
                    if (adjudicator.getResult(move)) {
                        resolveSuccessfulOrder(gameBuilder, setDestinations, move, isAut);
                    } else {
                        failedOrders.add(move);
                    }
                    allUnits.remove(move.getLocation());
                }
        );
        List<Dislodgement> dislodgements = new ArrayList<>(34);
        failedOrders.forEach(order -> resolveFailedOrder(gameBuilder, setDestinations, dislodgements, order, isAut));
        allUnits.forEach(region -> {
            final Power controller = currentGame.getController(region);
            if (setDestinations.contains(region)) {
                dislodgements.add(new Dislodgement(controller, region));
            } else {
                resolveSuccessfulOrder(gameBuilder, setDestinations, new HLDOrder(controller, region), isAut);
            }
        });
        if (!isAut) {
            for (Region region : currentGame.getRegions()) {
                final Power owner = currentGame.getOwner(region.getProvince());
                if (owner != null) {
                    gameBuilder.setOwner(owner.getName(), region.getName());
                }
            }
        }
        Game ret = gameBuilder.createMyGame();
        dislodgements.forEach(dislodged -> ret.addDislodgedRegion(dislodged.getRegion(), dislodged));
        return ret;
    }

    public static List<Order> guessAllOrders(Game currentGame, List<BasicDeal> knownCommitments) {

        return Collections.synchronizedList(currentGame.getNonDeadPowers())
                .stream()
                .flatMap(power -> {
                    final Game clone = currentGame;
                    final Power clonedPower = power;
                    try {
                        List<Order> orders =
                                Utility.Plans.getAllOrders(
                                        tacticsMap.get(power.getName())
                                                .determineBestPlan(clone, clonedPower,
                                                        filterCommitmentsForPower(knownCommitments, clonedPower)
                                                ));
                        AdvancedAdjudicator.totalOrdersGuessed += 1;
                        return orders.stream();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Error resolving " + clone + "  ");
                        AdvancedAdjudicator.totalOrdersExcepted += 1;
                        return Stream.empty();
                    }
                })
                .filter(distinctByKey(Order::getLocation))
                .collect(Collectors.toList());
    }

    public static Game advanceToMovementPhase(Game currentGame) {
        switch (currentGame.getPhase()) {
            case WIN:
                return advanceToMovementPhase(determineBuilds(currentGame));
            case SPR:
            case AUT:
                return currentGame;

            case FAL:
            case SUM:
                return advanceToMovementPhase(
                        determineRetreats(currentGame)
                );
        }
        throw new IllegalArgumentException("Could not identify phase");
    }

    public static Game determineRetreats(Game currentGame) {
        List<Order> builds = tacticsMap.entrySet().stream()
                .flatMap(entry -> generateRandomRetreats(currentGame, entry.getKey()))
                .collect(Collectors.toList());
        return determineOutcome(currentGame, builds);
    }

    public static Game determineBuilds(Game currentGame) {
        List<Order> builds = tacticsMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().getWinterOrders(currentGame, currentGame.getPower(entry.getKey()), Collections.emptyList()).stream())
                .collect(Collectors.toList());
        return determineOutcome(currentGame, builds);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static List<BasicDeal> convertDealToGame(List<BasicDeal> deals, Game clone) {
        return deals.stream().map(clonedDeal -> new BasicDeal(
                clonedDeal.getOrderCommitments().stream().map(clonedOrder -> new OrderCommitment(clonedOrder.getYear(), clonedOrder.getPhase(), convertOrder(clonedOrder.getOrder(), clone))).collect(Collectors.toList()),
                clonedDeal.getDemilitarizedZones().stream().map(clonedDMZ -> new DMZ(clonedDMZ.getYear(), clonedDMZ.getPhase(), convertPowers(clonedDMZ.getPowers(), clone), convertProvinces(clonedDMZ.getProvinces(), clone))).collect(Collectors.toList())
        )).collect(Collectors.toList());
    }

    private static Order convertOrder(Order clonedOrder, Game clone) {
        Order region = null;
        if (clonedOrder instanceof HLDOrder) {
            region = new HLDOrder(convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone));
        } else if (clonedOrder instanceof MTOOrder) {
            region = new MTOOrder(convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone), convertRegion(((MTOOrder) clonedOrder).getDestination(), clone));
        } else if (clonedOrder instanceof DSBOrder) {
            region = new DSBOrder(convertRegion(clonedOrder.getLocation(), clone), convertPower(clonedOrder.getPower(), clone));
        } else if (clonedOrder instanceof RTOOrder) {
            region = new RTOOrder(convertRegion(clonedOrder.getLocation(), clone), convertPower(clonedOrder.getPower(), clone), convertRegion(((RTOOrder) clonedOrder).getDestination(), clone));
        } else if (clonedOrder instanceof BLDOrder) {
            region = new BLDOrder(convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone));
        } else if (clonedOrder instanceof REMOrder) {
            region = new REMOrder(convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone));
        } else if (clonedOrder instanceof SUPMTOOrder) {
            region = new SUPMTOOrder(
                    convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone), (MTOOrder) convertOrder(((SUPMTOOrder) clonedOrder).getSupportedOrder(), clone)
            );
        } else if (clonedOrder instanceof SUPOrder) {
            region = new SUPOrder(
                    convertPower(clonedOrder.getPower(), clone), convertRegion(clonedOrder.getLocation(), clone), convertOrder(((SUPOrder) clonedOrder).getSupportedOrder(), clone)
            );
        }
        return region;
    }

    private static Region convertRegion(Region location, Game clone) {
        return clone.getRegion(location.getName());
    }

    private static Power convertPower(Power power, Game clone) {
        return clone.getPower(power.getName());
    }

    private static List<Province> convertProvinces(List<Province> provinces, Game clone) {
        return provinces.stream().map(clonedProvince -> clone.getProvince(clonedProvince.getName())).collect(Collectors.toList());
    }

    private static List<Power> convertPowers(List<Power> powers, Game clone) {
        return powers.stream().map(clonedPower -> convertPower(clonedPower, clone)).collect(Collectors.toList());
    }

    private static void resolveFailedOrder(DiplomacyGameBuilder gameBuilder, List<Region> setDestinations, List<Dislodgement> dislodgements, Order order, boolean isAut) {
        if (order instanceof SUPOrder || order instanceof SUPMTOOrder || order instanceof MTOOrder) {
            if (setDestinations.contains(order.getLocation())) {
                dislodgements.add(new Dislodgement(order.getPower(), order.getLocation()));
            } else {
                resolveSuccessfulOrder(gameBuilder, setDestinations, new HLDOrder(order.getPower(), order.getLocation()), isAut);
            }
        } else {
            dislodgements.add(new Dislodgement(order.getPower(), order.getLocation()));
        }
    }

    private static void resolveSuccessfulOrder(DiplomacyGameBuilder gameBuilder, List<Region> destinations, Order move, boolean isAut) {
        final Region finalDestination = Utility.Plans.getFinalDestination(move);
        gameBuilder.placeUnit(move.getPower().getName(), finalDestination.toString());
        destinations.add(finalDestination);
        if (isAut) {
            gameBuilder.setOwner(move.getPower().getName(), finalDestination.getName());
        }
    }

    private static Stream<Order> generateRandomRetreats(Game game, String powerName) {
        Power me = game.getPower(powerName);
        List<Order> orders = new ArrayList<>(game.getDislodgedRegions().size());
        HashMap<Region, Dislodgement> units = game.getDislodgedRegions();
        List<Region> dislodgedUnits = game.getDislodgedRegions(me);

        for (Region region : dislodgedUnits) {
            Dislodgement dislodgement = units.get(region);
            List<Region> dest = new ArrayList<>(dislodgement.getRetreateTo());
            if (dest.isEmpty()) {
                orders.add(new DSBOrder(region, me));
            } else {
                int randomInt = AdvancedAdjudicator.random.nextInt(dest.size());
                orders.add(new RTOOrder(region, me, dest.get(randomInt)));
            }
        }
        return orders.stream();
    }

    private static List<BasicDeal> filterCommitmentsForPower(List<BasicDeal> knownCommitments, Power power) {
        return Utility.Lists.createFilteredList(knownCommitments, (Predicate<BasicDeal>) deal ->
                deal.getDemilitarizedZones().stream().anyMatch(dmz -> dmz.getPowers().contains(power)) ||
                        deal.getOrderCommitments().stream().anyMatch(orderCommitment -> orderCommitment.getOrder().getPower().equals(power)));
    }

    public static String getData() {
        return String.format("{ guessOrders: Success = %d failures= %d}", totalOrdersGuessed, totalOrdersExcepted);
    }
}
