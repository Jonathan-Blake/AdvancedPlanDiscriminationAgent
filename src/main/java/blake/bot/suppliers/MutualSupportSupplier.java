package blake.bot.suppliers;

import blake.bot.eager.Utility;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Logger;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;

import java.util.*;
import java.util.stream.Collectors;

public class MutualSupportSupplier implements DealGenerator {
    private final DBraneTactics dBraneTactics;
    private final Game game;
    private final Power me;
    private final Logger logger;
    private final List<Power> allies;
    private Plan basePlan;
    private List<BasicDeal> commitments;
    private Iterator<Region> myUnitIterator;
    private Iterator<Region> adjacentUnitIterator;
    private Region unit;
    private Region adjacentUnit;
    private final Iterator<BasicDeal> myIterator = new BasicDealIterator(this::generateMutualSupport);

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.myIterator;
    }

    public MutualSupportSupplier(Collection<? extends BasicDeal> commitments, DBraneTactics dBraneTactics, Game game, Power me, List<Power> allies, Logger logger) {
        this.dBraneTactics = dBraneTactics;
        this.game = game;
        this.me = me;
        this.allies = allies;
        this.logger = logger;

        this.reset(new ArrayList<>(commitments));
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        this.reset(confirmedDeals, dBraneTactics.determineBestPlan(game, this.me, confirmedDeals, this.allies));
    }

    @Override
    public void reset(List<BasicDeal> commitments, Plan plan) {
        this.commitments = commitments;
        this.basePlan = plan;
        List<Region> myUnits = this.basePlan.getMyOrders().stream().filter(HLDOrder.class::isInstance).map(Order::getLocation).collect(Collectors.toList());
        myUnitIterator = myUnits.iterator();
        unit = null;
        adjacentUnit = null;
    }

    BasicDeal generateMutualSupport() {
        if (basePlan == null || this.allies.isEmpty()) {
            return null;
        }
        while (!allUnitsProcessed()) {
            while (!allAdjacentUnitsProcessed()) {
                BasicDeal deal = findDealForAdjacentUnit();
                if (deal != null) return deal;
            }
            unit = null;
        }
        return null;
    }

    private BasicDeal findDealForAdjacentUnit() {
        BasicDeal ret = null;
        if (adjacentUnit.getAdjacentRegions().contains(unit)) { //I don't know what this does so am leaving it in incase I need to revert
            Power adjacentController = this.game.getController(adjacentUnit);
            if (controllerIsAllied(adjacentController)) {
                List<OrderCommitment> orderCommitments = new ArrayList<>(2);
                orderCommitments.add(new OrderCommitment(
                        this.game.getYear(),
                        this.game.getPhase(),
                        generateSupport(adjacentController, adjacentUnit, this.me, unit)));
                orderCommitments.add(new OrderCommitment(
                        this.game.getYear(),
                        this.game.getPhase(),
                        generateSupport(this.me, unit, adjacentController, adjacentUnit)));
                BasicDeal deal = new BasicDeal(orderCommitments, Collections.emptyList());
                Plan newPlan = this.dBraneTactics.determineBestPlan(this.game, this.me, Utility.Lists.append(commitments, deal));
                if (newPlanIsEqualOrBetter(newPlan)) {
                    this.logger.logln("Found mutual support : " + deal.getOrderCommitments(), true);
                    ret = deal;
                } else {

                    this.logger.logln("Found mutual support was unhelpful: " + deal.getOrderCommitments(), true);
                }
            }
        }
        adjacentUnit = null;
        return ret;
    }

    /*
        Loop Processing
     */

    private boolean allUnitsProcessed() {
        //Currently, processing a unit or still has more to process
        if (unit != null) {
            return false;
        } else if (myUnitIterator.hasNext()) {
            unit = myUnitIterator.next();
            List<Region> adjacentUnits = this.game.getAdjacentUnits(unit.getProvince());
            adjacentUnitIterator = adjacentUnits.iterator();
            return false;
        }
        return true;
    }

    private boolean allAdjacentUnitsProcessed() {
        //Currently, processing a unit or still has more to process
        if (adjacentUnit != null) {
            return false;
        } else if (adjacentUnitIterator.hasNext()) {
            adjacentUnit = adjacentUnitIterator.next();
            return false;
        }
        return true;
    }

    private Order generateSupport(Power holdPower, Region holdUnit, Power supportPower, Region supportUnit) {
        Order hold = new HLDOrder(holdPower, holdUnit);
        return new SUPOrder(supportPower, supportUnit, hold);
    }
    /*
        Complex bool statements
     */

    private boolean controllerIsAllied(Power adjacentController) {
        return adjacentController != this.me && allies.contains(adjacentController);
    }

    private boolean newPlanIsEqualOrBetter(Plan newPlan) {
        return newPlan != null && Utility.Plans.compare(newPlan, basePlan) > 0;
    }
}
