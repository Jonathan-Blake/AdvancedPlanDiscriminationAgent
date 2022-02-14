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
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PlanBasedSupplier implements DealGenerator {
    private final DBraneTactics dBraneTactics;
    private final Power me;
    private final Game game;
    private Plan plan;
    private List<Order> orders;
    private List<BasicDeal> commitments;
    private List<Power> allies;
    private boolean submitted;
    private final Iterator<BasicDeal> iterator = new BasicDealIterator(this::getDeal);

    public PlanBasedSupplier(Power me, Plan plan, DBraneTactics dBraneTactics, Game game, List<BasicDeal> commitments, List<Power> allies) {
        this.me = me;
        this.plan = plan;
        this.dBraneTactics = dBraneTactics;
        this.game = game;
        this.allies = allies;
        this.orders = Utility.Plans.getAllOrders(this.plan);
        reset(commitments, plan);
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        this.reset(confirmedDeals, dBraneTactics.determineBestPlan(game, this.me, confirmedDeals, this.allies));
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan newPlan) {
        this.plan = newPlan;
        this.commitments = confirmedDeals;
        submitted = false;
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.iterator;
    }

    BasicDeal getDeal() {
        if (submitted) {
            return null;
        } else {
            submitted = true;
        }
        if (allies.isEmpty()) {
            return null;
        }
        List<OrderCommitment> committed = this.plan.getMyOrders().stream()
                .filter(this::orderFilter)
                .map(order -> new OrderCommitment(this.game.getYear(), this.game.getPhase(), order))
                .collect(Collectors.toList());
        if (committed.isEmpty()) {
            return null;
        }
        List<DMZ> dmzs = Collections.singletonList(new DMZ(this.game.getYear(), this.game.getPhase(), this.allies, Collections.singletonList(this.me.getOwnedSCs().get(0))));
        return new BasicDeal(committed, dmzs);
    }

    private boolean orderFilter(Order order) {
        return (order instanceof MTOOrder || order instanceof SUPMTOOrder) && notInCommitments(order);
    }

    private boolean notInCommitments(Order order) {
        return this.commitments.stream().noneMatch(
                deal -> deal.getOrderCommitments().stream().anyMatch(
                        orderCommitment -> new DatedObject(orderCommitment).equals(new DatedObject(game)) && orderCommitment.getOrder().equals(order)
                )
        );
    }
}
