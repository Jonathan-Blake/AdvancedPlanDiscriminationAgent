package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PeaceDealSupplier implements DealGenerator {
    private final boolean mutualOrCoalitionPeace;
    private final List<? extends Power> negotiators;
    private final Power me;
    private final Game game;
    private int aliveAllyIndex;
    private final Iterator<BasicDeal> iterator = new BasicDealIterator(this::generatePeaceDeal);

    public PeaceDealSupplier(boolean mutualOrCoalitionPeace, List<? extends Power> negotiators, Power me, Game game) {
        this.mutualOrCoalitionPeace = mutualOrCoalitionPeace;
        this.negotiators = negotiators;
        this.me = me;
        this.game = game;
    }

    private BasicDeal generatePeaceDeal() {

        BasicDeal ret = null;
        if (this.mutualOrCoalitionPeace) {
            while (aliveAllyIndex < negotiators.size() && ret == null) {
                Power power = this.negotiators.get(aliveAllyIndex);
                if (power != this.me) {
                    ret = proposeMutualPeace(this.me, power);
                }
                aliveAllyIndex++;
            }
        } else {
            ret = doNotInvadeCoalitionPartners();
        }
        return ret;
    }


    private BasicDeal doNotInvadeCoalitionPartners() {
        ArrayList<Power> relevantPowers;
        ArrayList<DMZ> demilitarizedZones;
        ArrayList<OrderCommitment> randomOrderCommitments;

        BasicDeal ret = null;
        while (aliveAllyIndex < negotiators.size() && ret == null) {
            relevantPowers = new ArrayList<>();
            relevantPowers.add(this.me);


            for (int i = 0; i < negotiators.size(); ++i) {
                if (i != aliveAllyIndex) {
                    relevantPowers.add(negotiators.get(i));
                }
            }

            demilitarizedZones = new ArrayList<>();
            demilitarizedZones.add(new DMZ(this.game.getYear(), this.game.getPhase(), relevantPowers, (negotiators.get(aliveAllyIndex)).getOwnedSCs()));
            randomOrderCommitments = new ArrayList<>();
            ret = new BasicDeal(randomOrderCommitments, demilitarizedZones);
            ++aliveAllyIndex;
        }
        return ret;
    }

    private BasicDeal proposeMutualPeace(Power power, Power other) {
        BasicDeal deal;
        ArrayList<OrderCommitment> randomOrderCommitments;
        ArrayList<DMZ> demilitarizedZones = new ArrayList<>();
        demilitarizedZones.add(new DMZ(this.game.getYear(), this.game.getPhase(), Collections.singletonList(other), power.getOwnedSCs()));
        demilitarizedZones.add(new DMZ(this.game.getYear(), this.game.getPhase(), Collections.singletonList(power), other.getOwnedSCs()));
        randomOrderCommitments = new ArrayList<>();
        deal = new BasicDeal(randomOrderCommitments, demilitarizedZones);
        return deal;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        //Required by inheritance however the logic doesn't change based on confirmations
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan newPlan) {
        //Required by inheritance however the logic doesn't change based on confirmations
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.iterator;
    }
}
