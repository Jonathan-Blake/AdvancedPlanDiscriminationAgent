package blake.bot.titfortat;

import blake.bot.suppliers.DealGenerator;
import blake.bot.suppliers.PlanBasedSupplier;
import blake.bot.suppliers.ProposalSupplierList;
import blake.bot.utility.DatedObject;
import blake.bot.utility.Relationship;
import blake.bot.utility.RelationshipMatrix;
import blake.bot.utility.Utility;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DiplomacyProposal;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TitForTatAgent extends AbstractNegotiationLoopNegotiator {

    public static final double DISLIKE_PROBABILITY_IF_FALSE = 0.9;
    public static final double DISLIKE_LIKELIHOOD = 0.1;
    public static final double LIKE_PROBABILITY_IF_FALSE = 0.3;
    public static final double LIKE_LIKELIHOOD = 0.7;
    private DealGenerator proposalSupplier;
    private boolean isFirstTurn = true;
    private RelationshipMatrix<Double> relationshipMatrix;
    private Plan plan;
    private List<Province> previouslyOwned = Collections.emptyList();

    TitForTatAgent(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        TitForTatAgent myPlayer = new TitForTatAgent(args);
        myPlayer.run();
    }

    @Override
    public void negotiate(long negotiationDeadline) {
        this.plan = getTacticalModule().determineBestPlan(this.getGame(), this.getMe(), this.getConfirmedDeals(), this.getAllies());
        super.negotiate(negotiationDeadline);
        this.isFirstTurn = false;
        this.proposalSupplier = null;
        this.previouslyOwned = this.getMe().getOwnedSCs();
    }

    @Override
    protected void handleRejectedMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        if (receivedMessage.getMessageId().contains(this.getMe().getName())) {
            //Rejected my proposal
            this.dislike(receivedMessage.getSender());
        }
        this.getLogger().logln("TitForTatBot.negotiate() Received rejection from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected void handleConfirmationMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        this.plan = this.getTacticalModule().determineBestPlan(this.getGame(), this.getMe(), this.getConfirmedDeals(), this.getAllies());
        this.getLogger().logln("TitForTatBot.negotiate() Received confirmation from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected void handleProposalMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        BasicDeal deal = (BasicDeal) receivedProposal.getProposedDeal();
        this.getLogger().logln("TitForTatBot.negotiate() Received proposal from " + receivedMessage.getSender() + ": " + receivedProposal);

        if (Utility.Plans.testConsistency(deal, this.getGame(), this.getConfirmedDeals())) {
            if (Utility.Plans.isPeaceDeal(deal, this.getMe())) {
                this.like(receivedMessage.getSender());
                this.getLogger().logln("Recognised Peace Deal " + new DatedObject(this.getGame()));
            }
            final Plan newPlan = this.getTacticalModule().determineBestPlan(this.getGame(), this.getMe(), Utility.Lists.append(this.getConfirmedDeals(), deal), this.relationshipMatrix.getAllies());
            final Integer comparisonValue = Utility.Plans.compare(newPlan, this.getBestPlan());
            if (comparisonValue > 0) {
                this.acceptProposal(receivedProposal.getId());
                this.like(receivedMessage.getSender());
                this.plan = newPlan;
            } else if (comparisonValue == 0 && this.getAllies().contains(this.getGame().getPower(receivedMessage.getSender()))) {
                this.acceptProposal(receivedProposal.getId());
                if (!this.plan.getMyOrders().containsAll(newPlan.getMyOrders())) {
                    // Made me change plan to something probably worse
                    this.dislike(receivedMessage.getSender());
                    this.plan = newPlan;
                } else {
                    this.like(receivedMessage.getSender());
                    this.getLogger().logln("Didn't need to change orders");
                }
            } else {
                this.rejectProposal(receivedProposal.getId());
                this.dislike(receivedMessage.getSender());
            }
        }
    }

    @Override
    protected void handleAcceptanceMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();

        this.like(receivedMessage.getSender());
        this.getLogger().logln("TitForTatBot.negotiate() Received acceptance from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected DealGenerator getProposalSupplier() {
        if (this.proposalSupplier == null) {
            List<Power> allies = this.getAllies();
            getAllies().retainAll(this.getGame().getNonDeadPowers());
            this.proposalSupplier = new ProposalSupplierList(
//                new PeaceDealSupplier(
//                        true,
//                        allies,
//                        this.getMe(),
//                        this.getGame()
//                )
//                new PeaceDealSupplier(
//                        this.isFirstTurn,
//                        allies,
//                        this.getMe(),
//                        this.getGame()
//                )
//				,
//				new MutualSupportSupplier(
//						this.getConfirmedDeals(),
//						this.getTacticalModule(),
//						this.getGame(),
//						this.getMe(),
//						this.getNegotiatingPowers(),
//						this.getLogger()
//				)
////                ,
                    new PlanBasedSupplier(
                            this.getMe(),
                            this.getBestPlan(),
                            this.getTacticalModule(),
                            this.getGame(),
                            this.getConfirmedDeals(),
                            allies
                    )
//				,
//                    new CombinedAttackSupplier(
//                            this.getMe(),
//                            allies,
//                            this.getNegotiatingPowers(),
//                            this.getGame(),
//                            this.getTacticalModule(),
//                            this.getBestPlan(),
//                            this.getConfirmedDeals()
//                    )
            );
        }
        return this.proposalSupplier;
    }

    private Plan getBestPlan() {
        return this.plan;
    }

    private List<Power> getAllies() {
        return this.getRelationshipMatrix().getAllies();
    }

    private RelationshipMatrix<Double> getRelationshipMatrix() {
        if (this.relationshipMatrix == null) {
            this.relationshipMatrix = RelationshipMatrix.getDoubleMatrix(this.getMe(), this.getNegotiatingPowers(), this.getGame(), 0.75, (value -> {
                if (value >= 0.5) {
                    return Relationship.ALLIED;
                } else {
                    return Relationship.WAR;
                }
            }));
        }
        return this.relationshipMatrix;
    }

    @Override
    public void start() {
        //Inherited but not used.
    }

    @Override
    public void receivedOrder(Order order) {
        if (order instanceof MTOOrder && order.getPower() != this.getMe()) {
            MTOOrder mtoOrder = (MTOOrder) order;
            if (this.previouslyOwned == null) {
                this.getLogger().logln("PreviouslyOwned is null?????", true);
                return;
            }
            if (mtoOrder.getDestination() == null) {
                this.getLogger().logln("__________________________________________Somehow MTO has no destination?????", true);
                return;
            }
            if (this.previouslyOwned.contains(mtoOrder.getDestination().getProvince())) {
                if (this.getAllies().contains(mtoOrder.getPower())) {
                    //Dislike or break alliance, whichever is smaller.
                    Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(mtoOrder.getPower());
                    currentRelationship.ifPresent(curr -> this.getRelationshipMatrix().setRelationship(mtoOrder.getPower(),
                            Math.min(
                                    0.25d,
                                    Utility.Probability.bayes(
                                            curr,
                                            DISLIKE_PROBABILITY_IF_FALSE,
                                            DISLIKE_LIKELIHOOD
                                    ))));
                } else {
                    this.dislike(mtoOrder.getPower());
                }
            }
        }
    }

    private void dislike(String power) {
        dislike(this.getGame().getPower(power));
    }

    private void dislike(Power power) {
        Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(power);
        currentRelationship.ifPresent(curr -> {
            this.getRelationshipMatrix().setRelationship(power,
                    Math.max(
                            0.000001d,
                            Utility.Probability.bayes(
                                    curr,
                                    DISLIKE_PROBABILITY_IF_FALSE,
                                    DISLIKE_LIKELIHOOD
                            )));
            this.getLogger().logln(String.format("Tit for Tat Agent dislikes %s : %s -> %s", power.getName(), currentRelationship.get(), this.getRelationshipMatrix().getRelationship(power)));
        });
    }

    private void like(String power) {
        like(this.getGame().getPower(power));
    }

    private void like(Power power) {
        Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(power);
        currentRelationship.ifPresent(curr -> {
            this.getRelationshipMatrix().setRelationship(power,
                    Math.min(
                            0.999999d,
                            Utility.Probability.bayes(
                                    curr,
                                    LIKE_PROBABILITY_IF_FALSE,
                                    LIKE_LIKELIHOOD
                            )));
            this.getLogger().logln(String.format("Tit for Tat Agent likes %s : %s -> %s", power.getName(), currentRelationship.get(), this.getRelationshipMatrix().getRelationship(power)));
        });

    }
}
