package blake.bot.utility;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.tools.Utilities;
import es.csic.iiia.fabregues.dip.board.*;
import es.csic.iiia.fabregues.dip.orders.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utility {

    private Utility() {
    }

    public static class Hashing {
        private Hashing() {
        }

        public static List<String> powerToString(List<Power> powers) {
            return powers.stream().map(Power::getName).collect(Collectors.toList());
        }

        public static boolean powersAreEqual(Power power, Power other) {
            if (power == null || other == null) throw new AssertionError();
            return power.getName().equals(other.getName());
        }
    }

    public static class Dates {
        private Dates() {
        }

        public static DatedObject getNextDate(DatedObject date) {
            Phase nextPhase = getNextPhase(date.getPhase());
            if (nextPhase == Phase.SPR) {
                return new DatedObject(date.getYear() + 1, nextPhase);
            } else {
                return new DatedObject(date.getYear(), nextPhase);
            }
        }

        public static DatedObject getNextMovementDate(DatedObject date) {
            Phase nextPhase = getNextMovementPhase(date.getPhase());
            if (nextPhase == Phase.SPR) {
                return new DatedObject(date.getYear() + 1, nextPhase);
            } else {
                return new DatedObject(date.getYear(), nextPhase);
            }
        }

        static Phase getNextMovementPhase(Phase phase) {
            switch (phase) {
                case SPR:
                case SUM:
                    return Phase.FAL;
                case FAL:
                case AUT:
                case WIN:
                    return Phase.SPR;
                default:
                    throw new IllegalArgumentException("Unknown Phase " + phase);
            }
        }

        static Phase getNextPhase(Phase phase) {
            switch (phase) {
                case SPR:
                    return Phase.SUM;
                case SUM:
                    return Phase.FAL;
                case FAL:
                    return Phase.AUT;
                case AUT:
                    return Phase.WIN;
                case WIN:
                    return Phase.SPR;
                default:
                    throw new IllegalArgumentException("Unknown Phase " + phase);
            }
        }

        public static boolean isHistory(Phase phase, int year, DatedObject currentDate) {
            if (year == currentDate.getYear()) {
                return getPhaseValue(phase) < getPhaseValue(currentDate.getPhase());
            } else {
                return year < currentDate.getYear();
            }
        }

        public static boolean isHistory(DatedObject datedObject, DatedObject currentDate) {
            return isHistory(datedObject.getPhase(), datedObject.getYear(), currentDate);
        }

        static int getPhaseValue(Phase phase) {
            switch (phase) {
                case SPR:
                    return 0;
                case SUM:
                    return 1;
                case FAL:
                    return 2;
                case AUT:
                    return 3;
                case WIN:
                    return 4;
                default:
                    return -1;
            }
        }
    }

    public static class Plans {

        private Plans() {
        }

        @SuppressWarnings("unchecked")
        public static List<Order> getAllOrders(Plan plan) {
            List<Order> orders;
            try {
                Field ordersField = plan.getClass().getDeclaredField("orders");
                ordersField.setAccessible(true);
                orders = (List<Order>) ordersField.get(plan);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                orders = plan.getMyOrders();
            }
            return orders;
        }

        public static boolean areIdentical(Plan oldPlan, Plan newPlan) {
            if (Objects.equals(oldPlan, newPlan)) {
                return true;
            } else {
                List<Order> oldOrders = getAllOrders(oldPlan);
                List<Order> newOrders = getAllOrders(newPlan);
                return oldOrders.size() == newOrders.size() && oldOrders.containsAll(newOrders);
            }
        }

        public static Integer compare(Plan plan, Plan otherPlan) {
            if (plan == null ^ otherPlan == null) {
                return plan == null ? -1 : 1;
            } else if (areIdentical(plan, otherPlan)) {
                return 0;
            }
            return (plan != null ? plan.getValue() : -1000) - (otherPlan != null ? otherPlan.getValue() : -1000);
        }

        public static boolean isPeaceDeal(BasicDeal deal, Power power) {
            //Powers Provinces are demilitarised for other powers
            List<Province> protectedRegions = power.getOwnedSCs();
            return deal.getDemilitarizedZones().stream().anyMatch(
                    dmz -> dmz.getProvinces().containsAll(protectedRegions) && !dmz.getPowers().contains(power)
            );
        }

        public static long maxDefensiveValue(Province target, Game game, List<Power> enemies) {
            Power owner = game.getController(target);
            if (owner != null && !enemies.contains(owner)) {
                enemies = Lists.append(enemies, owner);
            }
            final List<Power> finalEnemies = enemies;
            return game.getAdjacentUnits(target).stream().filter(adjacent -> finalEnemies.contains(game.getController(adjacent))).count() +
                    (game.getController(target) != null ? 1 : 0);
        }

        public static boolean testConsistency(BasicDeal deal, Game game, List<BasicDeal> commitments) {
            boolean outDated = false;

            for (DMZ dmz : deal.getDemilitarizedZones()) {
                if (Dates.isHistory(dmz.getPhase(), dmz.getYear(), new DatedObject(game))) {
                    outDated = true;
                    break;
                }
            }

            String consistencyReport = null;
            if (!outDated) {
                commitments.add(deal);
                consistencyReport = Utilities.testConsistency(game, commitments);
            }
            return !outDated && consistencyReport == null;
        }

        public static Region getFinalDestination(Order order) {
            Region region = null;
            if (order instanceof HLDOrder) {
                region = order.getLocation();
            } else if (order instanceof MTOOrder) {
                region = ((MTOOrder) order).getDestination();
            } else if (order instanceof DSBOrder) {
                region = order.getLocation();
            } else if (order instanceof RTOOrder) {
                region = ((RTOOrder) order).getDestination();
            } else if (order instanceof BLDOrder) {
                region = ((BLDOrder) order).getDestination();
            } else if (order instanceof REMOrder) {
                region = order.getLocation();
            } else if (order instanceof SUPMTOOrder) {
                region = order.getLocation();
            } else if (order instanceof SUPOrder) {
                region = order.getLocation();
            }
            return region;
        }
    }

    public static class Lists {
        private Lists() {
        }

        public static <T> List<T> append(List<T> list, T addition) {
            List<T> ret = new LinkedList<>(list);
            ret.add(addition);
            return ret;

        }

        public static <T> List<T> createFilteredList(Collection<T> fullList, Collection<T> itemsToRemove) {
            List<T> ret = new LinkedList<>(fullList);
            ret.removeAll(itemsToRemove);
            return ret;
        }

        public static <T> List<T> createFilteredList(Collection<T> fullList, T itemToRemove) {
            List<T> ret = new LinkedList<>(fullList);
            ret.remove(itemToRemove);
            return ret;
        }

        public static <T> List<T> createFilteredList(Collection<T> fullList, Predicate<T> filter) {
            List<T> ret = new LinkedList<>(fullList);
            ret.removeIf(filter);
            return ret;
        }
    }

    public static class Probability {
        private Probability() {
        }

        public static Double bayes(final Double prior, final double probabilityIfFalse, final double likelihood) {
            final double v = prior * likelihood;
            return v / (v + (1 - prior) * probabilityIfFalse);
        }
    }
}
