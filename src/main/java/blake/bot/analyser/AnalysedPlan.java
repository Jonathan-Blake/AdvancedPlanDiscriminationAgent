package blake.bot.analyser;

import blake.bot.utility.JavaDumbbot;
import blake.bot.utility.Utility;
import ddejonge.bandana.dbraneTactics.Plan;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalysedPlan {

    private final Plan plan;
    private final PlanInfo info;
    private Game expectedResult;

    AnalysedPlan(Plan plan, PlanInfo data) {
        this.plan = plan;
        this.info = data;
    }

    public Integer getDBraneValue() {
        return this.plan.getValue();
    }


    protected Integer dumbBotCalculation(List<Order> myOrders) {
        Map<Region, Integer> destinationWeights = new JavaDumbbot(this.getInfo().getGame(), this.getInfo().getMe().asPower(), false).getDestinationValue();

        Integer ret = 0;
        for (Order order : myOrders) {
            final Region finalDestination;
            if (order instanceof SUPMTOOrder) {
                finalDestination = order.getLocation();
            } else if (order instanceof SUPOrder) {
                finalDestination = order.getLocation();

            } else {
                finalDestination = Utility.Plans.getFinalDestination(order);
            }
            if (finalDestination == null) {
                System.out.println("_______" + order + " has null destination");
            } else {
                ret += destinationWeights.get(finalDestination);
            }
        }
        return ret;
    }

    public Plan getPlan() {
        return plan;
    }

    public PlanInfo getInfo() {
        return info;
    }

    public int getDumbbotValue(Map<Region, Integer> destinationWeights) {
        return getDumbbotValueString(destinationWeights.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getName(),
                Map.Entry::getValue
        )));
    }

    public int getDumbbotValueString(Map<String, Integer> destinationWeights) {
        Integer ret = 0;
        for (Order order : this.getPlan().getMyOrders()) {
            final Region finalDestination = order instanceof SUPMTOOrder || order instanceof SUPOrder ?
                    order.getLocation() :
                    Utility.Plans.getFinalDestination(order);
            if (finalDestination == null) {
                System.out.println("_______" + order + " has null destination");
            } else {
                ret += destinationWeights.get(finalDestination.getName());
            }
        }
        return ret;
    }

    public int getDumbbotValue(JavaDumbbot dumbbot) {
        return getDumbbotValue(dumbbot.getDestinationValue());
    }

    public Game getExpectedResult() {
        if (expectedResult == null) {
            expectedResult = AdvancedAdjudicator.determineOutcome(
                    this.getInfo().getGame(),
                    AdvancedAdjudicator.guessAllOrders(
                            this.getInfo().getGame(),
                            Utility.Lists.append(this.getInfo().getCommitments(), this.getInfo().getDeal())
                    ));
        }
        return expectedResult;
    }
}
