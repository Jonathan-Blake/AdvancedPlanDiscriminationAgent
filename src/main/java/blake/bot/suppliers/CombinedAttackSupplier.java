package blake.bot.suppliers;

import blake.bot.utility.DatedObject;
import blake.bot.utility.Utility;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CombinedAttackSupplier implements DealGenerator {
    private final DBraneTactics dBraneTactics;
    private final Power me;
    private final List<BasicDeal> commitments;
    private final List<Power> negotiators;
    List<Power> allies;
    List<Power> enemies;
    private Plan plan;
    private Game game;
    private Iterator<Region> myUnitIterator;
    private Region unit;
    private Iterator<Province> adjacentProvinceIterator;
    private Power targetOwner;

    private final Iterator<BasicDeal> myIterator = new BasicDealIterator(this::generateCombinedAttack);

    public CombinedAttackSupplier(Power me, List<Power> allies, List<Power> negotiators, Game game, DBraneTactics dBraneTactics, Plan currentPlan, List<BasicDeal> commitments) {
        this.me = me;
        this.allies = allies;
        this.negotiators = negotiators;
        this.enemies = Utility.Lists.createFilteredList(game.getNonDeadPowers(), allies);
        this.game = game;
        this.dBraneTactics = dBraneTactics;
        this.plan = currentPlan;
        this.commitments = commitments;
        allies.remove(this.me);

        reset(this.commitments);
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.myIterator;
    }


    BasicDeal generateCombinedAttack() {
        ArrayList<OrderCommitment> orderCommitments = new ArrayList<>();

        while (!allUnitsProcessed()) {
            unitLoop:
            {
                Province target;
                do {
                    do {
                        do {
                            if (!adjacentProvinceIterator.hasNext()) {
                                break unitLoop;
                            }
                            target = adjacentProvinceIterator.next();
                            orderCommitments.clear();
                            targetOwner = this.game.getOwner(target);
                        } while (targetOwner == null);
                    } while (!enemies.contains(targetOwner));

                    MTOOrder mainAttack = new MTOOrder(this.me, unit, this.game.getAdjacentRegionIn(target, unit));
                    orderCommitments.add(new OrderCommitment(this.game.getYear(), this.game.getPhase(), mainAttack));
                    List<Region> adjacentUnits = this.game.getAdjacentUnits(target);

                    Province finalTarget = target;
                    adjacentUnits.forEach(adjacentUnit -> createOrderCommitment(orderCommitments, mainAttack, finalTarget, adjacentUnit));
                } while (orderCommitments.size() < Utility.Plans.maxDefensiveValue(target, game, this.enemies));
                unit = null;
                BasicDeal newDeal = testPlan(orderCommitments, target);
                if (newDeal != null) return newDeal;
            }
            unit = null;
        }
        return null;
    }

    private BasicDeal testPlan(ArrayList<OrderCommitment> orderCommitments, Province target) {
        final BasicDeal newDeal = new BasicDeal(orderCommitments, createDMZ(orderCommitments, target));
        Plan newPlan = dBraneTactics.determineBestPlan(game, this.me, Utility.Lists.append(this.commitments, newDeal), this.allies);
        if (newPlan != null && Utility.Plans.compare(newPlan, this.plan) >= 1) {
            return newDeal;
        }
        return null;
    }

    private List<DMZ> createDMZ(ArrayList<OrderCommitment> orderCommitments, Province target) {
        DatedObject nextDate = Utility.Dates.getNextMovementDate(new DatedObject(game));
        List<Power> participants = orderCommitments.stream()
                .map(orderCommitment -> orderCommitment.getOrder().getPower())
                .filter(power -> power != this.me)
                .collect(Collectors.toList());
        return Collections.singletonList(new DMZ(nextDate.getYear(), nextDate.getPhase(), participants, Collections.singletonList(target)));
    }

    private void createOrderCommitment(ArrayList<OrderCommitment> orderCommitments, MTOOrder mainAttack, Province finalTarget, Region adjacentUnit) {
        final Power adjacentController = this.game.getController(adjacentUnit);
        if (adjacentController != targetOwner && negotiators.contains(adjacentController) && adjacentUnit.getAdjacentRegions().retainAll(finalTarget.getRegions())) {
            final Order supportingAttack = new SUPMTOOrder(adjacentController, adjacentUnit, mainAttack);
            orderCommitments.add(new OrderCommitment(this.game.getYear(), this.game.getPhase(), supportingAttack));
        }
    }

    private boolean allUnitsProcessed() {
        //Currently, processing a unit or still has more to process
        if (unit != null) {
            return false;
        } else if (myUnitIterator.hasNext()) {
            unit = myUnitIterator.next();
            List<Province> adjacentProvinces = unit.getAdjacentRegions().stream()
                    .filter(region -> this.game.getController(region) != this.me)
                    .filter(region -> !this.allies.contains(this.game.getController(region)))
                    .map(Region::getProvince)
                    .collect(Collectors.toList());
            this.adjacentProvinceIterator = adjacentProvinces.iterator();
            return false;
        }
        return true;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan plan) {
        this.myUnitIterator = this.me.getControlledRegions().iterator();
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        this.reset(confirmedDeals, null);
    }
}
